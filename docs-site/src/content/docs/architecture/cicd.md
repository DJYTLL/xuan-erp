---
title: "CI/CD 流程"
---

CI/CD 用于自动检查、构建、测试、生成权限迁移和部署服务。

## CI/CD 是什么

CI 是 Continuous Integration，持续集成。它发生在代码提交、推送分支、创建合并请求时，主要负责检查代码是否能合并。

CD 是 Continuous Delivery 或 Continuous Deployment，持续交付或持续部署。它发生在代码已经合并或打了发布标签后，主要负责把可发布产物部署到目标环境。

在 Xuan ERP 中，CI/CD 不是“服务启动时顺手做点事”，而是一条可审计、可回滚、可重复执行的发布流水线。

## 代码提交指什么

这里的代码提交指开发者把变更提交到 Git 仓库：

```text
git commit -> git push -> CI pipeline
```

比如 `xuan-product` 新增一个 `product:price:update` 权限时，代码提交应同时包含：

- Controller 或应用服务中的权限注解。
- `xuan-product/src/main/resources/permissions/permissions.yaml` 中的新权限定义。
- 菜单、pageKey、列权限定义。
- 权限回归测试。
- 如果涉及数据库初始化数据，再新增 Flyway migration。

CI 在这一步检查“变更是否完整”。它不应该直接修改生产 IAM 数据。

## 业务收口与提交规范

- 一个业务一个提交。
- 只暂存本次业务相关文件，避免 `git add .`。
- 根 README 记录业务变更：本次变更、影响范围、验证方式。
- 提交信息使用中文动宾短句，例如“修复库位分页异常”。
- 禁止模糊提交信息，例如“修改一下”“更新代码”“临时提交”。
- 生成文件默认不提交，除非本次业务确实需要。
- 提交前先说明本次改动范围、暂存文件范围、验证命令和验证结果。

收口脚本按暂存范围执行最小校验：前端改动跑前端检查，后端改动跑后端检查；同时改动时分别执行。

## 权限同步应该什么时候执行

| 时机 | 动作 | 适用环境 |
| --- | --- | --- |
| 本地开发启动 | 可选自动注册，方便调试 | dev |
| 代码提交后 CI | 扫描和校验权限清单，不写生产 IAM | dev/test/prod |
| 部署前置步骤 | 同步权限目录到目标环境 IAM | test/prod |
| 服务启动 | 只读自检，缺失则告警或失败 | test/prod |

商品服务新增权限后，不建议等商品服务生产启动时再写 IAM。原因是生产通常多实例启动，且启动动作不应该顺便做数据库变更。正确做法是：权限清单随代码提交，CI 校验，CD 部署前同步到 IAM，然后再启动服务。

## 推荐流水线

```text
代码提交
  -> 变更服务识别
  -> 编译与单元测试
  -> 权限/菜单/列权限扫描
  -> Flyway migration 版本校验
  -> 契约测试
  -> 构建 Docker 镜像
  -> 推送镜像仓库
  -> 部署前同步 IAM 权限目录
  -> 执行目标环境 migration
  -> 发布 Nacos 配置
  -> 滚动部署服务
  -> 健康检查和冒烟测试
```

## 测试与验证规范

- 权限接入类任务先写失败测试，再实现。
- 页面权限、菜单映射、未映射页面、列权限树等要有回归测试。
- ERP 搜索区布局要有结构测试，保证按钮不被筛选控件挤压。
- 性能敏感页面要测试入口页不直接引入重组件。
- 前端提交前跑类型检查和相关测试。
- 后端提交前至少跑编译检查，重要业务跑专项测试。
- 交付说明必须写清楚验证命令和结果。

## 单服务构建

monorepo 下可以根据变更路径只构建受影响服务。

示例：

```powershell
mvn -pl xuan-product -am clean verify
docker build -t registry.example.com/xuan-product:2026.06.07-001 ./xuan-product
docker push registry.example.com/xuan-product:2026.06.07-001
```

`-pl xuan-product -am` 表示构建 `xuan-product` 以及它依赖的公共模块。

如果公共模块变更，例如 `xuan-common-security`，CI 应识别所有依赖该模块的服务并执行更大范围测试。

## 权限扫描代码形态

推荐每个服务维护结构化权限清单：

```text
xuan-product/src/main/resources/permissions/
  menus.yaml
  permissions.yaml
  columns.yaml
```

CI 脚本做三类检查：

```text
scan-permissions
  -> 检查权限码格式
  -> 检查权限码全局唯一
  -> 检查 menuCode/pageKey 是否存在
  -> 检查 Controller 权限注解是否都能在 permissions.yaml 找到
  -> 检查新增页面是否有列权限声明或明确说明无列权限
```

伪代码：

```java
class PermissionCatalogCheck {
    void check(ServicePermissionCatalog catalog, ControllerPermissionIndex index) {
        assertNoDuplicatedPermissionCode(catalog);
        assertEveryControllerPermissionDeclared(catalog, index);
        assertEveryMenuHasPageKey(catalog);
        assertEveryColumnBelongsToPageKey(catalog);
    }
}
```

## IAM 同步脚本

部署前由流水线调用 IAM 管理接口或 migration 脚本：

```powershell
java -jar tools/permission-sync.jar `
  --env prod `
  --iam-url https://iam.example.com `
  --catalog ./build/permission-catalog.json `
  --mode upsert
```

同步策略：

- 新权限：新增到 IAM 权限目录，默认不授予普通角色。
- 已有权限：更新名称、说明、所属菜单等非授权字段。
- 删除权限：默认标记废弃，不直接物理删除。
- 授权关系：不由业务服务同步脚本修改，仍由 IAM 后台或独立授权迁移处理。

## Flyway migration 执行

每个服务维护自己的 migration：

```text
xuan-product/src/main/resources/db/migration/
xuan-sales/src/main/resources/db/migration/
```

流水线必须先校验：

- 版本号没有跳号。
- 没有复用历史版本号。
- 没有修改历史 migration 文件内容。
- migration 只作用于本服务数据库或 schema。

部署时可以选择：

| 方式 | 说明 |
| --- | --- |
| 服务启动自动 migrate | 简单，但生产多实例要保证只有一个实例执行 |
| 独立 migration job | 推荐，部署前单独执行，日志清晰，失败不会拉起新版本 |

生产更推荐独立 migration job。

## Nacos 配置发布

配置也纳入发布流水线：

```text
deploy/nacos/prod/xuan-product.yaml
deploy/nacos/prod/xuan-sales.yaml
```

发布要求：

- 配置变更必须经过代码评审。
- 密码、密钥不直接写入 Git。
- 配置发布先到灰度 namespace 或灰度 group。
- 配置发布后记录版本和操作人。

## 部署顺序

基础服务和依赖服务优先：

1. 基础设施：Nacos、Sentinel、RocketMQ、监控、日志。
2. `xuan-iam`：权限目录、菜单、角色授权基础能力。
3. `xuan-gateway`：路由、认证、限流。
4. 基础档案服务：`xuan-product`、`xuan-party`、`xuan-warehouse`。
5. 交易服务：`xuan-sales`、`xuan-procurement`、`xuan-inventory`、`xuan-finance`。
6. 辅助服务：`xuan-document`、`xuan-audit`、`xuan-query`。
7. 前端。

非首次上线时，可以按受影响服务滚动发布，但依赖契约发生变化时必须先发布兼容版本。

## 回滚策略

| 对象 | 回滚方式 |
| --- | --- |
| 应用镜像 | 回滚到上一镜像 tag |
| Nacos 配置 | 回滚到上一配置版本 |
| 权限目录 | 一般不删除，新增权限可保持未授权状态 |
| 数据库 migration | 不依赖自动回滚，优先写向前兼容 migration |
| 事件消费者 | 保持消息兼容，避免旧消费者无法消费新事件 |

数据库一旦执行 migration，生产回滚通常不能简单 `down`。因此 migration 必须向前兼容，例如先加字段、双写、迁移数据，再切读路径，最后清理旧字段。

## 最小 GitHub Actions 示例

```yaml
name: xuan-product-ci

on:
  pull_request:
    paths:
      - 'xuan-product/**'
      - 'xuan-common/**'
      - 'xuan-common-security/**'

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Build and test
        run: mvn -pl xuan-product -am clean verify
      - name: Check permissions
        run: java -jar tools/permission-check.jar --service xuan-product
      - name: Check migrations
        run: java -jar tools/flyway-version-check.jar --service xuan-product
```

正式生产流水线可以用 GitLab CI、Jenkins、GitHub Actions、云效或 Argo CD。工具不是关键，关键是检查、构建、同步、迁移、部署、验证这几个步骤要可重复、可审计。



