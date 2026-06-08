---
title: "页面开发规范"
---

本文记录新增页面时的前端、后端、权限、菜单、列权限和测试要求。

## 前端调用链路

前端页面只调用 Gateway 暴露的 `/api/**` 接口，不直接调用 `xuan-product`、`xuan-sales` 这类后端服务名。

```ts
// src/api/product.ts
import request from '@/utils/request';

export interface ProductDTO {
  id: string;
  code: string;
  name: string;
}

export function getProduct(id: string) {
  return request<ProductDTO>({
    url: `/api/products/${id}`,
    method: 'get',
  });
}
```

推荐链路：

```text
Vue 页面 -> xuan-gateway -> 后端业务服务
```

如果某个后端服务还需要调用另一个服务，例如销售服务查询商品快照，则属于后端服务间调用：

```text
Vue 页面 -> xuan-gateway -> xuan-sales -> xuan-product
```

OpenFeign、LoadBalancer、`@LoadBalanced` 都属于后端服务间通信实现，前端不直接关心，也不直接使用服务名。详细说明见：[服务通信](/architecture/service-communication/)。

## 待补充

- 页面首屏接口清单。
- 权限接入规则。
- 列权限接入规则。
- 回归测试要求。



