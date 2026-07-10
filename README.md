# Xuan ERP Microservice Architecture

本目录是 Xuan ERP 微服务化重构项目的根目录，后续用于承载微服务父工程、各业务服务、部署脚本、文档站和运维配置。

## 当前目录

```text
D:\xuan-erp
  pom.xml    Maven 根聚合工程，只聚合 backend
  docs-site/  Astro Starlight 项目文档站
  backend/    后端 Maven 多模块工程
```

## 文档站

文档站位于：

```text
D:\xuan-erp\docs-site
```

常用命令：

```powershell
cd D:\xuan-erp\docs-site
npm run docs:dev
npm run docs:build
npm run docs:preview
```

## 后端工程

后端工程位于：

```text
D:\xuan-erp\backend
```

常用命令：

```powershell
cd D:\xuan-erp\backend
mvn clean compile
```

如果在 IntelliJ IDEA 中打开 `D:\xuan-erp`，请刷新根目录 Maven 项目。根目录 `pom.xml` 只用于让 IDE 识别 `backend` 后端工程，不会把 `docs-site` 当成 Maven 模块。

## 前端工程

前端工程位于：

```text
D:\xuan-erp\frontend
```

常用命令：

```powershell
cd D:\xuan-erp\frontend
npm install
npm run dev
npm run build
```

本地开发默认使用 Vite 同源代理访问后端：前端请求 `/api/**`，代理目标通过 `VITE_DEV_PROXY_TARGET` 配置，参考 `frontend\.env.example`。当前登录接口按后端 IAM 契约调用 `POST /api/iam/auth/login`，请求体为 `tenantId + username + password`。如部署到独立前端域名，可设置 `VITE_API_BASE_URL` 为网关地址，并在网关侧放通 CORS。

