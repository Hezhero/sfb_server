> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/00.introduce.html
> 抓取时间: 2026-10-29 22:00:03
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 文档介绍

本文档对应当前 `sfb_admin`、`sfb_server`、`sfb` 三个开源项目，按“准备环境 → 本地联调 → 生产部署 → 商城配置”的顺序组织。

## 源码与适用范围

| 项目 | 用途 | 仓库 |
| --- | --- | --- |
| `sfb_admin` | Vue 3 运营管理后台 | [Gitee](https://gitee.com/jacky4631/sfb_admin) · [GitHub](https://github.com/jacky4631/sfb_admin) |
| `sfb_server` | 管理 API、APP API、订单采集与核心业务 | [Gitee](https://gitee.com/jacky4631/sfb_server) · [GitHub](https://github.com/jacky4631/sfb_server) |
| `sfb` | Android / iOS Flutter APP | [Gitee](https://gitee.com/jacky4631/sfb) · [GitHub](https://github.com/jacky4631/sfb) |

开源版已含淘宝、京东、拼多多、唯品会、抖音相关商品与订单模块，以及美团外卖、饿了么等活动入口。第三方接入需要自己的账号、密钥、证书与权限；源码中保留的管理页面不代表商业版同名功能已经在三端完整交付。

需要 SaaS 租户、团长、自定义装修、价保、在线打包、小程序或鸿蒙等能力时，查看[功能对比](https://www.mailvor.com/01.介绍/30.functional.html)与[版本授权](https://www.mailvor.com/01.介绍/40.price.html)。使用范围遵循各仓库 LICENSE 和对应授权说明。

## 当前环境

| 端 | 技术栈 / 环境 |
| --- | --- |
| 服务端 | Java 8、Spring Boot 2.7.18、MyBatis Plus；Maven 需使用 Java 8 |
| 管理端 | Vue 3、Element Plus、Vite 5、Pinia；建议 Node.js 20+ |
| APP | Flutter 3.47.3 / Dart 3.13.3；约束 Flutter ≥ 3.44、Dart ≥ 3.12 |
| Android 构建 | JDK 17、SDK 37、NDK 28.2.13676358、AGP 8.11.1、Gradle 8.14 |
| iOS 构建 | macOS、Xcode、CocoaPods；最低 iOS 15 |
| 数据存储 | MySQL 5.7、Redis |

APP 最低支持 Android 7.0（API 24）。服务端 Java 8 与 Android 构建的 JDK 17 是两套环境；不要因为切换端而沿用错误的 `JAVA_HOME`。

## 开发顺序

| 步骤 | 文档 | 关键内容 |
| --- | --- | --- |
| 1 | [SQL 获取](https://www.mailvor.com/02.开源版本/01.开发文档/10.db-get.html) · [数据库准备](https://www.mailvor.com/02.开源版本/01.开发文档/20.db-prepare.html) | 获取脚本、导入 MySQL、分别配置两个后端应用 |
| 2 | [服务端启动](https://www.mailvor.com/02.开源版本/01.开发文档/40.start-backend.html) | 显式使用 dev profile，启动 AdminServer 与 AppServer |
| 3 | [管理端启动](https://www.mailvor.com/02.开源版本/01.开发文档/50.start-web.html) | `npm ci`、`.env.dev.local`、`npm run dev` |
| 4 | [APP 准备](https://www.mailvor.com/02.开源版本/01.开发文档/30.app-prepare.html) · [APP 启动](https://www.mailvor.com/02.开源版本/01.开发文档/60.start-app.html) | Flutter、包名与签名、接口地址、第三方参数 |
| 5 | [常见问题](https://www.mailvor.com/02.开源版本/01.开发文档/70.other-prepare.html) | 依赖、profile、Redis 超时与 iOS 插件兼容性 |

两个后端应用分别维护配置：

| 应用 | 启动类 | 默认地址 | 使用方 |
| --- | --- | --- | --- |
| `mshop-admin` | `com.mailvor.AdminServer` | `http://localhost:8001/admin` | 管理后台 |
| `mshop-app` | `com.mailvor.AppServer` | `http://localhost:8008/api` | Flutter APP |

## 部署与商城配置

| 分类 | 文档 |
| --- | --- |
| 服务端部署 | [构建 JAR、外部 profile 与反向代理](https://www.mailvor.com/02.开源版本/02.部署文档/10.server-build.html) |
| 管理端部署 | [Vite 生产配置、构建与 Nginx](https://www.mailvor.com/02.开源版本/02.部署文档/20.web-build.html) |
| APP 发布 | [Android / iOS 本地打包](https://www.mailvor.com/02.开源版本/02.部署文档/30.app-build.html) |
| 商城配置 | [安卓设置](https://www.mailvor.com/02.开源版本/03.商城配置/10.android-setting.html) · [分享设置](https://www.mailvor.com/02.开源版本/03.商城配置/20.share-setting.html) · [订单结算](https://www.mailvor.com/02.开源版本/03.商城配置/30.settle.html) · [分销说明](https://www.mailvor.com/02.开源版本/03.商城配置/40.distribution.html) |

演示站包含商业版功能，不能直接作为开源仓库的功能验收清单。当前源码和三个仓库 README 是开源能力核对依据；商业版能力与交付权益以对应文档为准。
