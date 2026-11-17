-- ============================================================================
-- 定时任务注册种子：system_quartz_job
-- ----------------------------------------------------------------------------
-- 版本：2026-11-16
-- 对应工单：02-Quartz 任务 SQL 种子
-- 适用库：MySQL 5.7+ / 8.0（utf8mb4）
--
-- 【为什么需要这个脚本】
-- 本项目所有定时任务都由数据库表 system_quartz_job 驱动，代码里没有任何
-- @Scheduled 注解。启动链如下（详见 docs/sql/README-quartz.md）：
--   JobRunner(ApplicationRunner) → 查 is_pause=0 的记录 → QuartzManage.addJob
--   → Quartz 到点触发 ExecutionJob → QuartzRunnable 反射调用 bean.methodName
-- 因此：**这张表为空 = 31 个任务全部不执行**。订单采集、退款冲正、计费预估、
-- 会员过期等等会静默失效，且不会有任何报错，只在业务上表现为"一条订单都不回流"。
--
-- 【任务类清单说明】
-- mshop-admin/src/main/java/com/mailvor/modules/quartz/task/ 下共 39 个 .java，
-- 但其中 OrderTask 是**基类**（只提供 saveTb/saveJd/savePdd 等落库方法，
-- 没有 run 方法，不能注册成任务）。所以可注册任务是 **38 个**。
-- 本脚本录入 38 条。
--
-- 【2026-11-17 修订】
-- 初版只录了 30 条（对应当时的任务类）。工单 04/05/06/07/08 之后新增的
-- 8 个采集任务漏登记，部署后会静默不执行。本次补录至 38 条，
-- 并新增一条「任务类 vs 种子」的对账测试防止复发。
--
-- 【幂等】
-- 先按 bean_name 定向 DELETE，再 INSERT，且每条记录使用**固定显式 id**。
-- 重复执行任意次，结果一致，不会产生重复任务。
-- 之所以不用 INSERT ... ON DUPLICATE KEY UPDATE：system_quartz_job 的
-- bean_name 上没有唯一索引（建表脚本不在仓库内，无法保证），ON DUPLICATE KEY
-- 会静默退化成普通 INSERT，产生重复行。DELETE + INSERT 不依赖任何索引。
--
-- 【执行方式】
--   mysql -h<host> -u<user> -p <dbname> < docs/sql/system_quartz_job.sql
--   或在客户端里整段执行，然后**重启 mshop-admin**（JobRunner 只在启动时注册）。
--
-- 【多机部署】
-- 只有 quartz.main=true 的节点会真正执行任务，详见 README-quartz.md 第 3 节。
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 步骤 0：前置检查（可选，先手工执行确认表结构与依赖）
-- ----------------------------------------------------------------------------
-- SHOW CREATE TABLE system_quartz_job;
-- SELECT COUNT(*) AS existing FROM system_quartz_job;


-- ----------------------------------------------------------------------------
-- 步骤 1：幂等清理 —— 只删除本脚本负责的这 30 个 bean，不触碰运维手工建立的
--         其它任务（例如白名单里历史遗留的 tbOrderBindTask / pidPoolTask）。
-- ----------------------------------------------------------------------------
DELETE FROM `system_quartz_job`
WHERE `bean_name` IN (
  'tbOrderCollectionTask',
  'jdOrderCollectionTask',
  'pddOrderCollectionTask',
  'vipOrderCollectionTask',
  'dyOrderCollectionTask',
  'dyKuOrderCollectionTask',
  'eleKuOrderCollectionTask',
  'orderMtCPSCollectionTask',
  'csjEcomOrderCollectionTask',
  'tbOrderHistoryCollectionTask',
  'jdOrderHistoryCollectionTask',
  'pddOrderHistoryCollectionTask',
  'vipOrderHistoryCollectionTask',
  'dyOrderHistoryCollectionTask',
  'dyKuOrderHistoryCollectionTask',
  'eleKuOrderHistoryCollectionTask',
  'orderMtCPSHistoryCollectionTask',
  'fee7DayTask',
  'feeMonthTask',
  'feeProMonthTask',
  'feeTodayTask',
  'feeTotalTask',
  'feeTriggerTask',
  'orderRefundTask',
  'orderCheckTask',
  'selfOrderHbTask',
  'billUnlockTask',
  'memberExpiredTask',
  'payResetTask',
  'visitsTask',
  -- 2026-11-17 补录：工单 04/05/06/07/08 新增的采集任务（此前漏登记）
  'ksOrderCollectionTask',
  'ksOrderHistoryCollectionTask',
  'idleOrderCollectionTask',
  'tbLiveOrderCollectionTask',
  'foulOrderCollectionTask',
  'dtkGoodsNewestTask',
  'dtkGoodsPullTask',
  'dtkGoodsStaleTask'
);


-- ----------------------------------------------------------------------------
-- 步骤 2：插入 30 条任务
--
-- 列顺序：id, bean_name, method_name, params, cron_expression, is_pause,
--         job_name, remark, create_time, update_time, is_del
--
-- 约定（务必遵守，否则运行期反射失败）：
--   * bean_name   = Spring Bean 名 = 类名首字母小写
--   * method_name = run（全部 30 个任务都在自身类中声明了 run）
--   * params      = QuartzRunnable 的判断依据：
--                   params 非空 → 反射找 run(String.class)
--                   params 为空 → 反射找 run()
--                   因此只有 VisitsTask 用 NULL，其余 29 个必须给非空参数
--   * cron        = Quartz 格式，**秒在最前**（6 或 7 段），与 Linux crontab 不同
--   * is_pause    = 0 启用 / 1 暂停
--   * id          从 1001 起分配，避开 id=1（见文末说明）
-- ----------------------------------------------------------------------------
INSERT INTO `system_quartz_job`
  (`id`, `bean_name`, `method_name`, `params`, `cron_expression`, `is_pause`,
   `job_name`, `remark`, `create_time`, `update_time`, `is_del`)
VALUES
-- ===== 一、订单采集（实时）：9 个 =====
-- 每 10 分钟拉一次，参数 minutes=10 与 cron 周期严格对齐：
-- 相邻两次的查询窗口首尾相接，既不重复也不遗漏。
(1001, 'tbOrderCollectionTask',         'run', '{"minutes":10}', '0 0/10 * * * ?', 0, '淘宝订单采集',     '实时采集：淘宝联盟订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1002, 'jdOrderCollectionTask',         'run', '{"minutes":10}', '0 2/10 * * * ?', 0, '京东订单采集',     '实时采集：京东联盟订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1003, 'pddOrderCollectionTask',        'run', '{"minutes":10}', '0 4/10 * * * ?', 0, '拼多多订单采集',   '实时采集：拼多多订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1004, 'vipOrderCollectionTask',        'run', '{"minutes":10}', '0 6/10 * * * ?', 0, '唯品会订单采集',   '实时采集：唯品会联盟订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1005, 'dyOrderCollectionTask',         'run', '{"minutes":10}', '0 8/10 * * * ?', 0, '抖音订单采集',     '实时采集：抖音（大淘客）订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1006, 'dyKuOrderCollectionTask',       'run', '{"minutes":10}', '0 1/10 * * * ?', 0, '抖音-好单库订单采集', '实时采集：抖音订单（好单库渠道），每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1007, 'eleKuOrderCollectionTask',      'run', '{"minutes":10}', '0 3/10 * * * ?', 0, '饿了么-好单库订单采集', '实时采集：饿了么订单（好单库渠道），每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1008, 'orderMtCPSCollectionTask',      'run', '{"minutes":10}', '0 5/10 * * * ?', 0, '美团CPS订单采集',   '实时采集：美团 CPS 订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1009, 'csjEcomOrderCollectionTask',    'run', '{"minutes":10}', '0 7/10 * * * ?', 0, '穿山甲电商订单采集', '实时采集：穿山甲电商联盟订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),

-- ===== 二、订单采集（历史回溯）：8 个 =====
-- 每天凌晨低峰跑一次，按 day × minutes 切时间窗逐段回补，用于补齐
-- 实时采集漏单、以及平台延迟结算的订单。params 必须同时给 day 与 minutes。
-- day=90 minutes=60 → 每次任务切 2160 个时间窗，任务本身可能跑很久；
-- 若上游接口限流，把 day 调小（如 7）或 minutes 调大（如 180）即可。
(1101, 'tbOrderHistoryCollectionTask',      'run', '{"day":90,"minutes":60}', '0 30 1 * * ?', 0, '淘宝历史订单回溯',   '历史回溯：每天 01:30 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1102, 'jdOrderHistoryCollectionTask',      'run', '{"day":90,"minutes":60}', '0 0 2 * * ?',  0, '京东历史订单回溯',   '历史回溯：每天 02:00 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1103, 'pddOrderHistoryCollectionTask',     'run', '{"day":90,"minutes":60}', '0 30 2 * * ?', 0, '拼多多历史订单回溯', '历史回溯：每天 02:30 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1104, 'vipOrderHistoryCollectionTask',     'run', '{"day":90,"minutes":60}', '0 0 3 * * ?',  0, '唯品会历史订单回溯', '历史回溯：每天 03:00 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1105, 'dyOrderHistoryCollectionTask',      'run', '{"day":90,"minutes":60}', '0 30 3 * * ?', 0, '抖音历史订单回溯',   '历史回溯：每天 03:30 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1106, 'dyKuOrderHistoryCollectionTask',    'run', '{"day":90,"minutes":60}', '0 0 4 * * ?',  0, '抖音-好单库历史回溯', '历史回溯：每天 04:00 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1107, 'eleKuOrderHistoryCollectionTask',   'run', '{"day":90,"minutes":60}', '0 30 4 * * ?', 0, '饿了么-好单库历史回溯', '历史回溯：每天 04:30 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),
(1108, 'orderMtCPSHistoryCollectionTask',   'run', '{"day":90,"minutes":60}', '0 0 5 * * ?',  0, '美团CPS历史订单回溯', '历史回溯：每天 05:00 回补最近 90 天订单，按 60 分钟切窗', NOW(), NOW(), 0),

-- ===== 三、计费 / 佣金预估：6 个 =====
-- 参数是**数字字符串**（不是 JSON）：代码里是 Integer.parseInt(sizeStr)，
-- 表示"每批处理多少个用户"。给 "100" 即可。
-- 注意：这 5 个 Fee* 任务的参数必须非空，否则 parseInt(null) 抛
--       NumberFormatException；只有 feeTriggerTask 的入参是逗号分隔 uid 串，
--       传空串是有意义的（只处理 Redis 里的今日 uid），但传空串会让
--       QuartzRunnable 改走无参分支而失败，所以这里给它 "0" 占位，
--       代码会把它当 uid 加入列表（uid=0 查不到记录，等价于空跑）。
(1201, 'feeTodayTask',   'run', '100', '0 0/1 * * * ?',  0, '今日预估计费',   '计费：每分钟刷新当日有更新用户的今日预估（type=1）', NOW(), NOW(), 0),
(1202, 'fee7DayTask',    'run', '100', '0 0/20 * * * ?', 0, '7日预估计费',    '计费：每 20 分钟刷新近 7 天预估（type=6）', NOW(), NOW(), 0),
(1203, 'feeMonthTask',   'run', '100', '0 5/20 * * * ?', 0, '本月预估计费',   '计费：每 20 分钟刷新本月预估（type=3）', NOW(), NOW(), 0),
(1204, 'feeProMonthTask','run', '100', '0 5 0 1 * ?',    0, '上月预估计费',   '计费：每月 1 日 00:05 统计上月预估（type=4）', NOW(), NOW(), 0),
(1205, 'feeTotalTask',   'run', '100', '0 30 6 * * ?',   0, '全量预估计费',   '计费：每天 06:30 全量重算所有用户预估', NOW(), NOW(), 0),
(1206, 'feeTriggerTask', 'run', '0',   '0 0/1 * * * ?',  0, '实时计费触发',   '计费：每分钟消费 Redis 中的今日 uid 队列并刷新预估', NOW(), NOW(), 0),

-- ===== 四、订单后处理：4 个 =====
-- 退款冲正 / 风控检查 / 自购拆红包 / 账单解锁，都是幂等扫描型任务，
-- 按分钟级频率扫表即可，不宜过快（会与订单表产生锁竞争）。
(1301, 'orderRefundTask', 'run', '{"prior":30}',  '0 0/5 * * * ?',  0, '订单退款冲正',     '后处理：每 5 分钟扫描最近 30 天失效订单，扣回红包积分', NOW(), NOW(), 0),
(1302, 'orderCheckTask',  'run', '{}',            '0 10/30 * * * ?', 0, '订单风控检查',     '后处理：每 30 分钟检查高退款风险用户，调整拆红包次数', NOW(), NOW(), 0),
(1303, 'selfOrderHbTask', 'run', '{"limit":20}',  '0 2/10 * * * ?', 0, '自购拆红包',       '后处理：每 10 分钟处理 20 条已过解锁期的自购订单拆红包', NOW(), NOW(), 0),
(1304, 'billUnlockTask',  'run', '{"limit":20}',  '0 4/10 * * * ?', 0, '账单解锁',         '后处理：每 10 分钟解冻 20 条到期账单并回补余额', NOW(), NOW(), 0),

-- ===== 五、用户 / 运营：3 个 =====
(1401, 'memberExpiredTask', 'run', '{}', '0 0 0 * * ?',  0, '会员过期处理',     '运营：每天 00:00 扫描并处理会员过期', NOW(), NOW(), 0),
(1402, 'payResetTask',      'run', '{}', '0 0 0 1 * ?',  0, '支付通道额度重置', '运营：每月 1 日 00:00 重置支付通道额度', NOW(), NOW(), 0),
(1403, 'visitsTask',        'run', NULL, '0 0 0 * * ?',  0, '站点访问统计',     '运营：每天 00:00 写入当日访问统计基线（无参任务，params 必须为 NULL）', NOW(), NOW(), 0),

-- ===== 六、2026-11-17 补录：工单 04/05/06/07/08 新增的采集任务 =====
-- 【为什么补录】本脚本初版对应工单 02，只登记了当时的 30 个任务。
-- 之后工单 04（违规订单）、05（商品库同步三件套）、06（快手）、07（闲鱼）、
-- 08（淘宝更高佣）陆续新增了 8 个任务类，但都没补进种子 —— 部署后它们
-- 不会执行，也不会报错，表现为「新平台的订单一条都不回流」。
-- 代码里无 @Scheduled，任务类存在 ≠ 任务会跑；这张表才是执行源。
--
-- 【cron 与窗口的对应关系】
-- 实时采集类：每 10 分钟，代码内默认窗口也是 10 分钟，首尾相接不重复不遗漏。
-- 历史回溯类：每天凌晨错峰，单次窗口由代码内部分段（快手为 1 天/段）。
(1404, 'ksOrderCollectionTask',        'run', '{"minutes":10}', '0 3/10 * * * ?', 0, '快手订单采集',        '实时采集：好单库快手订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1405, 'ksOrderHistoryCollectionTask', 'run', '{"day":30}',     '0 30 5 * * ?',   0, '快手订单历史回溯',    '历史回溯：每天 05:30 回溯最近 30 天快手订单，代码内按 1 天分段', NOW(), NOW(), 0),
(1406, 'idleOrderCollectionTask',      'run', '{"minutes":10}', '0 5/10 * * * ?', 0, '闲鱼订单采集',        '实时采集：好单库闲鱼订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1407, 'tbLiveOrderCollectionTask',    'run', '{"minutes":10}', '0 7/10 * * * ?', 0, '淘宝更高佣订单采集',  '实时采集：好单库淘宝更高佣订单，每 10 分钟拉取最近 10 分钟增量', NOW(), NOW(), 0),
(1408, 'foulOrderCollectionTask',      'run', '{"day":30}',     '0 15 6 * * ?',   0, '违规订单采集',        '合规：每天 06:15 拉取最近 30 天违规订单并标记不可结算（官方查询跨度上限 30 天）', NOW(), NOW(), 0),
(1409, 'dtkGoodsNewestTask',           'run', '{"pageSize":100}', '0 20 1 * * ?', 0, '商品增量更新',        '商品库：每天 01:20 拉取新增/变更商品（scroll_id 分页）', NOW(), NOW(), 0),
(1410, 'dtkGoodsPullTask',             'run', '{"pageSize":100}', '0 40 2 * * ?', 0, '商品定时拉取',        '商品库：每天 02:40 按时间窗批量拉取商品', NOW(), NOW(), 0),
(1411, 'dtkGoodsStaleTask',            'run', '{"pageSize":100}', '0 0 1,13 * * ?', 0, '失效商品检测',      '商品库：每天 01:00 与 13:00 各一次（官方查询窗口仅 24 小时，必须每日至少跑一次）', NOW(), NOW(), 0);


-- ============================================================================
-- 步骤 3：验证（执行完请跑这几条，结果应全部符合注释里的期望）
-- ============================================================================
-- ① 总数应为 38，且 is_pause 全为 0
SELECT COUNT(*) AS total, SUM(`is_pause` = 0) AS enabled
FROM `system_quartz_job`
WHERE `bean_name` IN (
  'tbOrderCollectionTask','jdOrderCollectionTask','pddOrderCollectionTask','vipOrderCollectionTask',
  'dyOrderCollectionTask','dyKuOrderCollectionTask','eleKuOrderCollectionTask','orderMtCPSCollectionTask',
  'csjEcomOrderCollectionTask','tbOrderHistoryCollectionTask','jdOrderHistoryCollectionTask',
  'pddOrderHistoryCollectionTask','vipOrderHistoryCollectionTask','dyOrderHistoryCollectionTask',
  'dyKuOrderHistoryCollectionTask','eleKuOrderHistoryCollectionTask','orderMtCPSHistoryCollectionTask',
  'ksOrderCollectionTask','ksOrderHistoryCollectionTask','idleOrderCollectionTask',
  'tbLiveOrderCollectionTask','foulOrderCollectionTask',
  'dtkGoodsNewestTask','dtkGoodsPullTask','dtkGoodsStaleTask',
  'fee7DayTask','feeMonthTask','feeProMonthTask','feeTodayTask','feeTotalTask','feeTriggerTask',
  'orderRefundTask','orderCheckTask','selfOrderHbTask','billUnlockTask',
  'memberExpiredTask','payResetTask','visitsTask'
);
-- 期望：total=38  enabled=38

-- ② 不应有重复 bean_name（幂等的验收条件）
SELECT `bean_name`, COUNT(*) AS c
FROM `system_quartz_job`
GROUP BY `bean_name`
HAVING c > 1;
-- 期望：Empty set

-- ③ 逐个核对参数与 cron
SELECT `id`, `bean_name`, `method_name`, `params`, `cron_expression`, `is_pause`, `job_name`
FROM `system_quartz_job`
WHERE `id` BETWEEN 1001 AND 1411
ORDER BY `id`;


-- ============================================================================
-- 附：关于 id 的分配
-- ----------------------------------------------------------------------------
-- 本脚本占用 1001~1403 区间，故意远离 1。
-- 原因：QuartzJobServiceImpl.updateIsPause() 开头有一句
--       if (quartzJob.getId().equals(1L)) throw new BadRequestException("该任务不可操作");
-- 而 ExecutionJob 在任务抛异常时会调用 updateIsPause 自动把任务**暂停**。
-- 若某个任务占了 id=1，一旦第一次执行失败，就会抛 BadRequestException，
-- 任务状态停留在"未暂停但一直失败"，排障时非常迷惑。
-- 用 1001 起可以完全避开这个坑。
--
-- 附：关于 quartz.full 白名单里两个不存在的 bean
-- ----------------------------------------------------------------------------
-- application-*.yml 的 quartz.full 白名单里列了 tbOrderBindTask 和 pidPoolTask，
-- 但任务包中**没有**这两个类（已用 git grep 确认）。这两个条目是历史遗留，
-- 不影响主节点执行（主节点不看白名单），但多机部署时会让副节点尝试
-- getBean 失败。本脚本不注册它们；如需彻底清理，请另行修改 yml（本工单不改 yml）。
-- ============================================================================
