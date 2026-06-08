---
title: "xuan-product"
---

xuan-product 是 Xuan ERP 的商品服务，主要负责：商品、分类、单位、品牌、价格、车型适配、商品条码。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-product` |
| 职责 | 商品、分类、单位、品牌、价格、车型适配、商品条码 |
| 权限前缀 | `product` |
| 数据库/Schema | `xuan_product` |
| 事件 Topic | `xuan-product-event` |
| Java 包名 | `com.xuan.erp.product` |

## 限界上下文

商品主数据上下文。商品服务拥有商品档案和与商品强相关的分类、单位、价格规则，不拥有库存余额和交易单据。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Product, ProductCategory, ProductPriceRule | 本服务内部一致性边界 |
| 值对象 | ProductCode, SkuCode, Money, VehicleFitment | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | SkuUniquenessPolicy, ProductPricingPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_product` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
