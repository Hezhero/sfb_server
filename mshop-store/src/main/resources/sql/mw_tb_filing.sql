-- ============================================================
-- 推客淘宝渠道备案（工单 03：会员备案接入）
-- 版本：2026-11-16
--
-- 业务背景：淘宝联盟要求推广者与渠道方建立备案关系，未备案的推广行为
-- 无法归因，佣金从源头就断。这是 B2B2C 模式的合法性前提。
--
-- 两张表的分工：
--   mw_tb_filing      —— 每个推客当前备案状态（一条记录，覆盖更新）
--   mw_tb_filing_log  —— 状态变更留痕（只在状态真正变化时写一条）
-- 结算争议时用后者证明「某笔订单发生时该推客处于什么状态」。
--
-- 官方 get-publisher-info 返回的 inviter_list 是数组（一个 externalId 可能有
-- 多条备案记录），存储策略：mw_tb_filing 只存 inviter_list 的**最新一条**
-- （special_id / root_pid / account_name），另用 total_count 记录总数，
-- 既避免为明细再开一张表，又不会丢掉「有几条」这个关键信息。
-- 官方不返回状态字段，relation_status 由 total_count>0 且 inviter_list 非空推断。
--
-- 说明：本 DDL 放在 mshop-store/src/main/resources/sql/ 而不是 docs/sql/，
-- 因为本工单的写集不含 docs/sql（该目录由另一子代理并行维护）。
-- 上线时请把两个文件一并交给 DBA 执行。
-- ============================================================


-- ============================================================
-- 1. 推客备案状态表
-- ============================================================
CREATE TABLE IF NOT EXISTS `mw_tb_filing` (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid`             bigint(20)   NOT NULL                COMMENT '推客用户uid（上游 externalId 即其字符串形式）',
  `tb_pid`          varchar(64)  DEFAULT NULL            COMMENT '渠道推广位pid（来自官方 root_pid，仅用于追溯）',
  `relation_status` tinyint(4)   NOT NULL DEFAULT '0'    COMMENT '备案状态：1已备案 0未备案（由 total_count>0 推断）',
  `filed_time`      datetime     DEFAULT NULL            COMMENT '备案完成时间',
  `relation_url`    varchar(512) DEFAULT NULL            COMMENT '备案授权链接',
  `special_id`      bigint(20)   DEFAULT NULL            COMMENT '会员关系ID sid（官方 special_id）',
  `root_pid`        varchar(64)  DEFAULT NULL            COMMENT '推广位PID（官方 root_pid）',
  `account_name`    varchar(128) DEFAULT NULL            COMMENT '会员淘宝昵称（官方 account_name）',
  `total_count`     int(11)      DEFAULT NULL            COMMENT '备案记录总数（官方 total_count）',
  `remark`          varchar(255) DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `is_del`          tinyint(4)   DEFAULT '0'             COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_status` (`relation_status`),
  KEY `idx_special_id` (`special_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推客淘宝渠道备案信息';


-- ============================================================
-- 2. 推客备案状态变更记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `mw_tb_filing_log` (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid`             bigint(20)   NOT NULL                COMMENT '推客用户uid（上游 externalId 即其字符串形式）',
  `tb_pid`          varchar(64)  DEFAULT NULL            COMMENT '备案使用的渠道推广位pid',
  `special_id`      bigint(20)   DEFAULT NULL            COMMENT '变更时的会员关系ID sid（官方 special_id）',
  `from_status`     tinyint(4)   DEFAULT NULL            COMMENT '变更前状态：1已备案 0未备案，NULL表示首次记录',
  `relation_status` tinyint(4)   NOT NULL                COMMENT '变更后状态：1已备案 0未备案',
  `remark`          varchar(255) DEFAULT NULL            COMMENT '变更说明',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `is_del`          tinyint(4)   DEFAULT '0'             COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_uid_create` (`uid`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推客备案状态变更记录';


-- ============================================================
-- 验证
-- ============================================================
-- SHOW CREATE TABLE mw_tb_filing;
-- SHOW CREATE TABLE mw_tb_filing_log;
--
-- 查未备案推客（运营引导用）：
-- SELECT f.uid, f.relation_status, f.create_time
--   FROM mw_tb_filing f
--  WHERE f.relation_status = 0 AND f.is_del = 0
--  ORDER BY f.create_time DESC;
--
-- 查某推客的备案变更时间线（结算争议取证）：
-- SELECT * FROM mw_tb_filing_log WHERE uid = ? ORDER BY create_time DESC;
--
-- 备案明细对账（sids 与推广位）：
-- SELECT uid, special_id, root_pid, account_name, total_count, filed_time
--   FROM mw_tb_filing WHERE relation_status = 1 AND is_del = 0;
