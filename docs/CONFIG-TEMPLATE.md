# 配置模板（安全基线）

本目录的 `application-dev.yml` / `application-prod.yml` 含环境相关值，不纳入版本控制。
部署时请按下列键从环境变量注入敏感值，不要写死在 yml 里。

## 支付与监控（安全加固，2026-11-18）

| 环境变量 | 用途 | 是否必填 |
|---|---|---|
| `IOS_SHARED_SECRET` | Apple 内购收据校验共享密钥（对应 `ios.shared-secret`） | 使用 iOS 内购时必填 |
| `DRUID_STAT_ENABLED` | 是否开启 Druid 监控台，默认 `false` | 否 |
| `DRUID_ALLOW` | Druid 访问白名单，默认 `127.0.0.1` | 否 |
| `DRUID_USERNAME` | Druid 监控台用户名 | 开启监控时必填 |
| `DRUID_PASSWORD` | Druid 监控台密码（务必强密码，不要 123456） | 开启监控时必填 |

## 说明

- Druid 监控台已不再通过 `/druid/**` 匿名放行，访问需先登录管理后台；
  如无需监控，保持 `DRUID_STAT_ENABLED=false` 即可。
- Apple 共享密钥、Druid 口令、数据库口令等一律从环境变量读取；
  源码与配置文件中不得出现明文凭据。
