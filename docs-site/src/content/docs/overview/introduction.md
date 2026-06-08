---
title: "项目介绍"
---

本文档用于记录 Xuan ERP 微服务化重构项目的背景、目标和范围。

## 项目背景

当前系统是汽配仓储、进销存、ERP 一体化系统，覆盖系统管理、租户、权限、商品、客户、供应商、仓库、销售、采购、库存、组装拆分、财务、打印、审计和报表查询。

## 文档目标

本项目文档站需要像 RuoYi 文档一样，长期记录项目所有关键资料，包括架构设计、服务说明、开发规范、组件使用、部署运维和常见问题。

## 技术基线

当前 Xuan ERP 微服务项目直接采用 Java 21 LTS 作为生产运行基线，不再以 Java 17 作为默认版本。

| 组件 | 项目基线 |
| --- | --- |
| JDK | Java 21 LTS |
| Spring Boot | 4.0.6 或同分支最新 4.0.x |
| Spring Cloud | 2025.1.1 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Nacos Server | 3.2.2 |
| Nacos Client | 跟随 Spring Cloud Alibaba BOM |
| LoadBalancer | `spring-cloud-starter-loadbalancer`，服务间调用负载均衡 |
| Sentinel | 跟随 Spring Cloud Alibaba BOM，当前为 1.8.9 |
| Seata | 跟随 Spring Cloud Alibaba BOM，当前为 2.5.0 |
| RocketMQ Client | 跟随 Spring Cloud Alibaba BOM，当前为 5.3.1 |
| RocketMQ Server | 5.5.0 |

所有后端微服务的 Maven 父工程应统一声明 `java.version=21`，业务服务不单独覆盖 Spring Boot、Spring Cloud、Nacos、LoadBalancer、Sentinel、Seata、RocketMQ 等核心版本。







