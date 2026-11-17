-- ============================================================
-- 穿山甲激励视频 S2S 服务端回调 - 奖励白名单表
-- 版本：2026-11-17
-- 说明：
--   穿山甲 S2S 回调只是「透传验证」——它证明上游确认用户看完了广告，
--   但回调本身不直接发积分。流程：
--     1) 穿山甲 GET 回调 → 验签通过 → 本表插入一条「可发放」记录
--     2) 客户端 onRewardArrived 上报 /csj/reward → 服务端检查本表白名单
--     3) 两层都过才发积分，发奖后本标记 consumed=1
--
--   这样即使客户端被 hook 伪造 transactionId，没有第 1 步的 S2S 验签记录也拿不到积分。
-- ============================================================

CREATE TABLE IF NOT EXISTS `mw_csj_s2s_whitelist` (
  `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trans_id`       varchar(128) NOT NULL                COMMENT '穿山甲回调 trans_id',
  `uid`            bigint(20)   DEFAULT NULL            COMMENT '回调回传 user_id（若上游带）.',
  `reward_name`    varchar(128) DEFAULT NULL            COMMENT '奖励名称',
  `reward_amount`  int(11)      DEFAULT NULL            COMMENT '奖励数量',
  `extra`          varchar(255) DEFAULT NULL            COMMENT '回调 extra 原样透传',
  `consumed`       tinyint(4)   NOT NULL DEFAULT '0'    COMMENT '是否已消费：0可发放 1已发放',
  `source_ip`      varchar(64)  DEFAULT NULL            COMMENT '回调来源 IP',
  `create_time`    datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`    datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trans_id` (`trans_id`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿山甲 S2S 奖励白名单';

-- 验证
-- SHOW CREATE TABLE mw_csj_s2s_whitelist;
