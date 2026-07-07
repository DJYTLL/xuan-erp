---
title: "本地开发"
---

本文记录本地开发环境准备、服务启动顺序和常用命令。

## 待补充

- JDK。
- Maven。
- Node.js。
- PostgreSQL。
- Nacos。
- RocketMQ。
- Redis。
- Elasticsearch。
- SkyWalking。

## 文档站全局搜索

Starlight 的全局搜索使用 Pagefind。Pagefind 索引在静态构建阶段生成，因此：

- `npm run docs:dev` 适合写文档和热更新，但不适合验证全局搜索。
- `npm run docs:build` 会生成 Pagefind 搜索索引。
- `npm run docs:preview` 用来预览构建产物，此时才能完整验证全局搜索。

验证全局搜索时使用：

```powershell
npm run docs:build
npm run docs:preview
```

然后访问：

```text
http://127.0.0.1:3000
```

如果只运行 `npm run docs:dev`，搜索弹窗可能提示开发模式不可用或搜索结果不完整，这是 Starlight / Pagefind 的正常行为。

项目提供的启动脚本已经按这个规则处理：

```powershell
.\start-docs.ps1
```

该脚本会先执行 `npm run docs:build` 生成搜索索引，再执行 `npm run docs:preview` 启动文档站。



