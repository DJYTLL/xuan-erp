---
title: "服务命名规范"
---

Xuan ERP 后端服务统一采用短服务名，不使用 `-service` 后缀。服务名同时作为工程目录名、Maven artifactId、Nacos 服务名、Docker 镜像名和 Kubernetes workload 名的默认来源。

## 命名规则

```text
xuan-领域名
```

示例：

| 类型 | 示例 |
| --- | --- |
| IAM 服务 | `xuan-iam` |
| 商品服务 | `xuan-product` |
| 销售服务 | `xuan-sales` |
| 库存服务 | `xuan-inventory` |

## 不推荐写法

| 写法 | 原因 |
| --- | --- |
| `xuan-erp-iam` | `erp` 已由产品名表达，服务名过长 |
| `xuan-iam-service` | `service` 后缀冗余，注册名和镜像名会变长 |
| `iam-service` | 缺少产品前缀，跨系统部署时不清晰 |

## 统一映射

| 对象 | 命名 |
| --- | --- |
| 工程目录 | `xuan-product` |
| Maven artifactId | `xuan-product` |
| Nacos service name | `xuan-product` |
| Docker image | `xuan-product:<version>` |
| Kubernetes Deployment | `xuan-product` |
| Nacos config dataId | `xuan-product.yaml` |
| RocketMQ Topic | `xuan-product-event` |

## 当前服务名

| 服务 | 说明 |
| --- | --- |
| `xuan-gateway` | API Gateway |
| `xuan-iam` | 身份认证、用户、角色、权限、菜单、列权限 |
| `xuan-tenant` | 租户、租户配置、租户生命周期 |
| `xuan-product` | 商品、分类、单位、价格、车型适配 |
| `xuan-party` | 客户、供应商、联系人、往来主体 |
| `xuan-warehouse` | 仓库、库位、仓储基础档案 |
| `xuan-sales` | 销售单、销售退货、审核、红冲 |
| `xuan-procurement` | 采购单、采购退货、审核、红冲 |
| `xuan-inventory` | 库存余额、库存流水、盘点、移库 |
| `xuan-manufacturing` | 组装、拆分、组装模板 |
| `xuan-finance` | 应收、应付、收款、付款、核销 |
| `xuan-document` | 打印模板、打印日志、打印快照 |
| `xuan-audit` | 审计日志、接口耗时、SQL 耗时 |
| `xuan-query` | 页面聚合查询、报表、首页统计 |

## 包名规则

Java 包名统一使用：

```text
com.xuan.erp.<domain>
```

示例：

```text
com.xuan.erp.product
com.xuan.erp.sales
com.xuan.erp.inventory
```

公共模块使用：

```text
com.xuan.erp.common
com.xuan.erp.common.security
com.xuan.erp.common.web
```
