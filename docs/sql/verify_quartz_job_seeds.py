#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
system_quartz_job 种子 SQL 的离线一致性校验器（不连数据库、不启应用）。

为什么需要它
------------
Quartz 任务注册链是「按 Bean 名反射调用」的：
    QuartzRunnable: SpringContextHolder.getBean(beanName)
                    target.getClass().getDeclaredMethod(methodName[, String])
                    method.invoke(target[, params])
    —— getDeclaredMethod 不查父类，且「params 非空就找 run(String)，否则找 run()」。
所以种子 SQL 里任何一个字段写错，都不会在编译期报错，只会在运行期抛
NoSuchMethodException / NoSuchBeanDefinitionException，而且只有到 cron 触发那一刻
才在日志里暴露。部署前必须用本脚本做静态校验。

校验项
------
  1. 清单齐全：SQL 覆盖的 bean_name 集合 == 任务包中「含 run 方法的类」集合
  2. 分类数量：实时采集 9 / 历史回溯 8 / 计费 6 / 订单后处理 4 / 用户运营 3 = 30
  3. bean_name == 类名首字母小写（Spring 默认 Bean 名）
  4. method_name 必须在目标类自身声明（父类 OrderTask 上没有 run，不能靠继承）
  5. params 空/非空必须与 run()/run(String) 重载匹配，否则反射必然失败
  6. cron 为合法 Quartz 6/7 段写法（秒在前），且类别语义正确：
     - 实时采集：每 10 分钟触发，params.minutes 恰为 10（窗口与周期对齐，不重不漏）
     - 历史回溯：每天一次，params 必须带 day 与 minutes
  7. is_pause 全部为 0
  8. 幂等形态：先按 bean_name 定向 DELETE 再 INSERT，且显式 id 唯一
  9. 任务类清单里不含基类 OrderTask（它没有 run 方法，不是任务）

用法
----
    python docs/sql/verify_quartz_job_seeds.py [种子SQL路径]
退出码 0 = 全部通过；1 = 有失败断言。
"""
import os
import re
import sys

# Windows 控制台默认 GBK，直接 print 中文/emoji 会 UnicodeEncodeError。
# 统一改写成 UTF-8（不可编码字符替换掉，不要让校验本身崩在输出上）。
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
TASK_DIR = os.path.join(
    REPO_ROOT, "mshop-admin", "src", "main", "java", "com", "mailvor", "modules", "quartz", "task"
)
DEFAULT_SQL = os.path.join(REPO_ROOT, "docs", "sql", "system_quartz_job.sql")

# 期望分类（独立于 SQL 的规格真值，来自工单与代码上下文笔记第 5 章）
EXPECTED = {
    "realtime": [
        "tbOrderCollectionTask", "jdOrderCollectionTask", "pddOrderCollectionTask",
        "vipOrderCollectionTask", "dyOrderCollectionTask", "dyKuOrderCollectionTask",
        "eleKuOrderCollectionTask", "orderMtCPSCollectionTask", "csjEcomOrderCollectionTask",
    ],
    "history": [
        "tbOrderHistoryCollectionTask", "jdOrderHistoryCollectionTask", "pddOrderHistoryCollectionTask",
        "vipOrderHistoryCollectionTask", "dyOrderHistoryCollectionTask", "dyKuOrderHistoryCollectionTask",
        "eleKuOrderHistoryCollectionTask", "orderMtCPSHistoryCollectionTask",
    ],
    "fee": [
        "fee7DayTask", "feeMonthTask", "feeProMonthTask",
        "feeTodayTask", "feeTotalTask", "feeTriggerTask",
    ],
    "post": ["orderRefundTask", "orderCheckTask", "selfOrderHbTask", "billUnlockTask"],
    "ops": ["memberExpiredTask", "payResetTask", "visitsTask"],
}

passed = 0
failed = 0


def ok():
    global passed
    passed += 1


def bad(msg):
    global failed
    failed += 1
    print("  [FAIL] " + msg)


def load_java_tasks():
    """扫描任务包，返回 {beanName: (has_noarg_run, has_string_run, path)}"""
    result = {}
    for name in sorted(os.listdir(TASK_DIR)):
        if not name.endswith(".java"):
            continue
        cls = name[:-5]
        path = os.path.join(TASK_DIR, name)
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            src = fh.read()
        if cls == "OrderTask":
            # 基类：只有 saveXxx 落库方法，没有 run()，不是可执行任务
            if re.search(r"\bvoid\s+run\s*\(", src):
                bad("基类 OrderTask 竟然声明了 run()，请复核任务清单")
            continue
        noarg = re.search(r"\bvoid\s+run\s*\(\s*\)", src) is not None
        witharg = re.search(r"\bvoid\s+run\s*\(\s*String\b", src) is not None
        if not (noarg or witharg):
            continue  # 不含 run 的辅助类不计入
        bean = cls[0].lower() + cls[1:]
        result[bean] = (noarg, witharg, path)
    return result


def parse_rows(sql):
    """解析每条 VALUES 元组：按列显式顺序取值。"""
    col_re = re.compile(
        r"\(\s*(\d+)\s*,\s*"                 # id
        r"'([^']*)'\s*,\s*"                  # bean_name
        r"'([^']*)'\s*,\s*"                  # method_name
        r"(NULL|'(?:[^'\\]|\\.)*')\s*,\s*"   # params
        r"'([^']*)'\s*,\s*"                  # cron_expression
        r"([01])\s*,\s*"                     # is_pause
        r"'([^']*)'\s*,\s*"                  # job_name
        r"'([^']*)'"                         # remark
    )
    rows = []
    for m in col_re.finditer(sql):
        rid, bean, method, params, cron, pause, job_name, remark = m.groups()
        rows.append({
            "id": int(rid), "bean": bean, "method": method,
            "params": None if params.upper() == "NULL" else params.strip("'"),
            "cron": " ".join(cron.split()),
            "pause": int(pause), "job_name": job_name, "remark": remark,
        })
    return rows


def cron_fields(cron):
    return [f for f in re.split(r"\s+", cron) if f]


def main():
    sql_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SQL
    print("== 种子 SQL: %s" % sql_path)
    if not os.path.isfile(sql_path):
        print("  [FAIL] 种子文件不存在")
        print("\n结果: ❌ FAILED (1 项失败)")
        return 1
    with open(sql_path, "r", encoding="utf-8") as fh:
        sql = fh.read()

    java = load_java_tasks()
    rows = parse_rows(sql)
    by_bean = {r["bean"]: r for r in rows}

    print("== Java 中可执行任务: %d 个；SQL 中记录: %d 条" % (len(java), len(rows)))

    # 1. 清单齐全（双向）
    print("-- 1. bean_name 清单与 Java 源码双向一致")
    for bean in sorted(java):
        if bean not in by_bean:
            bad("任务类未写入种子: %s" % bean)
        else:
            ok()
    for bean in sorted(by_bean):
        if bean not in java:
            bad("种子里存在任务包中不存在的 bean: %s" % bean)
        else:
            ok()

    # 2. 分类数量
    print("-- 2. 五类任务数量与工单一致（合计 30）")
    total_expected = 0
    for cat, names in EXPECTED.items():
        total_expected += len(names)
        for n in names:
            if n not in java:
                bad("规格里的任务在 Java 中找不到: %s" % n)
            else:
                ok()
    if total_expected != 30:
        bad("规格分类合计应为 30，实际 %d" % total_expected)
    else:
        ok()
    if len(rows) != 30:
        bad("种子记录数应为 30（OrderTask 是基类不计），实际 %d" % len(rows))
    else:
        ok()

    # 3. bean_name 命名规则 + 4. method 在类内声明 + 5. 参数重载匹配
    print("-- 3/4/5. bean 命名、method 声明位置、params 与重载匹配")
    for bean, row in sorted(by_bean.items()):
        if bean not in java:
            continue
        noarg, witharg, path = java[bean]
        cls = bean[0].upper() + bean[1:]
        if not os.path.basename(path).startswith(cls):
            bad("%s 的 bean 名不符合「类名首字母小写」: 期望 %s" % (bean, path))
        else:
            ok()

        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            src = fh.read()
        mod = r"(?:public|protected|private)"
        if not re.search(mod + r"\s+void\s+" + re.escape(row["method"]) + r"\s*\(", src):
            bad("%s 未在自身类中声明方法 %s（getDeclaredMethod 不查父类）" % (cls, row["method"]))
        else:
            ok()

        is_blank = row["params"] is None or row["params"].strip() == ""
        if noarg and not witharg:
            if not is_blank:
                bad("%s 只有无参 run()，params 必须为 NULL，实际: %r" % (bean, row["params"]))
            else:
                ok()
        elif witharg and not noarg:
            if is_blank:
                bad("%s 只有 run(String)，params 不能为空，否则反射去找 run() 会失败" % bean)
            else:
                ok()
        else:
            bad("%s 同时存在 run() 与 run(String)，种子无法确定语义，需人工确认" % bean)

    # 6. cron 合法性 + 类别语义
    print("-- 6. cron 合法性与类别语义")
    realtime = set(EXPECTED["realtime"])
    history = set(EXPECTED["history"])
    for bean, row in sorted(by_bean.items()):
        fields = cron_fields(row["cron"])
        if len(fields) not in (6, 7):
            bad("%s 的 cron 段数应为 6 或 7，实际 %d: %s" % (bean, len(fields), row["cron"]))
            continue
        if not re.match(r"^[0-9*?/,A-Za-z\- ]+$", row["cron"]):
            bad("%s 的 cron 含非法字符: %s" % (bean, row["cron"]))
            continue
        ok()

        if bean in realtime:
            # 每 10 分钟一次：秒=0，分=X/10
            if not re.match(r"^\d+\s+\d+/\d+\s+\*\s+\*\s+\*\s+\?$", row["cron"]):
                bad("%s 是实时采集任务，cron 应为「每 N 分钟」形态: %s" % (bean, row["cron"]))
            else:
                step = int(re.match(r"^\d+\s+\d+/(\d+)", row["cron"]).group(1))
                if step != 10:
                    bad("%s 采集周期应为 10 分钟，实际 %d" % (bean, step))
                else:
                    ok()
            if not row["params"] or '"minutes"' not in row["params"]:
                bad("%s 实时采集 params 应含 minutes: %r" % (bean, row["params"]))
            else:
                m = re.search(r'"minutes"\s*:\s*(\d+)', row["params"])
                if not m or int(m.group(1)) != 10:
                    bad("%s 采集窗口 minutes 必须与 cron 周期一致为 10，实际: %s" % (bean, row["params"]))
                else:
                    ok()

        if bean in history:
            # 每天一次：秒 分 时 * * ?
            if not re.match(r"^\d+\s+\d+\s+\d+\s+\*\s+\*\s+\?$", row["cron"]):
                bad("%s 是历史回溯任务，应每天触发一次: %s" % (bean, row["cron"]))
            else:
                ok()
            p = row["params"] or ""
            if '"day"' not in p or '"minutes"' not in p:
                bad("%s 历史回溯 params 必须同时给出 day 与 minutes: %r" % (bean, row["params"]))
            else:
                ok()

    # 7. is_pause
    print("-- 7. is_pause 全部为 0（部署即启用）")
    for bean, row in sorted(by_bean.items()):
        if row["pause"] != 0:
            bad("%s 的 is_pause 应为 0，实际 %d" % (bean, row["pause"]))
        else:
            ok()

    # 8. 幂等形态与 id 唯一
    print("-- 8. 幂等形态（定向 DELETE + INSERT）与显式 id")
    has_delete = re.search(
        r"DELETE\s+FROM\s+`?system_quartz_job`?\s+WHERE\s+`?bean_name`?\s+IN", sql, re.I
    )
    if not has_delete:
        bad("缺少按 bean_name 定向的 DELETE，重复执行会产生重复任务")
    else:
        ok()
    if re.search(r"^\s*INSERT\s+INTO\s+`?system_quartz_job`?", sql, re.I | re.M):
        ok()
    else:
        bad("未找到 INSERT INTO system_quartz_job")
    ids = [r["id"] for r in rows]
    if len(ids) != len(set(ids)):
        bad("显式 id 存在重复")
    else:
        ok()
    if len(by_bean) != len(rows):
        bad("同一 bean_name 出现多次")
    else:
        ok()

    # 9. INSERT 列数 == 每行值数（列序写错是最隐蔽的错，必须机器核对）
    print("-- 9. INSERT 列清单与 values 元组个数一致")
    col_block = re.search(r"INSERT\s+INTO\s+`?system_quartz_job`?\s*\((.*?)\)\s*VALUES",
                          sql, re.I | re.S)
    if not col_block:
        bad("解析不到 INSERT 的列清单")
    else:
        cols = [c.strip().strip("`") for c in col_block.group(1).split(",") if c.strip()]
        required = ["id", "bean_name", "method_name", "params", "cron_expression",
                    "is_pause", "job_name", "remark"]
        for need in required:
            if need not in cols:
                bad("INSERT 列清单缺少必需列: %s" % need)
            else:
                ok()
        # 逐行核对值个数
        bad_rows = 0
        for line in sql.splitlines():
            s = line.strip()
            if not re.match(r"^\(\d{4},\s*'", s):
                continue
            body = s.rstrip(",").rstrip(";").strip()
            if not (body.startswith("(") and body.endswith(")")):
                bad("行未正确闭合: %s" % s[:50])
                bad_rows += 1
                continue
            inner = body[1:-1]
            # 统计顶层逗号（忽略单引号内的逗号）
            n, in_q = 1, False
            for ch in inner:
                if ch == "'":
                    in_q = not in_q
                elif ch == "," and not in_q:
                    n += 1
            if n != len(cols):
                bad("值的个数(%d) != 列数(%d): %s" % (n, len(cols), s[:50]))
                bad_rows += 1
        if bad_rows == 0:
            ok()

    # 10. DELETE 的 bean 集合必须与 INSERT 的 bean 集合完全相同
    print("-- 10. DELETE 清单与 INSERT 清单严格一致（否则重复执行会留下孤儿/重复）")
    del_block = re.search(
        r"DELETE\s+FROM\s+`?system_quartz_job`?\s+WHERE\s+`?bean_name`?\s+IN\s*\((.*?)\)\s*;",
        sql, re.I | re.S)
    if not del_block:
        bad("解析不到 DELETE ... IN (...) 清单")
    else:
        del_b = set(re.findall(r"'([^']+)'", del_block.group(1)))
        ins_b = set(by_bean.keys())
        if del_b != ins_b:
            for x in sorted(ins_b - del_b):
                bad("INSERT 有但 DELETE 没有，重复执行会留下重复行: %s" % x)
            for x in sorted(del_b - ins_b):
                bad("DELETE 有但 INSERT 没有，会误删运维数据: %s" % x)
        else:
            ok()

    # 11. is_del 必须为 0（@TableLogic 逻辑删除，非 0 会被 findByIsPauseIsFalse 过滤掉）
    print("-- 11. is_del 全部为 0（否则 @TableLogic 会把这些行过滤掉，等于没注册）")
    nz = 0
    for line in sql.splitlines():
        s = line.strip()
        if not re.match(r"^\(\d{4},\s*'", s):
            continue
        tail = re.search(r"NOW\(\)\s*,\s*NOW\(\)\s*,\s*(\d+)\s*\)\s*[,;]?\s*$", s)
        if not tail:
            bad("行尾未按 NOW(), NOW(), is_del 结尾: %s" % s[:50])
            nz += 1
        elif tail.group(1) != "0":
            bad("is_del 应为 0，实际 %s: %s" % (tail.group(1), s[:50]))
            nz += 1
    if nz == 0:
        ok()

    # 12. 显式 id 范围：必须避开 id=1（updateIsPause 对 id=1 抛异常）
    print("-- 12. id 避让 1（QuartzJobServiceImpl.updateIsPause 对 id=1 抛「该任务不可操作」）")
    if 1 in ids:
        bad("种子里使用了 id=1，任务失败时自动暂停会抛 BadRequestException")
    else:
        ok()

    print("\n================ 汇总 ================")
    print("通过断言: %d" % passed)
    print("失败断言: %d" % failed)
    if failed:
        print("结果: ❌ FAILED")
        return 1
    print("结果: ✅ ALL PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
