# Xuan ERP Microservice Architecture

本目录是 Xuan ERP 微服务化重构项目的根目录，后续用于承载微服务父工程、各业务服务、部署脚本、文档站和运维配置。

## 当前目录

```text
D:\xuan-erp
  docs-site/  Astro Starlight 项目文档站
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

## 后续规划

后续微服务项目可以逐步加入：

```text
xuan-gateway/
xuan-iam/
xuan-tenant/
xuan-product/
xuan-party/
xuan-warehouse/
xuan-sales/
xuan-procurement/
xuan-inventory/
xuan-finance/
deploy/
scripts/
```

