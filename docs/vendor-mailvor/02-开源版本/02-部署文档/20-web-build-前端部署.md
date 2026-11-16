> 来源: https://www.mailvor.com/02.%E5%BC%80%E6%BA%90%E7%89%88%E6%9C%AC/02.%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3/20.web-build.html
> 抓取时间: 2026-10-29 22:00:08
> 版权归苏州迈沃通信有限公司所有，仅供本地部署参考，请勿外传

# 前端部署

当前管理端为 Vue 3 + Element Plus + Vite，仓库名 `sfb_admin`。

## 1. 配置生产环境

修改根目录 `.env.prod`：

```dotenv
VITE_BASE_API=https://api.example.com/admin
VITE_OUT_DIR=dist
VITE_SOURCEMAP=false
VITE_DROP_DEBUGGER=true
VITE_DROP_CONSOLE=true
```

示例域名应替换为自己的地址。也可以配置 `VITE_BASE_API=/admin`，通过同域 Nginx 代理管理服务，避免跨域配置。

当前 `build:prod` 读取 `.env.prod`，不使用旧版 Vue CLI 的 `VUE_APP_BASE_API` / `VUE_APP_WS_API`，也不使用 `.env.production` 作为此命令的模式配置。

## 2. 构建与预览

```bash
npm ci
npm run build:prod
npm run preview
```

默认产物目录为 `dist/`，可通过 `VITE_OUT_DIR` 调整。`preview` 用于本地检查，不作为生产服务。

## 3. 部署到 Nginx

将 `dist/` 内容上传到自己的静态站点目录。下面使用同域 `/admin` 代理，需配合 `VITE_BASE_API=/admin` 构建：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    root /var/www/sfb-admin;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /admin/ {
        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

`proxy_pass` 保留请求中的 `/admin/` 上下文。示例为 HTTP 站点结构，生产域名使用 HTTPS 时配置对应证书；若选择独立 API 域名，应同时确认后端跨域配置。

## 4. 部署检查

- 页面与静态资源可访问，浏览器刷新业务路由不会 404。
- 登录请求进入 `mshop-admin` 的 `/admin/auth/...`，业务请求进入 `/admin/api/...`。
- 管理端服务、数据库与 Redis 可用，菜单与权限已初始化。

服务端部署见[后端部署](https://www.mailvor.com/02.开源版本/02.部署文档/10.server-build.html)。
