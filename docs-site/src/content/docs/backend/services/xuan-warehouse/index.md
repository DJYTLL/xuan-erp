---
title: "xuan-warehouse"
---

xuan-warehouse 是 Xuan ERP 的仓储基础服务，主要负责：仓库、库位、仓储区域、库位启停、仓库权限基础数据。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-warehouse` |
| 职责 | 仓库、库位、仓储区域、库位启停、仓库权限基础数据 |
| 权限前缀 | `warehouse` |
| 数据库/Schema | `xuan_warehouse` |
| 事件 Topic | `xuan-warehouse-event` |
| Java 包名 | `com.xuan.erp.warehouse` |

## 限界上下文

仓储基础档案上下文。它拥有仓库和库位，不拥有库存数量、库存流水和盘点结果。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Warehouse, Location | 本服务内部一致性边界 |
| 值对象 | WarehouseCode, LocationCode | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | LocationAvailabilityPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_warehouse` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
