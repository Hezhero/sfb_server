package com.junchen.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明批量 upsert 的冲突目标列（PostgreSQL ON CONFLICT）。
 *
 * <p>MySQL 5.7 的 {@code ON DUPLICATE KEY UPDATE} 依赖任意唯一索引自动判定冲突；
 * PostgreSQL 13 必须在 {@code ON CONFLICT} 中显式指定冲突列（唯一索引/主键）。
 * 该注解标注在实体类上，供自定义 SQL 注入器读取。</p>
 *
 * <p>示例：{@code @UpsertConflict("dtk_id")} 表示按 {@code dtk_id} 唯一键判重。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UpsertConflict {

    /** 冲突目标列（物理列名，可多个组成联合唯一键）。 */
    String[] value();
}
