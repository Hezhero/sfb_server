> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/20.db-prepare.html
> 抓取时间: 2026-10-29 22:00:04
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# 数据库准备

## 1. 准备数据库与脚本

当前开源版以 MySQL 5.7 为基线。先在[SQL 获取页](https://syh.hzhero.cn/02.开源版本/01.开发文档/10.db-get.html)取得初始化脚本，创建数据库并导入；如使用 MySQL 8，需要自行核对 SQL、字符集与 SQL mode 的兼容性。

数据库名与字符集应和所用 SQL 脚本及应用配置保持一致。账号、密码使用自己的环境参数，不依赖历史截图中的示例值。

## 2. 分别修改两个服务的配置

```text
server/mshop-admin/src/main/resources/config/application-dev.yml
server/mshop-app/src/main/resources/config/application-dev.yml
```

在两个文件中分别设置数据源 URL、用户名、密码和 Redis 地址、端口、数据库索引、认证信息。管理端与 APP 服务应连接同一套业务数据库，Redis 配置按实际部署保持一致。

当前 `application.yml` 默认 profile 为 `dev`。本地运行必须修改 `application-dev.yml`。

## 3. 检查初始化结果

确认 SQL 导入完成、应用能连接数据库和 Redis，再继续[启动服务端](https://syh.hzhero.cn/02.开源版本/01.开发文档/40.start-backend.html)。后台菜单与角色权限依赖 SQL 中的初始化数据，仅拉取前端源码不会自动生成菜单。
