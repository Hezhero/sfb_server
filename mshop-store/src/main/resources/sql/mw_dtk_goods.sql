-- ============================================================
-- 大淘客商品库（工单 05：商品库同步三件套）
-- 版本：2026-11-16
--
-- 业务背景：C 端此前每屏实时调用大淘客接口，本地不落库，因此无法表达
-- 「已下架」「已变价」两种状态——用户点进去发现无法下单或价格不符，
-- 是返利类 App 投诉最集中的问题。本表承接三个上游接口的结果：
--   get-newest-goods       （id=3， v1.2.0）增量商品 → 插入/更新
--   get-stale-goods-by-time（id=11，v1.0.1）失效商品 → 按 dtk_id 打标记
--   pull-goods-by-time     （id=12，v1.2.3）全量拉取 → 插入/更新（字段最全）
--
-- 【为什么新建表】仓库原先没有任何 CPS 商品库表：
--   mw_store_product 是自营商城商品（price/stock/is_show），字段与淘宝客商品
--   完全不搭；mw_wechat_live_goods 是微信直播商品，无关。
--   而三个上游接口返回的是原价/券后价/佣金比例/券链接这一套，必须单独建表。
--
-- 【匹配键 dtk_id】官方失效接口在响应字段 id 上明确标注
--   「大淘客商品id（用该ID进行匹配）」，所以本地业务唯一键就是 dtk_id。
--   不用 goods_sign 作键：官方说明它「每次转链后都会变化」，且多处标注「暂时为空」。
--   不用 goods_id 作键：官方类型是 String 且非必填，用它匹配会漏。
--
-- 【写入方式】ON DUPLICATE KEY UPDATE 依赖 uk_dtk_id，
--   服务层用 mysqlInsertOrUpdateBath 分片批量 upsert。
--
-- 说明：本 DDL 放在 mshop-store/src/main/resources/sql/ 而不是 docs/sql/，
-- 因为本工单写集不含 docs/sql（该目录由另一子代理并行维护）。
-- 上线时请把文件交给 DBA 执行。
-- ============================================================


-- ============================================================
-- 1. 商品库主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `mw_dtk_goods` (
  `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '本地主键',
  `dtk_id`             bigint(20)    NOT NULL                COMMENT '大淘客商品id（官方响应字段 id，业务唯一键与失效匹配键）',
  `goods_id`           varchar(64)   DEFAULT NULL            COMMENT '淘宝商品id（官方 goods_id）',
  `goods_sign`         varchar(128)  DEFAULT NULL            COMMENT '新商品id（官方 goods_sign，每次转链后变化，可能为空）',
  `item_link`          varchar(512)  DEFAULT NULL            COMMENT '商品淘宝链接',
  `title`              varchar(512)  DEFAULT NULL            COMMENT '淘宝标题',
  `dtitle`             varchar(255)  DEFAULT NULL            COMMENT '短标题',
  `description`        text                                   COMMENT '推广文案（官方 desc）',
  `cid`                bigint(20)    DEFAULT NULL            COMMENT '大淘客一级分类id',
  `subcid`             varchar(255)  DEFAULT NULL            COMMENT '大淘客二级分类id，多个用英文逗号分隔',
  `main_pic`           varchar(512)  DEFAULT NULL            COMMENT '商品主图链接',
  `marketing_main_pic` varchar(512)  DEFAULT NULL            COMMENT '营销主图链接',
  `original_price`     decimal(12,2) DEFAULT NULL            COMMENT '商品原价',
  `actual_price`       decimal(12,2) DEFAULT NULL            COMMENT '券后价（最易变价，每轮同步覆盖）',
  `coupon_price`       decimal(12,2) DEFAULT NULL            COMMENT '优惠券金额',
  `coupon_link`        varchar(1024) DEFAULT NULL            COMMENT '优惠券链接',
  `coupon_remain_count` bigint(20)   DEFAULT NULL            COMMENT '优惠券剩余量',
  `coupon_receive_num` bigint(20)    DEFAULT NULL            COMMENT '优惠券领券量',
  `commission_rate`    decimal(8,2)  DEFAULT NULL            COMMENT '佣金比例',
  `month_sales`        bigint(20)    DEFAULT NULL            COMMENT '30天热销',
  `shop_name`          varchar(255)  DEFAULT NULL            COMMENT '店铺名称',
  `seller_id`          varchar(64)   DEFAULT NULL            COMMENT '淘宝卖家id',
  `goods_create_time`  datetime      DEFAULT NULL            COMMENT '商品上架时间（官方 create_time）',

  -- 同步状态
  `goods_status`       tinyint(4)    NOT NULL DEFAULT '1'    COMMENT '商品状态：0失效（已下架） 1正常',
  `source`             varchar(32)   DEFAULT NULL            COMMENT '来源：newest=商品更新 pull=定时拉取',
  `stale_time`         datetime      DEFAULT NULL            COMMENT '失效时间（上游不下发下架时间，取被检测到的时刻）',
  `last_sync_time`     datetime      DEFAULT NULL            COMMENT '最后一次同步时间',

  `create_time`        datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_time`        datetime      DEFAULT NULL            COMMENT '更新时间',
  `is_del`             tinyint(4)    DEFAULT '0'             COMMENT '逻辑删除：0正常 1删除',

  PRIMARY KEY (`id`),
  -- UPSERT 依赖这个唯一键：ON DUPLICATE KEY UPDATE 才会命中并覆盖价格
  UNIQUE KEY `uk_dtk_id` (`dtk_id`),
  KEY `idx_goods_id` (`goods_id`),
  -- 前端过滤「排除失效商品」会按这个索引扫
  KEY `idx_status_sync` (`goods_status`, `last_sync_time`),
  KEY `idx_cid` (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大淘客商品库（商品更新/失效/定时拉取三件套落库）';


-- ============================================================
-- 2. 验证
-- ============================================================
-- SHOW CREATE TABLE mw_dtk_goods;
--
-- 查当前失效商品数（前端应当过滤掉的量级）：
-- SELECT goods_status, COUNT(*) FROM mw_dtk_goods GROUP BY goods_status;
--
-- 查同步新鲜度（超过 24 小时未同步说明任务没跑起来）：
-- SELECT source, COUNT(*) AS total,
--        MIN(last_sync_time) AS oldest, MAX(last_sync_time) AS newest
--   FROM mw_dtk_goods WHERE is_del = 0 GROUP BY source;
--
-- 验证失效匹配确实按 dtk_id（抽样比对上游 id）：
-- SELECT dtk_id, goods_id, goods_status, stale_time
--   FROM mw_dtk_goods WHERE goods_status = 0 ORDER BY stale_time DESC LIMIT 20;
--
-- 查价格是否有更新（券后价变化过的商品）：
-- SELECT dtk_id, title, actual_price, update_time
--   FROM mw_dtk_goods
--  WHERE is_del = 0 AND actual_price IS NOT NULL
--  ORDER BY update_time DESC LIMIT 20;
