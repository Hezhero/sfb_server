package com.junchen.common.batch;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.junchen.annotation.UpsertConflict;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * PostgreSQL 版批量 upsert 注入器（保留方法名 mysqlInsertOrUpdateBath 以兼容调用点）。
 *
 * <p>MySQL 原实现使用 {@code INSERT ... ON DUPLICATE KEY UPDATE col=values(col)}；
 * PG 13 不支持该语法，改为 {@code INSERT ... ON CONFLICT (cols) DO UPDATE SET col=excluded.col}。
 * 冲突列由实体上的 {@link UpsertConflict} 注解声明；DO UPDATE 集合排除主键与冲突列。</p>
 *
 * <p><b>重要</b>：本方法由 {@link CustomizedSqlInjector} 全局注入到所有 RootMapper 子类，
 * 但只有少数实体真正调用它。对未声明 {@link UpsertConflict} 且无主键的实体，
 * 不能抛异常（否则 Spring 容器启动失败），而是注入一条合法的
 * {@code ON CONFLICT DO NOTHING} 占位语句——该方法在这些 mapper 上从不被调用。</p>
 */
public class MysqlInsertOrUpdateBath extends AbstractMethod {

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 冲突列：注解优先；未声明时回退主键列；都没有则为空（生成 DO NOTHING 占位）
        Set<String> conflictCols = resolveConflictColumns(modelClass, tableInfo);

        final String tableName = tableInfo.getTableName();
        final String fieldSql = prepareFieldSql(tableInfo);
        final String valuesSql = prepareModelValuesSql(tableInfo);

        final String onConflictSql;
        if (conflictCols.isEmpty()) {
            // 无冲突目标：合法占位语句（该方法在此类 mapper 上从不被调用）
            onConflictSql = "ON CONFLICT DO NOTHING";
        } else {
            final String conflictSql = String.join(", ", conflictCols);
            final String updateSql = prepareUpdateSql(tableInfo, conflictCols);
            onConflictSql = "ON CONFLICT (" + conflictSql + ") "
                    + (updateSql == null ? "DO NOTHING" : "DO UPDATE SET " + updateSql);
        }

        final String sqlResult = String.format(
                "<script>INSERT INTO %s %s VALUES %s %s</script>",
                tableName, fieldSql, valuesSql, onConflictSql);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlResult, modelClass);
        return this.addInsertMappedStatement(mapperClass, modelClass, "mysqlInsertOrUpdateBath",
                sqlSource, new NoKeyGenerator(), null, null);
    }

    /** 读取实体上的 @UpsertConflict；未标注则回退到主键列；都没有则返回空集。 */
    private Set<String> resolveConflictColumns(Class<?> modelClass, TableInfo tableInfo) {
        Set<String> cols = new LinkedHashSet<>();
        UpsertConflict ann = modelClass.getAnnotation(UpsertConflict.class);
        if (ann != null && ann.value().length > 0) {
            for (String c : ann.value()) {
                cols.add(normalize(c));
            }
        } else if (!StringUtils.isEmpty(tableInfo.getKeyColumn())) {
            cols.add(normalize(tableInfo.getKeyColumn()));
        }
        return cols;
    }

    /** 构造 DO UPDATE SET：排除主键与冲突列，其余列用 excluded.<col> 更新。 */
    private String prepareUpdateSql(TableInfo tableInfo, Set<String> conflictCols) {
        StringBuilder sb = new StringBuilder();
        String pk = StringUtils.isEmpty(tableInfo.getKeyColumn()) ? null : normalize(tableInfo.getKeyColumn());
        for (TableFieldInfo f : tableInfo.getFieldList()) {
            String col = normalize(f.getColumn());
            if (col.equals(pk) || conflictCols.contains(col)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(col).append(" = excluded.").append(col);
        }
        if (sb.length() == 0) {
            return null; // 除主键/冲突列外无其它列 → DO NOTHING
        }
        return sb.toString();
    }

    /** 去掉列定义中可能残留的反引号。 */
    private String normalize(String col) {
        return col == null ? null : col.replace("`", "").trim();
    }

    /** (col1, col2, ...) —— 无主键时只列普通字段。 */
    private String prepareFieldSql(TableInfo tableInfo) {
        StringBuilder sb = new StringBuilder("(");
        boolean hasKey = !StringUtils.isEmpty(tableInfo.getKeyColumn());
        if (hasKey) {
            sb.append(normalize(tableInfo.getKeyColumn())).append(",");
        }
        for (TableFieldInfo f : tableInfo.getFieldList()) {
            sb.append(normalize(f.getColumn())).append(",");
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }

    /** (<foreach ...>#{item.prop},...</foreach>) —— 无主键时只列普通属性。 */
    private String prepareModelValuesSql(TableInfo tableInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<foreach collection=\"list\" item=\"item\" index=\"index\" open=\"(\" separator=\"),(\" close=\")\">");
        if (!StringUtils.isEmpty(tableInfo.getKeyProperty())) {
            sb.append("#{item.").append(tableInfo.getKeyProperty()).append("},");
        }
        for (TableFieldInfo f : tableInfo.getFieldList()) {
            sb.append("#{item.").append(f.getProperty()).append("},");
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append("</foreach>");
        return sb.toString();
    }
}
