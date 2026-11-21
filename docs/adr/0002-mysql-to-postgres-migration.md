# ADR-0002：MySQL 5.7 数据库迁移到 PostgreSQL 13 的策略

- 日期：2026-11-20
- 状态：已接受

## 背景

仓库的数据库层由 MySQL 5.7（`mshop.sql`，111 张表）承载，后端为 Java（`com.junchen`，MyBatis-Plus）。
用户决定将数据库迁移到 PostgreSQL 13，产出「转换器脚本 + 人工复核的 dump」两种形式。

## 需求（用户要求）

| 项 | 内容 |
| --- | --- |
| 迁移任务 | 将 MySQL 5.7 数据库（`mshop`，源文件 `docs/sql/mshop.sql`，111 张表）迁移到 PostgreSQL 13 |
| 源文件 | `docs/sql/mshop.sql`（MySQL dump 8.0.19 / Server 5.7.44-log） |
| 产出形式 | 「转换器脚本 + 人工复核的 dump」两种形式 |
| 语言 | 所有对话、文档、生成的 SQL dump 说明统一使用简体中文（见 `AGENTS.md` 语言强制规则） |

## 决策

### 1. 字符集：统一使用 `UTF8`（而非 `utf8mb4`）

MySQL 的 `utf8` 是 3 字节、`utf8mb4` 是 4 字节；PostgreSQL 的 `UTF8` 是 4 字节且统一。
源库 111 张表中，85 张声明 `utf8`、26 张声明 `utf8mb4`，混用导致同一字段在不同表宽度不一致。
**统一为 PostgreSQL 的 `UTF8`**，消除 3/4 字节的歧义。

### 2. 标识符冲突时：重命名

源库存在一批与 PostgreSQL 关键字冲突的列名与索引名（`unique`、`key`、`name`、`type`、
`sort`、`hide`、`pm`、`state`、`level`、`category`等）。
**统一重命名，例如 `key` →  `biz_key`, `unique` → `biz_unique`等,统一添加`biz_`前缀。

### 3. `tinyint(1)` → `boolean`，`bit(1)` → `boolean`

### 4. `json` → `jsonb`

### 5. `ON UPDATE CURRENT_TIMESTAMP` → 触发器函数

PostgreSQL 没有原生「更新时自动写时间戳」的列属性，用 `BEFORE UPDATE` 触发器实现。

### 6. `AUTO_INCREMENT` → 序列（`SERIAL` / 显式 `SEQUENCE`）

保留源库中非默认的种子自增值（如 `system_visits`、`system_quartz_log` 的大值），
作为序列的当前值，避免后续主键回跳。

### 7. 注释通过 `COMMENT ON` 保留

表注释 → `COMMENT ON TABLE`，列注释 → `COMMENT ON COLUMN`，索引注释 → `COMMENT ON INDEX`。

### 8. 外键命名

源库 12 条外键使用自动生成名（如 `FKcngg2qadojhi3a651a5adkacd`）。**重命名为有意义的名称**
（如 `<子表>_<列>_fkey`），保留 `REFERENCES` 关系语义。

### 9. 文件范围

`docs/sql/` 下全部 SQL 文件纳入迁移：`mshop.sql`（111 张表）、`mw_csj_reward_log.sql`、
`mw_csj_s2s_whitelist.sql`（完整建表 + 数据）；`mw_order_tb_foul_columns.sql` 的 `ALTER TABLE`
作为「违规字段」扩展在基础表之后应用；`system_quartz_job.sql` 是文档与示例查询，不属于 schema，跳过。

### 10. `mediumint(N)` → `BIGINT`

`mediumint` 不是 PostgreSQL 类型（最大值约 2.1M）。
**决策：`mediumint` → `BIGINT`**（用户确认，避免高频自增 ID 未来溢出）。

### 11. `datetime` → `TIMESTAMPT`

**决策：`datetime` → `TIMESTAMPT`**（用户确认，统一时区语义；用东八区的 `timestamp` ）。

## 后果

- **`_binary` 字面量（约 16,331 处）**。源库用 `_binary '\x01'`、`_binary '\0'` 表达二进制填充，
  按目标列类型转换为 `bytea` / 文本，集中在 `system_quartz_log`、`system_column_config`、
  `system_menu`。
- **排序一致性**。使用 `pg_catalog.UTF8`（二进制、区分大小写），与源库用于主键/唯一键的
  `utf8mb4_bin` 行为一致；默认列 collation 与 `utf8mb4_bin` 不同，但显式键已用 `_bin`。
- **数据迁移方式**。采用 `INSERT` 语句（非 `COPY`），便于人工复核；行尾符统一为 `\n`。
- **`mw_order_tb_foul_columns` 的 `AFTER` 子句**在 PostgreSQL 中无效（`ADD COLUMN ... AFTER`
  不支持），仅保留列定义与索引，列位置由应用层决定。

## 备选方案（未采用）

- 使用 `COPY` 批量加载：数据量小（种子/参考数据），`INSERT` 更易复核。
- 使用 locale collation（如 `en_US.UTF-8`）：依赖 locale，排序不稳定。
