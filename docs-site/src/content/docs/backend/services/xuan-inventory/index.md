---
title: "xuan-inventory"
---

xuan-inventory 是 Xuan ERP 的库存服务，主要负责：库存余额、库存流水、出入库、盘点、移库、库存预占与释放。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-inventory` |
| 职责 | 库存余额、库存流水、出入库、盘点、移库、库存预占与释放 |
| 权限前缀 | `inventory` |
| 数据库/Schema | `xuan_inventory` |
| 事件 Topic | `xuan-inventory-event` |
| Java 包名 | `com.xuan.erp.inventory` |

## 限界上下文

库存上下文。库存服务是库存数量和库存流水的唯一归属服务，销售、采购、组装拆分只能通过接口或事件影响库存。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | StockBalance, StockTransaction, StockReservation, StockCheck | 本服务内部一致性边界 |
| 值对象 | StockQuantity, WarehouseLocation, LotNo | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | StockDeductionPolicy, StockReservationPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_inventory` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
