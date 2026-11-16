-- 违规订单标记字段（工单 04）
-- 淘宝联盟判定订单违规（店铺淘宝客 / 订单虚假交易）后，平台会冻结或扣除佣金。
-- 本地必须据此把这些订单排除出结算，否则结算金额系统性高于实际到账。
--
-- 设计原则：只用独立的 foul_* 三列，绝不复用 bind / tk_status / refund_tag：
-- 那三个字段是退款冲正（OrderRefundTask → decHbAndRefundOrder）的选单依据，
-- 一旦被违规逻辑改写，冲正会漏单或被误触发，两种资损叠加。
--
-- 重要：foul_* 字段加在 TkOrder 基类上，所有 7 个平台的订单表都继承它，
-- 因此每张表都必须加列，否则 MyBatis-Plus 的 SELECT 会带出不存在的列而报
-- "Unknown column 'foul_status' in 'field list'"。

ALTER TABLE `mw_order_tb` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态：0/NULL=正常，1=违规冻结' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_tb` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型：1=店铺淘宝客，2=订单虚假交易' AFTER `foul_status`;
ALTER TABLE `mw_order_tb` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;
ALTER TABLE `mw_order_tb` ADD INDEX `idx_foul_status` (`foul_status`);

ALTER TABLE `mw_order_jd` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_jd` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型' AFTER `foul_status`;
ALTER TABLE `mw_order_jd` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;

ALTER TABLE `mw_order_pdd` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_pdd` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型' AFTER `foul_status`;
ALTER TABLE `mw_order_pdd` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;

ALTER TABLE `mw_order_dy` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_dy` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型' AFTER `foul_status`;
ALTER TABLE `mw_order_dy` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;

ALTER TABLE `mw_order_vip` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_vip` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型' AFTER `foul_status`;
ALTER TABLE `mw_order_vip` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;

ALTER TABLE `mw_order_mt` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态' AFTER `spread_hb_time`;
ALTER TABLE `mw_order_mt` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型' AFTER `foul_status`;
ALTER TABLE `mw_order_mt` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间' AFTER `violation_type`;

-- 注意：mw_order_ele 表结构不同（无 spread_hb_time 列），因此不指定 AFTER
ALTER TABLE `mw_order_ele` ADD COLUMN `foul_status` INT NULL DEFAULT 0 COMMENT '违规状态';
ALTER TABLE `mw_order_ele` ADD COLUMN `violation_type` INT NULL COMMENT '违规类型';
ALTER TABLE `mw_order_ele` ADD COLUMN `foul_time` DATETIME NULL COMMENT '违规发现时间';
