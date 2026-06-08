---
title: "xuan-manufacturing"
---

xuan-manufacturing 是 Xuan ERP 的组装拆分服务，主要负责：组装单、拆分单、BOM 模板、成品与配件转换。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-manufacturing` |
| 职责 | 组装单、拆分单、BOM 模板、成品与配件转换 |
| 权限前缀 | `manufacturing` |
| 数据库/Schema | `xuan_manufacturing` |
| 事件 Topic | `xuan-manufacturing-event` |
| Java 包名 | `com.xuan.erp.manufacturing` |

## 限界上下文

轻制造上下文。它负责组装拆分业务规则，不拥有商品主数据和库存余额。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | AssemblyOrder, DisassemblyOrder, AssemblyTemplate | 本服务内部一致性边界 |
| 值对象 | BomLine, ComponentQuantity | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | AssemblyFeasibilityPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_manufacturing` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
