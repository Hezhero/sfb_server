# ADR-0003：PostgreSQL 13 迁移执行阶段的补充决策

- 日期：2026-11-21
- 状态：已接受
- 关联：ADR-0002（MySQL 5.7 → PostgreSQL 13 迁移策略）

## 背景

在 ADR-0002 确立的整体策略下，本轮完成阶段一（数据层转换与实际导入验证）。
执行过程中依据用户裁决新增或细化了若干决策，其中一项**变更了 ADR-0002 决策 #2**，
一项因 PostgreSQL 引擎限制无法按 ADR-0002 决策 #5 的原计划实现。按领域文档约定，
在此显式标注冲突而不悄悄覆盖。

## 决策

### 1. 列名重命名范围：全部 96 个 PG 关键字列（变更 ADR-0002 决策 #2）

ADR-0002 决策 #2 原版为「保留原名 + 双引号包裹」，本轮用户裁决改为**统一加 `biz_` 前缀重命名**。
实际范围：源 dump 中所有列名与 `pg_get_keywords()`（PG 13.18）交集共 **96 列**
（含非保留字如 `name`×26、`type`×27、`level`×4，以及保留字 `unique`×4、`user`×1）。
命名规则：小写直连 `biz_<原名小写>`，如 `unique` → `biz_unique`、`name` → `biz_name`。

**完整映射**见 `docs/sql/pg/column-mapping.csv`。

_与 ADR-0002 决策 #2 原版（保留原名 + 双引号）冲突，按用户裁决采用重命名方案；
ADR-0002 正文已在执行中被同步更新为重命名表述。_

### 2. tinyint(1) → boolean 采用白名单（细化 ADR-0002 决策 #3）

只有 `bit(1)` 与语义明确的 `tinyint(1)` 标志位列转 `boolean`；码值列（`status`/`type`/
`bind`/`paid`/`score`/`terminal`/`spec_type` 等）保留 `smallint`。
依据：实测 4 个 `tinyint(1)` 列含非 0/1 值（`mw_pay_channel.status`=8、
`mw_store_canvas.terminal`=3、`mw_store_product_reply.product_score`/`service_score`=5），
强转 boolean 会直接损坏数据。`tinyint(2/4/255)` 一律保留整数。

### 3. 索引全量重命名

所有索引统一为 `<表名>_<源索引名>_idx`（唯一键 `_ukey`），主键 `<表名>_pkey`。
源库 24 个索引名跨表重复（PG 要求 schema 内唯一），且同一表内存在
`type` 与 `type_2` 两个索引指向同一列的情况——命名中加入源索引名以保证唯一。

### 4. ON UPDATE CURRENT_TIMESTAMP：逐列独立触发器（偏离 ADR-0002 决策 #5 的实现方式）

ADR-0002 决策 #5 要求用 `BEFORE UPDATE` 触发器；执行方案原选「通用函数 + `TG_ARGV[0]`
指定列名」。但 PostgreSQL 13 的 PL/pgSQL **无法动态给 `NEW.<col>` 记录字段赋值**
（`EXECUTE format('NEW.%I := now()', ...) INTO NEW` 对记录字段无效，
`hstore`/`jsonb` 转换会引入额外扩展依赖）。因此改为**每列一个独立触发函数**
（`set_<表>_<列>()`），共 5 个，语义与 ADR-0002 一致，仅实现方式不同。

### 5. 交付范围：阶段一 + 应用影响报告

本会话只交付数据层（转换脚本 + dump + 实际导入验证）与应用侧改造清单文档，
不改 Java 代码。应用改造（驱动、方言、238 处注解 SQL、实体列名同步 `biz_`）作为独立后续阶段。

### 6. 导入与验证

- 目标实例：`localhost:5431`，PostgreSQL 13.18，UTF8，`Asia/Shanghai`，新建 `mshop` 库。
- 严格模式（Q9=A）：遇错即停，不静默跳过。
- 成功标准（Q8）：111 表 + 26,733 行 + 18 UNIQUE + 12 FK + 93 序列 + 5 触发器 +
  高频 SQL 抽查，全部实测通过。

## 后果

- **应用层负债**：96 列重命名要求同步修改实体 `@TableField`/`@TableId` 注解、
  注解 SQL 中的列名、以及可能的跨端字段映射。影响清单见本次交付的应用报告。
- **boolean 列比较**：转为 boolean 的列在应用 SQL 中若写 `is_del = 0` 会在 PG 报错，
  需改为 `is_del = false`（或 `= 0` 的等价改写），逐项见应用报告。
- **`_binary` 数据**：实测 16,331 处 `_binary` 字面量全部落在 `bit(1)` 列，
  转换时按 `'\x01'`→`true`、`'\0'`→`false` 处理；数据中的 MySQL 反斜杠转义统一解码后
  以 PG 标准字符串字面量输出。

## 阶段二：应用层改造（2026-11-21）

在 Q17–Q22 裁决下完成应用侧改造，决策与结果：

1. **连接配置（Q17/Q18）**：dev 指向 `localhost:5431/mshop`（阶段一验证库），
   prod 保留占位地址但改为 PG 协议与默认端口 5432；时区用
   `options=-c%20TimeZone%3DAsia%2FShanghai`。4 个含明文密码的
   `application-{dev,prod}.yml` 只改本地工作区、不提交、不改暂存区
   （遵守 `working-conventions.md` 第 2 节）。
2. **upsert 注入器（Q19）**：新增 `@UpsertConflict` 注解声明冲突列；
   `MysqlInsertOrUpdateBath` 生成 `INSERT ... ON CONFLICT (...) DO UPDATE SET col=excluded.col`，
   DO UPDATE 集合排除主键与冲突列。两个调用点分别标注 `dtk_id`（mw_dtk_goods）
   与 `uid`（mw_user_fee_log_opt）。方法名 `mysqlInsertOrUpdateBath` 保持不变以兼容调用点。
3. **MySQL 函数改写（Q20）**：`IFNULL`→`COALESCE`(74)、`DATE_FORMAT`→`to_char`(85，含格式掩码
   `%Y`→`YYYY` 等)、`GROUP_CONCAT`→`string_agg`(2)、`STR_TO_DATE`→`to_timestamp`(2)、
   `TO_DAYS` 差值→`INTERVAL '1 day'`(1)、`FIND_IN_SET`→`= ANY(string_to_array(...))`(1)。
4. **boolean 比较**：`is_del = 0` 等 75 处改为 `= false/true`。Q21 将 `mw_express`/`mw_user_recharge`
   的 `is_del`（源 tinyint(4)）统一为 boolean，消除全库分歧——阶段一 dump 已同步重导。
5. **实体与 QueryCriteria 列名**：补 65 处 `@TableField("biz_...")`（至此全库 74 处）；
   `QueryHelpPlus` 绕过 `@TableField` 直接把字段名当列名，因此 12 处
   `QueryCriteria` 的 blurry / propName 也按 CSV 映射改名。
6. **依赖（Q22）**：MyBatis-Plus 3.5.1→3.5.2（本地已缓存、API 兼容），
   删除 mshop-app 中无引用的 3.4.2 版本属性；mysql-connector-j→postgresql 驱动。

### 阶段二验证

- **编译**：沙箱拒绝写 `target/`（maven 卡在 `createdFiles.lst`），改用逐模块 `javac`
  验证——11 个模块 0 错误；mshop-log 的 3 个错误来自未改动的 `LogController.java`
  缺 spring-security 依赖（classpath 环境问题，与迁移无关）。
- **SQL 语义**：在 PG 13 实际执行 12 项改写后的关键 SQL（boolean 比较、COALESCE、to_char、
  to_timestamp、string_agg、ANY、biz_ 列查询、两种 ON CONFLICT upsert），全部通过。
- **残留扫描**：可执行的 MySQL 函数/驱动/方言/连接串均为 0；仅余 4 处 Javadoc 历史措辞。
- **未执行**：`mvn test` 因 target 目录沙箱写限制无法运行（非代码原因）。

## 阶段三：让项目与全部测试跑通（2026-11-21 晚）

阶段二完成后 `mvn test` 实跑 220 个测试、4 个失败（均为连库的 `@SpringBootTest`）。
根因与修复如下（Q23–Q26 裁决：全量 Java 侧转 Boolean，以 `mvn test` 全绿为验收）：

1. **upsert 注入器 bean 启动异常**：`CustomizedSqlInjector` 全局注入到所有 RootMapper，
   对未声明 `@UpsertConflict` 且无主键的实体（如 `JunchenEleOrder`）抛异常导致 Spring 容器启动失败。
   修复：冲突列为空时注入 `ON CONFLICT DO NOTHING` 占位语句（这些方法在相关 mapper 上从不被调用）。
2. **Integer ↔ boolean 错配**：阶段一将 `is_del` 等列转为 boolean，但实体字段仍是 Integer，
   逻辑删除配置仍是数字 1/0。PG 报 `boolean = integer` / `boolean` 列不接受整数参数。
   修复：79 个字段 `Integer/int → Boolean`（含 2 个基类 `BaseDomain`/`BaseDeleteDomain`），
   逻辑删除配置改 `true/false`，`MetaHandler` 填充改 `false`，29 处 Wrapper 枚举值改布尔字面量，
   连带 16+6+1 处 Controller/Service 编译错误逐一修正（`Boolean.TRUE.equals(...)` 避免拆箱 NPE）。
3. **jsonb 写入**：`FastjsonTypeHandler` 把对象序列化成 String 传给 PG jsonb 列被拒。
   新增 `PgJsonbTypeHandler`（继承 `FastjsonTypeHandler`，写入用 `PGobject`），替换 7 个实体上的 12 处引用；
   postgresql 驱动 scope 由 runtime 改 compile。
4. **其它迁移遗留**：
   - `JunchenJdOrder.subsideRate`（源库无此列）加 `@TableField(exist=false)`；
   - `mw_order_jd."Goods_info"` 首字母大写导致自动 SQL 不匹配，统一改小写 `goods_info`
     （转换器 `rename_col` 统一小写列名，PG 库同步 `ALTER TABLE ... RENAME COLUMN`，重导 dump）；
   - `submitOrder` 中 orderId 为 String、订单表 `order_id/parent_id` 为 bigint，
     转 Long 后比较（非数字订单号用 -1，语义同 MySQL）；
   - 补 15 处漏加的 `@TableField("biz_...")`（审计发现注解被相邻字段注解误判而漏加）。

**最终验证**：`mvn test -o` → `Tests run: 220, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

## 备选方案（未采用）

- 保留列名 + 双引号（ADR-0002 原版）：被用户裁决否决。
- 通用触发器函数 + `TG_ARGV`：PG 13 PL/pgSQL 不支持动态赋值 `NEW` 字段。
- `COPY` 批量加载：本库数据量小（26,733 行），`INSERT` 便于人工复核。
- `QueryCriteria` 改用 `@TableField` 联动：QueryHelpPlus 是字符串拼列名，无法自动联动。
- PG boolean 列回退 smallint（阶段三 Q23 选项 B）：会推翻阶段一已接受的 boolean 决策。
- 靠 JDBC 连接参数让 PG 自动把整数转 boolean（阶段三选项 C）：实测 PG 驱动无此隐式转换。
