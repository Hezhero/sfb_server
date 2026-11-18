package com.junchen.quartz;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 定时任务与种子 SQL 的对账测试。
 *
 * <p><b>为什么需要这条测试</b>：本项目没有任何 {@code @Scheduled}，任务能否执行完全取决于
 * {@code system_quartz_job} 表里有没有对应记录（{@code bean_name}）。而「新写一个任务类」
 * 和「往种子 SQL 里加一条」是两件独立的事——漏了后者，任务会静默不执行，线上表现为
 * 「新平台订单一条都不回流」，且没有任何报错。</p>
 *
 * <p>2026-11-17 就发生过一次：工单 04/05/06/07/08 新增的 8 个采集任务全部漏登记。
 * 这条测试把「接线」从人肉检查变成可验证的断言。</p>
 */
public class QuartzSeedCoverageTest {

    /** 不能注册为任务的类：基类没有 run 方法 */
    private static final List<String> NON_TASK_CLASSES = List.of("OrderTask");

    /** 类名 → beanName（首字母小写），与 Spring 默认命名一致 */
    private static String toBeanName(String className) {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static Path projectRoot() {
        // 测试运行时的工作目录是模块目录（mshop-admin），种子 SQL 在仓库根的 docs/sql 下
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("docs/sql/system_quartz_job.sql"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到仓库根（docs/sql/system_quartz_job.sql）");
    }

    private static List<String> taskClassNames(Path root) throws IOException {
        Path taskDir = root.resolve("mshop-admin/src/main/java/com/junchen/modules/quartz/task");
        try (Stream<Path> files = Files.list(taskDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> p.getFileName().toString().replace(".java", ""))
                    .filter(n -> !NON_TASK_CLASSES.contains(n))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String seedSql(Path root) throws IOException {
        return Files.readString(root.resolve("docs/sql/system_quartz_job.sql"), StandardCharsets.UTF_8);
    }

    @Test
    public void everyTaskClassHasSeedRecord() throws IOException {
        Path root = projectRoot();
        String sql = seedSql(root);
        List<String> missing = new ArrayList<>();
        for (String className : taskClassNames(root)) {
            String bean = toBeanName(className);
            if (!sql.contains("'" + bean + "'")) {
                missing.add(bean + "（" + className + ".java）");
            }
        }
        if (!missing.isEmpty()) {
            fail("以下任务类在 docs/sql/system_quartz_job.sql 中没有种子记录，"
                    + "部署后不会执行且不报错。请补录种子：\n  - " + String.join("\n  - ", missing));
        }
    }

    @Test
    public void everyTaskClassDeclaresRunEntryPoint() throws IOException {
        Path root = projectRoot();
        Path taskDir = root.resolve("mshop-admin/src/main/java/com/junchen/modules/quartz/task");
        List<String> broken = new ArrayList<>();
        // QuartzRunnable 用 getDeclaredMethod + ReflectionUtils.makeAccessible，
        // 因此 protected/public 都可以，但方法必须**声明在本类**（不能只继承）。
        // 两种合法签名：run(String params) 与无参 run()。
        Pattern withParam = Pattern.compile("(public|protected)\\s+void\\s+run\\s*\\(\\s*(String|final String)\\s+\\w+\\s*\\)");
        Pattern noParam = Pattern.compile("(public|protected)\\s+void\\s+run\\s*\\(\\s*\\)");
        for (String className : taskClassNames(root)) {
            String body = Files.readString(taskDir.resolve(className + ".java"), StandardCharsets.UTF_8);
            if (!withParam.matcher(body).find() && !noParam.matcher(body).find()) {
                broken.add(className);
            }
        }
        if (!broken.isEmpty()) {
            fail("以下任务类缺少 run(String) 或无参 run() 入口（Quartz 用 getDeclaredMethod 反射调用，"
                    + "方法必须声明在本类，不能只在父类）：\n  - " + String.join("\n  - ", broken));
        }
    }

    @Test
    public void seedSqlUsesFixedIdsAndDeleteListMatchesInsertList() throws IOException {
        Path root = projectRoot();
        String sql = seedSql(root);

        // DELETE 清单里的 bean 与 INSERT 的 bean 必须完全一致，否则重复执行会留孤儿或重复
        List<String> deleted = matchAll(sql, "DELETE FROM `system_quartz_job`[\\s\\S]*?WHERE `bean_name` IN \\(([\\s\\S]*?)\\);");
        List<String> inserted = matchAll(sql, "INSERT INTO `system_quartz_job`[\\s\\S]*?VALUES([\\s\\S]*?);");

        List<String> deleteBeans = matchAll(deleted.isEmpty() ? "" : deleted.get(0), "'([a-zA-Z0-9]+)'");
        List<String> insertBeans = matchAll(inserted.isEmpty() ? "" : inserted.get(0), "\\d{4},\\s*'([a-zA-Z0-9]+)'");

        assertTrue("DELETE 清单为空，解析失败", deleteBeans.size() > 0);
        assertTrue("INSERT 清单为空，解析失败", insertBeans.size() > 0);

        List<String> inInsertNotDelete = insertBeans.stream()
                .filter(b -> !deleteBeans.contains(b)).distinct().sorted().collect(Collectors.toList());
        List<String> inDeleteNotInsert = deleteBeans.stream()
                .filter(b -> !insertBeans.contains(b)).distinct().sorted().collect(Collectors.toList());

        if (!inInsertNotDelete.isEmpty() || !inDeleteNotInsert.isEmpty()) {
            fail("DELETE 与 INSERT 的 bean 清单不一致：\n"
                    + "  只在 INSERT：" + inInsertNotDelete + "\n"
                    + "  只在 DELETE：" + inDeleteNotInsert);
        }
    }

    private static List<String> matchAll(String text, String regex) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }
}
