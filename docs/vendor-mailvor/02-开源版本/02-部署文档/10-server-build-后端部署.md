> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/02.%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3/10.server-build.html
> 抓取时间: 2026-10-29 22:00:07
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 后端部署

## 1. 构建服务

使用 JDK 17，在 `sfb_server` 根目录执行：

```bash
mvn -pl mshop-admin,mshop-app -am package -DskipTests
```

得到两个应用包：

```text
mshop-admin/target/mshop-admin-5.1.0.jar
mshop-app/target/mshop-app-5.1.0.jar
```

实际版本号以根 `pom.xml` 为准。构建引用根目录 `lib/` 中的第三方 SDK，需保留这些文件。

## 2. 准备外部配置

两个应用分别配置数据库、Redis 和第三方参数，保持 `/admin` 与 `/api` 上下文一致。可基于仓库配置准备自己的 `application-adminprod.yml` 与 `application-appprod.yml`，不要直接沿用仓库中的账号、密钥或默认部署环境。

将 JAR 与配置上传到自己的部署目录，在该目录分别启动：

```bash
java -jar mshop-admin-5.1.0.jar --server.port=8001 --spring.profiles.active=prod
```

```bash
java -jar mshop-app-5.1.0.jar --server.port=8008 --spring.profiles.active=prod
```

## 3. 配置反向代理

```nginx
server {
    listen 80;
    server_name api.example.com;

    location /api/ {
        proxy_pass http://127.0.0.1:8008;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /admin/ {
        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

示例保留原上下文。配置 HTTPS、域名和证书后，管理端填写 `https://api.example.com/admin`，APP 的 `baseHost` 填写 `https://api.example.com`，由客户端追加 `/api`。

## 4. 联调检查

确认实际启用的 profile、数据库与 Redis 连接正确，再检查管理端登录、APP 分类与配置、第三方回调和定时订单采集。Redis 命令超时与 APP HTTP 超时是不同层级，排查见[常见问题](https://www.mailvor.com/02.开源版本/01.开发文档/70.other-prepare.html)。
