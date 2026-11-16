-- ============================================================
-- 穿山甲（Pangle）激励视频广告 - 奖励记录表
-- 版本：2026-11-02
-- 说明：transaction_id 建唯一索引，是防重放的核心；
--       idx_uid_create 支撑"今日次数"与"最小间隔"两项查询。
-- ============================================================

CREATE TABLE IF NOT EXISTS `mw_csj_reward_log` (
  `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid`            bigint(20)   NOT NULL                COMMENT '用户uid',
  `transaction_id` varchar(128) NOT NULL                COMMENT '穿山甲奖励流水号，全局唯一',
  `pos_id`         varchar(64)  DEFAULT NULL            COMMENT '广告位ID',
  `integral`       int(11)      NOT NULL DEFAULT '0'    COMMENT '发放积分',
  `status`         tinyint(4)   NOT NULL DEFAULT '1'    COMMENT '状态：1已发放 2已拒绝',
  `remark`         varchar(255) DEFAULT NULL            COMMENT '备注/拒绝原因',
  `ip`             varchar(64)  DEFAULT NULL            COMMENT '请求IP',
  `create_time`    datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`    datetime     DEFAULT NULL            COMMENT '更新时间',
  `is_del`         tinyint(4)   DEFAULT '0'             COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  KEY `idx_uid_create` (`uid`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿山甲激励视频奖励记录';


-- ============================================================
-- 可选：在后台菜单里加入口（如果你的后台菜单是数据库驱动的）
-- 不执行也不影响接口，只是后台看不到"广告奖励"记录页
-- ============================================================
-- INSERT INTO `sys_menu` (`parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `create_time`)
-- VALUES (0, '广告奖励', '/csj/reward', 'csj:reward:list', 1, 'el-icon-video-camera', 90, NOW());


-- ============================================================
-- 验证
-- ============================================================
-- SHOW CREATE TABLE mw_csj_reward_log;
-- SELECT COUNT(*) FROM mw_csj_reward_log;
