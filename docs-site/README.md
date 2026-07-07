# Xuan ERP Docs Site

这是 Xuan ERP 微服务架构文档站，使用 Astro Starlight 构建。

## 基本信息

| 项 | 内容 |
| --- | --- |
| 项目名称 | Xuan ERP 微服务架构文档站 |
| 本地目录 | `D:\xuan-erp\docs-site` |
| 本地访问端口 | `3000` |
| 本地访问地址 | `http://localhost:3000` |
| 框架 | Astro + Starlight |
| 主要内容 | 微服务架构、技术栈、服务边界、权限、租户、部署运维、开发规范 |

## 常用命令

```powershell
npm run docs:dev
```

启动本地开发服务：

```text
http://127.0.0.1:3000
```

构建静态文档：

```powershell
npm run docs:build
```

预览构建结果：

```powershell
npm run docs:preview
```

## 全局搜索

Starlight 全局搜索依赖 Pagefind，搜索索引只在 `npm run docs:build` 时生成。

验证全局搜索请使用：

```powershell
npm run docs:build
npm run docs:preview
```

只运行 `npm run docs:dev` 时，搜索不代表最终效果。

## 辅助脚本

| 文件 | 说明 |
| --- | --- |
| `start-docs.ps1` | 构建文档站并启动 preview，支持全局搜索 |
| `start-docs.vbs` | Windows 下可见窗口构建并启动文档站，完成后停留提示 |
| `stop-docs.ps1` | 停止文档站 |
| `stop-docs.vbs` | Windows 下静默停止文档站 |

## 文档结构

| 目录 | 说明 |
| --- | --- |
| `src/content/docs/overview` | 项目概览 |
| `src/content/docs/guide` | 快速开始和基础组件准备 |
| `src/content/docs/architecture` | 架构设计 |
| `src/content/docs/backend` | 后端开发规范和服务文档 |
| `src/content/docs/frontend` | 前端接入规范 |
| `src/content/docs/security` | 权限、租户和服务间鉴权 |
| `src/content/docs/ops` | 部署运维 |
| `src/content/docs/components` | 通用功能组件 |

## 当前基础设施栈

```text
Nacos + Sentinel + Seata + RocketMQ + Redis + Elasticsearch + SkyWalking + PostgreSQL pg_stat_statements
```

`duaoyunxuan.com` 服务器中间件端口见：

```text
src/content/docs/ops/middleware-ports.md
```

注意：`duaoyunxuan.com`、NAS 服务器和其它远程服务器端口记录互不覆盖，查端口前先确认目标服务器。

## 文档分工

- Starlight 文档站记录稳定结论、正式规范和可复用说明。
- Obsidian 记录项目推进、每日计划、ADR 草稿和临时想法。
- 重要 ADR 在 Obsidian 成稿后，应同步到 Starlight 对应架构或规范页面。

## 维护约定

- 新增正式规范时，优先放入 `src/content/docs`。
- 新增页面后，如果需要出现在左侧导航，要同步修改 `astro.config.mjs`。
- 修改文档后建议运行 `npm run docs:build` 验证。
- 构建目录 `dist` 为生成结果，不作为源文档维护入口。
