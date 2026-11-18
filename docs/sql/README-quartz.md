# 定时任务（Quartz）部署说明

本目录下的 `system_quartz_job.sql` 是全部定时任务的注册种子。**不执行它，系统部署后一条订单都不会采集**。

---

## 1. 任务是怎么跑起来的

本项目**没有** `@Scheduled` 注解，所有定时任务由数据库表 `system_quartz_job` 驱动。启动链条：

```
① JobRunner (implements ApplicationRunner)     ← 应用启动时触发一次
   └─ quartzJobService.findByIsPauseIsFalse()  ← 只查 is_pause=0 的记录
      └─ quartzManage.addJob(quartzJob)        ← 逐条注册到 Quartz

② QuartzManage.addJob
   └─ JobBuilder.newJob(ExecutionJob.class).withIdentity("TASK_" + id)
   └─ 用 cron_expression 建 CronTrigger
   └─ JobDataMap 塞入整行 QuartzJob
   └─ scheduler.scheduleJob(...)

③ ExecutionJob.executeInternal(到点时触发)
   └─ 从 JobDataMap 取 QuartzJob
   └─ if (!QUARTZ_MAIN && !QUARTZ_FULL_LIST.contains(beanName)) return;   ← 多机主从判定
   └─ EXECUTOR.submit(new QuartzRunnable(beanName, methodName, params)).get()
   └─ 写 QuartzLog（成功/失败、耗时、异常栈）

④ QuartzRunnable
   └─ SpringContextHolder.getBean(beanName)                     ← 按 Bean 名取实例
   └─ params 非空 → getDeclaredMethod(methodName, String.class)
      params 为空 → getDeclaredMethod(methodName)
   └─ method.invoke(target[, params])
```

**两个关键推论：**

1. **`bean_name` / `method_name` / `params` 三者任一写错，都不会在编译期报错**，而是到 cron 到点那一刻在运行期抛 `NoSuchBeanDefinitionException` 或 `NoSuchMethodException`，任务静默失败。
2. **`params` 是否为空，决定了反射找哪个重载方法**。所以：
   - 只有 `public void run()`（无参）的任务 → `params` 必须是 `NULL`
   - 只有 `public void run(String)` 的任务 → `params` 必须非空

   写反了就是 100% 失败。本仓库中只有 `VisitsTask` 是无参的。

---

## 2. 执行方式

```bash
# 1) 备份（强烈建议）
mysqldump -h<host> -u<user> -p <db> system_quartz_job > system_quartz_job.bak.$(date +%F).sql

# 2) 执行种子
mysql -h<host> -u<user> -p <db> < docs/sql/system_quartz_job.sql

# 3) 验证（SQL 文件末尾也带了这几条）
mysql -h<host> -u<user> -p <db> -e "
  SELECT COUNT(*) AS total, SUM(is_pause=0) AS enabled FROM system_quartz_job
  WHERE bean_name IN ('tbOrderCollectionTask','jdOrderCollectionTask',/* ... */'visitsTask');"
# 期望 total=30, enabled=30

mysql -h<host> -u<user> -p <db> -e "
  SELECT bean_name, COUNT(*) c FROM system_quartz_job GROUP BY bean_name HAVING c>1;"
# 期望 Empty set

# 4) 重启 mshop-admin，启动日志应出现：
#    --------------------注入定时任务---------------------
#    --------------------定时任务注入完成---------------------
```

> **必须重启**：`JobRunner` 只在应用启动时读表注册，改表不重启不生效。

---

## 3. 多机部署：只有主节点执行

`ExecutionJob` 第一行逻辑就是主从判定：

```java
if (!QUARTZ_MAIN && !QUARTZ_FULL_LIST.contains(quartzJob.getBeanName())) {
    return;
}
```

两个配置项来自 `application-*.yml`：

```yaml
quartz:
  #todo 当部署多个管理端时，只有一个设置为true
  main: true
  #表明这些task只在一台管理端执行
  full:
    - tbOrderCollectionTask
    - tbOrderHistoryCollectionTask
    # ...
```

对应 Java：`TaskConfig.QUARTZ_MAIN`（读 `quartz.main`）、`TaskFullRunConfig.QUARTZ_FULL_LIST`（读 `quartz.full`）。

### 部署规则

| 场景 | 配置要求 |
| --- | --- |
| 单机部署 | `quartz.main: true` 即可，白名单不用管 |
| 多机部署 | **只允许一台** `quartz.main: true`，其余全部 `false` |
| 多机 + 想并行分摊 | 副节点 `false`，但把要分摊的 bean 加进各自机器的 `quartz.full` |

### 重要提醒

1. **所有节点都会把全部 30 条记录注册进 Quartz**（`JobRunner` 不区分主从，它是查全表后统一 `addJob`）。区别在于：副节点触发后，`ExecutionJob` 第 42 行会直接 `return` 而不执行任务体。所以副节点的 Quartz 日志里会看到"触发"，但业务没跑——这是**正常现象**，不是故障。
2. **`quartz.main` 必须且只能有一个 true**。如果有两台都是 `true`，采集任务会被两个进程同时跑，订单表上会产生并发写与重复扣红包的风险。
3. **`quartz.full` 白名单里有两个实际不存在的 bean**：`tbOrderBindTask`、`pidPoolTask`。任务包中已无这两个类（`git grep` 确认）。对主节点无影响（主节点不看白名单），但多机时副节点会尝试 `getBean` 失败。清理需要改 yml，**不在本工单范围内**，此处仅作记录。
4. 同一台机器重启期间，Quartz 的内存调度状态会丢；重启后由 `JobRunner` 重新注册，`startTime` 重置为启动时刻，不会补跑停机期间错过的触发。

---

## 4. 30 个任务清单

任务包 `mshop-admin/src/main/java/com/junchen/modules/quartz/task/` 下共 **31 个 `.java`**，但其中 `OrderTask` 是**基类**——它只有 `saveTb` / `saveJd` / `savePdd` 等落库方法，**没有 `run` 方法**，不能注册为任务。所以可注册任务是 **30 个**。

> 备注：工单原文写"31 个任务"，实际按"含 `run` 方法的类"统计是 30 个。本脚本录入 30 条，不虚报。

### 4.1 订单采集（实时）— 9 个

| bean_name | method | cron | params | 用途 |
| --- | --- | --- | --- | --- |
| `tbOrderCollectionTask` | run | `0 0/10 * * * ?` | `{"minutes":10}` | 淘宝联盟订单采集 |
| `jdOrderCollectionTask` | run | `0 2/10 * * * ?` | `{"minutes":10}` | 京东联盟订单采集 |
| `pddOrderCollectionTask` | run | `0 4/10 * * * ?` | `{"minutes":10}` | 拼多多订单采集 |
| `vipOrderCollectionTask` | run | `0 6/10 * * * ?` | `{"minutes":10}` | 唯品会订单采集 |
| `dyOrderCollectionTask` | run | `0 8/10 * * * ?` | `{"minutes":10}` | 抖音（大淘客）订单采集 |
| `dyKuOrderCollectionTask` | run | `0 1/10 * * * ?` | `{"minutes":10}` | 抖音（好单库）订单采集 |
| `eleKuOrderCollectionTask` | run | `0 3/10 * * * ?` | `{"minutes":10}` | 饿了么（好单库）订单采集 |
| `orderMtCPSCollectionTask` | run | `0 5/10 * * * ?` | `{"minutes":10}` | 美团 CPS 订单采集 |
| `csjEcomOrderCollectionTask` | run | `0 7/10 * * * ?` | `{"minutes":10}` | 穿山甲电商联盟订单采集 |

**为什么错开分钟数？** 9 个采集任务如果都写 `0 0/10`，会每 10 分钟同一秒并发发起 9 个外部 API 调用，容易触发上游限流。把起始分钟错开（0/1/2/3/4/5/6/7/8）后，压力被摊平到 10 分钟内的 9 个点上。

**为什么窗口必须等于周期？** 代码里 `minutes` 兜底取"最近 N 分钟"。若 cron 每 10 分钟跑一次但 `minutes=30`，相邻两次窗口重叠 20 分钟 → 重复拉取；若 `minutes=5` 则漏单。所以两者必须严格相等，本脚本统一为 10。

### 4.2 订单采集（历史回溯）— 8 个

| bean_name | method | cron | params | 用途 |
| --- | --- | --- | --- | --- |
| `tbOrderHistoryCollectionTask` | run | `0 30 1 * * ?` | `{"day":90,"minutes":60}` | 淘宝历史回溯 |
| `jdOrderHistoryCollectionTask` | run | `0 0 2 * * ?` | `{"day":90,"minutes":60}` | 京东历史回溯 |
| `pddOrderHistoryCollectionTask` | run | `0 30 2 * * ?` | `{"day":90,"minutes":60}` | 拼多多历史回溯 |
| `vipOrderHistoryCollectionTask` | run | `0 0 3 * * ?` | `{"day":90,"minutes":60}` | 唯品会历史回溯 |
| `dyOrderHistoryCollectionTask` | run | `0 30 3 * * ?` | `{"day":90,"minutes":60}` | 抖音历史回溯 |
| `dyKuOrderHistoryCollectionTask` | run | `0 0 4 * * ?` | `{"day":90,"minutes":60}` | 抖音（好单库）历史回溯 |
| `eleKuOrderHistoryCollectionTask` | run | `0 30 4 * * ?` | `{"day":90,"minutes":60}` | 饿了么（好单库）历史回溯 |
| `orderMtCPSHistoryCollectionTask` | run | `0 0 5 * * ?` | `{"day":90,"minutes":60}` | 美团 CPS 历史回溯 |

**实时 vs 历史的策略差异：**

| 维度 | 实时采集 | 历史回溯 |
| --- | --- | --- |
| 触发频率 | 每 10 分钟 | 每天一次（凌晨 1:30–5:00 错峰） |
| 时间窗 | 最近 10 分钟 | `day=90` × `minutes=60` → 循环 2160 个时间窗 |
| 目的 | 日常增量回流 | 补齐漏单、平台延迟结算单 |
| 耗时 | 秒级 | 分钟级到小时级 |
| 单次执行量 | 少 | 大（每窗之间有 `Thread.sleep(100ms)` 限速，美团是 1000ms） |

历史回溯任务被安排在凌晨连续错开（1:30/2:00/2:30/3:00/3:30/4:00/4:30/5:00），避免同一时刻集中打爆上游。若上游限流严重，把 `day` 调小（如 `7`）或 `minutes` 调大（如 `180`）即可减少轮次。

### 4.3 计费 / 佣金预估 — 6 个

| bean_name | method | cron | params | 用途 |
| --- | --- | --- | --- | --- |
| `feeTodayTask` | run | `0 0/1 * * * ?` | `100` | 每分钟刷新今日预估（type=1） |
| `fee7DayTask` | run | `0 0/20 * * * ?` | `100` | 每 20 分钟刷新 7 日预估（type=6） |
| `feeMonthTask` | run | `0 5/20 * * * ?` | `100` | 每 20 分钟刷新本月预估（type=3） |
| `feeProMonthTask` | run | `0 5 0 1 * ?` | `100` | 每月 1 日 00:05 统计上月预估（type=4） |
| `feeTotalTask` | run | `0 30 6 * * ?` | `100` | 每天 06:30 全量重算 |
| `feeTriggerTask` | run | `0 0/1 * * * ?` | `0` | 每分钟消费 Redis 今日 uid 队列 |

**注意：这 5 个 `Fee*Task` 的参数是纯数字字符串，不是 JSON。** 代码里是 `Integer.parseInt(sizeStr)`，表示"每批处理多少用户"。给 `"100"` 即可。

**参数为什么不能为空？** `parseInt(null)` 会抛 `NumberFormatException`，且因为 `params` 为空时 `QuartzRunnable` 会改去找无参 `run()`（不存在）→ 直接 `NoSuchMethodException`。双重失败。

**`feeTriggerTask` 为什么用 `"0"`？** 它的入参是逗号分隔的 uid 串，正常业务上给空是有意义的（只处理 Redis 里的今日 uid）。但传空串会让反射走无参分支而失败，因此给 `"0"` 占位——代码会把它当 uid 加入列表，而 `initFeeLogs` 里对查不到的 user 会 `continue`（`if (user == null) continue;`），等价于空跑，无副作用。

### 4.4 订单后处理 — 4 个

| bean_name | method | cron | params | 用途 |
| --- | --- | --- | --- | --- |
| `orderRefundTask` | run | `0 0/5 * * * ?` | `{"prior":30}` | 退款冲正：扫描最近 30 天失效订单，扣回红包积分 |
| `orderCheckTask` | run | `0 10/30 * * * ?` | `{}` | 风控：高退款风险用户调整拆红包次数 |
| `selfOrderHbTask` | run | `0 2/10 * * * ?` | `{"limit":20}` | 自购拆红包：处理过期未拆的自购订单 |
| `billUnlockTask` | run | `0 4/10 * * * ?` | `{"limit":20}` | 账单解锁：解冻到期账单并回补余额 |

这 4 个都是"扫表批处理"型，频率不宜过快（会与订单表争锁）。`orderRefundTask` 的 `prior` 单位是天，默认 30 与代码里 `OrderRefundParam.prior = 30` 一致。

### 4.5 用户 / 运营 — 3 个

| bean_name | method | cron | params | 用途 |
| --- | --- | --- | --- | --- |
| `memberExpiredTask` | run | `0 0 0 * * ?` | `{}` | 每天 00:00 处理会员过期 |
| `payResetTask` | run | `0 0 0 1 * ?` | `{}` | 每月 1 日 00:00 重置支付通道额度 |
| `visitsTask` | run | `0 0 0 * * ?` | `NULL` | 每天 00:00 写访问统计基线（**无参任务**） |

---

## 5. cron 表达式含义（Quartz 格式）

**Quartz 的 cron 与 Linux crontab 不同：多一个"秒"字段，放在最前面。**

```
 ┌───────────── 秒        (0-59)
 │ ┌─────────── 分        (0-59)
 │ │ ┌───────── 时        (0-23)
 │ │ │ ┌─────── 日        (1-31)
 │ │ │ │ ┌───── 月        (1-12 或 JAN-DEC)
 │ │ │ │ │ ┌─── 周        (1-7 或 SUN-SAT；1=SUN)
 │ │ │ │ │ │ ┌─ 年（可选）
 * * * * * ? *
```

本脚本用到的写法：

| 写法 | 含义 |
| --- | --- |
| `0 0/10 * * * ?` | 每 10 分钟一次（第 0、10、20… 分钟，秒=0） |
| `0 2/10 * * * ?` | 每 10 分钟一次，但从第 2 分钟起（2、12、22…） |
| `0 0/1 * * * ?` | 每分钟一次 |
| `0 30 1 * * ?` | 每天 01:30:00 |
| `0 5 0 1 * ?` | 每月 1 日 00:05:00 |
| `?` | 不指定（日/周互斥时占位，Quartz 要求二者之一是 `?`） |

> **`?` 与 `*` 的区别**：日和周不能同时用 `*`，必须有一个是 `?`（表示"不关心"）。`0 0 0 * * ?` = 每天 0 点（日字段 `*`，周字段 `?`）。

---

## 6. 验证与排障

### 6.1 离线静态校验（不连数据库）

种子 SQL 的任何字段错误都只会在运行期暴露，所以本目录附带了一个校验器：

```bash
python docs/sql/verify_quartz_job_seeds.py
```

它会读 Java 源码 + 解析种子 SQL，做 292 项断言：

1. `bean_name` 集合与源码中"含 `run` 方法的类"双向一致
2. 五类任务数量（9+8+6+4+3=30）
3. `bean_name` == 类名首字母小写
4. `method_name` 必须在目标类**自身**声明（`getDeclaredMethod` 不查父类）
5. `params` 空/非空必须与 `run()` / `run(String)` 重载匹配
6. cron 合法性 + 类别语义（实时=每 10 分钟且 `minutes=10`；历史=每天且带 `day`+`minutes`）
7. `is_pause` 全为 0
8. 幂等形态（定向 DELETE）与 `id` 唯一
9. INSERT 列数 == 每行值数
10. DELETE 清单与 INSERT 清单严格一致（防止重复执行留孤儿/误删运维数据）
11. `is_del` 全为 0（否则 `@TableLogic` 会把行过滤掉，等于没注册）
12. `id` 避让 1（见下文）

**修改种子后请重跑**。校验器本身做过变异测试（改错 method、改空 params、改错 cron、删掉一条）均能正确报错。

### 6.2 线上排障

| 现象 | 可能原因 |
| --- | --- |
| 启动日志无"注入定时任务" | `is_pause=1`，或 DB 连接/表名不对 |
| 日志 `任务执行失败` + `NoSuchMethodException` | `method_name` 错，或 `params` 空/非空与 `run` 重载不匹配 |
| 日志 `任务执行失败` + `NoSuchBeanDefinitionException` | `bean_name` 拼错（不是类名首字母小写） |
| 副节点日志显示触发但业务没动 | 正常：`QUARTZ_MAIN=false` 且不在 `full` 白名单 |
| 任务执行一次就被暂停 | `ExecutionJob` 异常分支会调 `updateIsPause` 自动置 `is_pause=1` |

查执行历史：

```sql
SELECT job_name, baen_name, is_success, time, cron_expression,
       LEFT(exception_detail, 500) AS err
FROM system_quartz_log
ORDER BY id DESC LIMIT 50;
```

> 日志表名是 `system_quartz_log`（见 `QuartzLog.java` 的 `@TableName`）。其中 `baen_name` 列是仓库里的**原始拼写错误**（实体字段就叫 `baenName`），查询时照抄即可。

---

## 7. 幂等与 id 分配的说明

### 幂等实现

`DELETE`（按 `bean_name` 定向）+ `INSERT`（固定显式 id）。

**为什么不用 `INSERT ... ON DUPLICATE KEY UPDATE`？** 因为 `system_quartz_job` 的 `bean_name` 上**没有唯一索引**（建表脚本不在仓库内，无法保证生产库有）。没有唯一索引时 `ON DUPLICATE KEY UPDATE` 会静默退化成普通 `INSERT`，重复执行直接产生重复行——这恰恰违反幂等要求。`DELETE + INSERT` 不依赖任何索引。

**为什么不用 `WHERE NOT EXISTS`？** 那样无法修正"已存在但字段是错的"记录（比如 cron 写错了），只能新增、不能更新。`DELETE + INSERT` 能保证每次执行后表状态**收敛到脚本内容**。

`DELETE` 用 `WHERE bean_name IN (...)` 精确限定 30 个名字，**不会误删运维手工创建的其它任务**。

### 为什么 id 从 1001 起

`QuartzJobServiceImpl.updateIsPause()` 开头有一句：

```java
if (quartzJob.getId().equals(1L)) {
    throw new BadRequestException("该任务不可操作");
}
```

而 `ExecutionJob` 在任务抛异常时会调 `updateIsPause` 把任务**自动暂停**。若某个任务占了 `id=1`，第一次执行失败就会抛 `BadRequestException`，任务卡在"未暂停但一直失败"的状态，排障时非常迷惑。用 1001 起完全避开。

---

## 8. 未纳入本脚本的任务

| bean | 情况 |
| --- | --- |
| `OrderTask` | **基类**，无 `run` 方法，不能注册（不是任务） |
| `tbOrderBindTask` | 仅出现在 `application-*.yml` 的 `quartz.full` 白名单，**任务包中无此类** |
| `pidPoolTask` | 同上 |

后两个是历史遗留条目。本工单**不修改 yml**（写集限制），如需清理请单独提工单。
