> 来源: https://syh.hzhero.cn/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/01.%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3/50.start-web.html
> 抓取时间: 2026-10-29 22:00:06
> 版权归济南隽宸信息科技有限公司所有，仅供本地部署参考，请勿外传

# 快速启动（前端项目）

当前管理端仓库为 **`sfb_admin`**，已升级 Vue 3、Element Plus、Vite 5、Pinia。建议使用 Node.js 20+。

## 1. 获取代码与依赖

```bash
git clone https://gitee.com/jacky4631/sfb_admin.git
cd sfb_admin
npm ci
```

## 2. 配置开发接口

先启动后端 `mshop-admin`。开发模式读取 `.env.dev`，本机覆写配置使用 `.env.dev.local`：

```dotenv
VITE_PORT=8013
VITE_BASE_API=http://127.0.0.1:8001/admin
```

Vite 会把 `/api` 与 `/auth` 代理到管理服务。保留 `/admin` 上下文，不要连接到 APP 服务的 `8008/api`。

## 3. 启动管理端

```bash
npm run dev
```

访问 **<http://localhost:8013>**。后台账号、菜单与角色权限取决于数据库初始化结果。

当前 `dev:local` 脚本使用了 Vite 保留的 `local` 模式名，请使用上面的 `dev` 命令和 `.env.dev.local` 配置，不使用 `--mode local`。

## 4. 检查与构建

| 命令 | 用途 |
| --- | --- |
| `npm run ts:check` | 类型检查 |
| `npm run build:test` | 使用 `.env.test` 构建测试包 |
| `npm run build:prod` | 使用 `.env.prod` 构建生产包 |
| `npm run preview` | 本地预览构建结果 |

生产部署见[前端部署](https://syh.hzhero.cn/02.开源版本/02.部署文档/20.web-build.html)。旧 Vue 2 文档中的 `VUE_APP_*` 配置不适用于当前 Vite 项目。
