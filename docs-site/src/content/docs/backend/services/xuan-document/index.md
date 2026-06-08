---
title: "xuan-document"
---

xuan-document 是 Xuan ERP 的文档打印服务，主要负责：打印模板、打印任务、打印快照、导出模板、附件元数据。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-document` |
| 职责 | 打印模板、打印任务、打印快照、导出模板、附件元数据 |
| 权限前缀 | `document` |
| 数据库/Schema | `xuan_document` |
| 事件 Topic | `xuan-document-event` |
| Java 包名 | `com.xuan.erp.document` |

## 限界上下文

文档输出上下文。它负责模板和输出快照，不拥有业务单据本体。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | PrintTemplate, PrintJob, DocumentSnapshot | 本服务内部一致性边界 |
| 值对象 | TemplateCode, DocumentNo | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | PrintRenderPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_document` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
