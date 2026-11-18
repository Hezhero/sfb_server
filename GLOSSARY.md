# 术语表

> 只记录业务/领域术语，不写实现细节。实现约定见 `docs/agents/working-conventions.md`。

## 品牌与标识

**三盈会**
本项目的统一品牌名。全拼 `sanyinghui`，简写 `syh`。
此前品牌名为「苏分宝」，属历史遗留，已统一替换为「三盈会」。
注意：法律权利主体由原许可方变更为济南隽宸信息科技有限公司（详见 ADR-0001）。

**syh**
「三盈会」的简写。用于需要短标识的场景：数据库名、配置键、应用 ID 等。
正式名称、目录名、包名段使用全拼 `sanyinghui`。

**junchen**
Java 包名根（`com.junchen`）。注意：这是代码组织命名空间，与品牌全拼 `sanyinghui` 不同——
原包名 `com.mailvor` → `com.junchen`，`com.mailvor.sufenbao` → `com.junchen.sanyinghui`。
仓库名亦采用此前缀：`syh_server`、`syh_app`、`syh_admin`。

## 业务术语

（待后续领域讨论补充）
