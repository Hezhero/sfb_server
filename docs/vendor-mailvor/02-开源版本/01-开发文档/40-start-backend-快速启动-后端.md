> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/40.start-backend.html
> 抓取时间: 2026-10-29 22:00:05
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 快速启动（后端项目）

## 1. 准备 Java 8 与源码

当前开源服务端使用 Java 8、Spring Boot 2.6.7。使用 IDEA 或其他支持 Maven / Lombok 的 IDE，先核对：

```bash
java -version
mvn -v
git clone https://gitee.com/jacky4631/sfb_server.git
cd sfb_server
```

两条版本命令都应显示 Java 8。拉取完整仓库，保留 `lib/` 中 Maven 引用的第三方 SDK。

## 2. 初始化 MySQL 与 Redis

先完成[数据库准备](https://www.mailvor.com/02.开源版本/01.开发文档/20.db-prepare.html)，分别配置：

```text
mshop-admin/src/main/resources/config/application-dev.yml
mshop-app/src/main/resources/config/application-dev.yml
```

配置自己的数据库、Redis、联盟平台、短信、支付、推送等参数。只启用所需第三方业务，并确保所需账号和权限已经准备好。

## 3. 显式选择开发 profile

主配置目前指定 `sfbadminprod` / `sfbappprod`，本地运行请为两个应用都设置程序参数：

```text
--spring.profiles.active=dev
```

IDEA 分别运行：

| 启动类 | 默认地址 | 用途 |
| --- | --- | --- |
| `com.mailvor.AdminServer` | `http://localhost:8001/admin` | 管理端 API |
| `com.mailvor.AppServer` | `http://localhost:8008/api` | APP API |

## 4. 命令行构建与运行

在根目录构建两个应用及其依赖：

```bash
mvn -pl mshop-admin,mshop-app -am package -DskipTests
```

两个终端分别执行：

```bash
java -jar mshop-admin/target/mshop-admin-5.1.0.jar --spring.profiles.active=dev
```

```bash
java -jar mshop-app/target/mshop-app-5.1.0.jar --spring.profiles.active=dev
```

JAR 版本号以当前根 `pom.xml` 为准。缺少美团 / 易宝 SDK 时先查看仓库 `lib/README.md`，相关源码压缩包也放在 `lib/` 下。

下一步：[管理端启动](https://www.mailvor.com/02.开源版本/01.开发文档/50.start-web.html) · [APP 启动](https://www.mailvor.com/02.开源版本/01.开发文档/60.start-app.html)。
