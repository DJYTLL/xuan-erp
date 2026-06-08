---
title: "数据库初始化"
---

本文记录各微服务数据库、schema、账号、Flyway migration 和初始化数据策略。

## 迁移规则

涉及数据库结构、字段、索引、约束、初始化数据或 Flyway migration 的修改，必须先扫描 migration 目录确认当前最高版本号，再按顺序追加新 migration。



