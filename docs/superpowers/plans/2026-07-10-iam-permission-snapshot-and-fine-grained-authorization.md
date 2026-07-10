# IAM Permission Snapshot And Fine-Grained Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 先让 `xuan-iam` 输出前端可直接消费的当前用户菜单树和权限快照，再分阶段扩展列权限、字段编辑权限、数据范围权限和状态动作权限。

**Architecture:** 第一阶段只改 `backend/xuan-iam` 的读路径接口与查询组装，不新增 migration，复用现有 `iam_menu`、`iam_permission`、`iam_authorization_snapshot` 和 `CurrentUser`。第二阶段再在 `backend/xuan-iam` 追加 `V4__*.sql`，把细颗粒权限结构正式入库，并由 `xuan-common-security` 提供统一消费能力，业务模块做最终鉴权兜底。

**Tech Stack:** Spring Boot, Spring Security, MyBatis XML, Flyway, JUnit 5, OpenAPI, Markdown

---

## File Structure

- `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/`
  - 继续承载 IAM 对外接口；第一阶段新增“当前菜单树 / 当前权限快照”接口最合适。
- `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/`
  - 新增当前权限快照查询服务，负责把 `CurrentUser`、授权快照、菜单目录、权限目录组装成前端可直接消费的结果。
- `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/`
  - 新增当前菜单树节点、当前权限快照视图等查询对象。
- `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/`
  - 新增接口响应 DTO，避免直接把领域对象暴露给前端。
- `backend/xuan-iam/src/test/java/com/xuan/erp/iam/`
  - 第一阶段新增应用服务测试和接口装配测试；优先复用当前仓库已经存在的 in-memory repository 测试风格。
- `backend/xuan-iam/src/main/resources/db/migration/`
  - 第一阶段不改。
  - 第二阶段如需细颗粒权限入库，必须从当前最高版本 `V3__extend_iam_tenant_bootstrap_admin_account.sql` 之后追加 `V4__*.sql`。
- `backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/`
  - 第二阶段增强统一权限判断与细颗粒权限消费，不持有权限事实数据。
- `backend/xuan-product` / `backend/xuan-procurement`
  - 第二阶段末或第三阶段做试点，接接口权限、状态权限、数据范围权限。

---

### Task 1: 固化阶段 A 接口契约并补失败测试

**Files:**
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamCurrentAuthorizationApplicationServiceTest.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamChineseDocumentationTest.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamDddSkeletonTest.java`

- [ ] **Step 1: 写“当前权限快照接口契约”的失败测试**

```java
@Test
void buildsCurrentPermissionSnapshotFromCurrentUserAndAuthorizationSnapshot() {
    InMemoryMenuRepository menuRepository = new InMemoryMenuRepository(
            menu("system", null, "系统", "/system", "iam:view", 120),
            menu("product", null, "商品", "/product", "product:read", 20));
    InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository(
            permission("iam:view", "权限查看", "xuan-iam", "system"),
            permission("product:read", "商品查看", "xuan-product", "product"));
    InMemoryAuthorizationSnapshotRepository snapshotRepository = new InMemoryAuthorizationSnapshotRepository(
            snapshot(1L, 1001L, 7L, List.of("iam:view", "product:read"), List.of("system", "product")));

    IamCurrentAuthorizationApplicationService service = new IamCurrentAuthorizationApplicationService(
            menuRepository, permissionRepository, snapshotRepository);

    CurrentUser currentUser = new CurrentUser(
            1001L, 1L, "tenant_admin", Set.of("tenant_admin"), 7L, Set.of("iam:view", "product:read"));

    IamCurrentPermissionSnapshotView view = service.getCurrentPermissionSnapshot(currentUser);

    assertThat(view.authVersion()).isEqualTo(7L);
    assertThat(view.routePermissions()).containsExactly("iam:view", "product:read");
    assertThat(view.buttonPermissions()).containsExactly("iam:view", "product:read");
    assertThat(view.menus()).extracting(IamCurrentMenuNodeView::code).containsExactly("product", "system");
}
```

- [ ] **Step 2: 跑定向测试，确认因为生产代码不存在而失败**

Run: `mvn -pl backend/xuan-iam -am "-Dtest=IamCurrentAuthorizationApplicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，提示 `IamCurrentAuthorizationApplicationService` / `IamCurrentPermissionSnapshotView` / `IamCurrentMenuNodeView` 等类型尚不存在。

- [ ] **Step 3: 写“中文文档与骨架清单”回归测试更新**

```java
assertThat(classes)
        .contains(
                com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService.class,
                com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView.class,
                com.xuan.erp.iam.application.query.IamCurrentMenuNodeView.class,
                com.xuan.erp.iam.interfaces.dto.IamCurrentPermissionSnapshotResponse.class,
                com.xuan.erp.iam.interfaces.dto.IamCurrentMenuNodeResponse.class);
```

- [ ] **Step 4: 跑骨架/中文注释测试确认先失败**

Run: `mvn -pl backend/xuan-iam -am "-Dtest=IamChineseDocumentationTest,IamDddSkeletonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，提示新增类型尚未落地。

- [ ] **Step 5: Commit**

```bash
git add backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamCurrentAuthorizationApplicationServiceTest.java backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamChineseDocumentationTest.java backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamDddSkeletonTest.java
git commit -m "test: define current permission snapshot contract"
```

### Task 2: 在 xuan-iam 第一阶段实现“当前菜单树 + 当前权限快照”真实接口

**Files:**
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamCurrentAuthorizationApplicationService.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/IamCurrentPermissionSnapshotView.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/IamCurrentMenuNodeView.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamCurrentPermissionSnapshotResponse.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamCurrentMenuNodeResponse.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/assembler/IamCurrentAuthorizationAssembler.java`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/IamAuthenticationController.java`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamMenuApplicationService.java`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamPermissionApplicationService.java`

- [ ] **Step 1: 新增当前菜单树和权限快照查询对象**

```java
public record IamCurrentMenuNodeView(
        String code,
        String title,
        String i18nKey,
        String path,
        String icon,
        String permissionCode,
        int sortNo,
        List<IamCurrentMenuNodeView> children
) {
}

public record IamCurrentPermissionSnapshotView(
        List<IamCurrentMenuNodeView> menus,
        List<String> routePermissions,
        List<String> buttonPermissions,
        Map<String, List<String>> columnPermissions,
        Map<String, List<String>> fieldPermissions,
        List<String> dataScopes,
        Map<String, List<String>> stateActionRules,
        Long authVersion
) {
}
```

- [ ] **Step 2: 新增最小实现服务，先只复用现有快照与目录数据**

```java
@Service
public class IamCurrentAuthorizationApplicationService {

    private final IamMenuRepository menuRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;

    public IamCurrentAuthorizationApplicationService(
            IamMenuRepository menuRepository,
            IamPermissionRepository permissionRepository,
            IamAuthorizationSnapshotRepository snapshotRepository) {
        this.menuRepository = menuRepository;
        this.permissionRepository = permissionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public IamCurrentPermissionSnapshotView getCurrentPermissionSnapshot(CurrentUser currentUser) {
        IamAuthorizationSnapshot snapshot = snapshotRepository.findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
                .orElseThrow(() -> new BusinessException("IAM_AUTHORIZATION_SNAPSHOT_NOT_FOUND", "当前用户授权快照不存在"));
        List<IamMenu> activeMenus = menuRepository.findActiveMenus();
        Set<String> allowedMenuCodes = new LinkedHashSet<>(snapshot.menuCodes());
        List<IamCurrentMenuNodeView> menuTree = buildMenuTree(activeMenus, allowedMenuCodes);
        List<String> permissionCodes = snapshot.permissionCodes().stream().sorted().toList();
        return new IamCurrentPermissionSnapshotView(
                menuTree,
                permissionCodes,
                permissionCodes,
                snapshot.columnSettings(),
                Map.of(),
                List.of(),
                Map.of(),
                snapshot.authVersion());
    }
}
```

- [ ] **Step 3: 在接口层暴露前端真实接口**

```java
@Operation(summary = "查询当前用户权限快照", description = "返回当前登录用户的菜单树、路由权限、按钮权限和权限版本")
@GetMapping("/api/iam/permissions/current")
public ApiResponse<IamCurrentPermissionSnapshotResponse> currentPermissions(Authentication authentication) {
    CurrentUser currentUser = requireCurrentUser(authentication);
    return ApiResponse.success(IamCurrentAuthorizationAssembler.toResponse(
            currentAuthorizationApplicationService.getCurrentPermissionSnapshot(currentUser)));
}

@Operation(summary = "查询当前用户菜单树", description = "返回当前登录用户可见菜单树")
@GetMapping("/api/iam/menus/current")
public ApiResponse<List<IamCurrentMenuNodeResponse>> currentMenus(Authentication authentication) {
    CurrentUser currentUser = requireCurrentUser(authentication);
    return ApiResponse.success(IamCurrentAuthorizationAssembler.toMenuTreeResponse(
            currentAuthorizationApplicationService.getCurrentPermissionSnapshot(currentUser).menus()));
}
```

- [ ] **Step 4: 运行当前权限快照测试，确认通过**

Run: `mvn -pl backend/xuan-iam -am "-Dtest=IamCurrentAuthorizationApplicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS，新增测试通过。

- [ ] **Step 5: 运行 IAM 模块测试，确认没有回归**

Run: `mvn -pl backend/xuan-iam -am test`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamCurrentAuthorizationApplicationService.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/IamCurrentPermissionSnapshotView.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/IamCurrentMenuNodeView.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamCurrentPermissionSnapshotResponse.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamCurrentMenuNodeResponse.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/assembler/IamCurrentAuthorizationAssembler.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/IamAuthenticationController.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamMenuApplicationService.java backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamPermissionApplicationService.java
git commit -m "feat: add current menu tree and permission snapshot endpoints"
```

### Task 3: 同步文档并冻结阶段 A 边界

**Files:**
- Modify: `docs-site/src/content/docs/security/iam.md`
- Modify: `docs-site/src/content/docs/backend/permission-integration.md`

- [ ] **Step 1: 在 IAM 文档里补“前端可消费真实接口”**

```md
### 当前用户权限快照接口

- `GET /api/iam/menus/current`
- `GET /api/iam/permissions/current`

第一阶段返回：
- `menus`
- `routePermissions`
- `buttonPermissions`
- `columnPermissions`
- `authVersion`

其中 `fieldPermissions`、`dataScopes`、`stateActionRules` 在第一阶段返回空结构，占位但不承诺已配置化。
```

- [ ] **Step 2: 在权限接入文档里明确阶段 A 不新增 migration**

```md
当前 `xuan-iam` migration 已到 `V3__extend_iam_tenant_bootstrap_admin_account.sql`。
第一阶段“当前菜单树 / 当前权限快照”实现只复用现有表：
- `iam_menu`
- `iam_permission`
- `iam_authorization_snapshot`

因此第一阶段不新增 Flyway migration。
```

- [ ] **Step 3: 运行文档相关回归测试**

Run: `mvn -pl backend/xuan-iam -am "-Dtest=IamChineseDocumentationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add docs-site/src/content/docs/security/iam.md docs-site/src/content/docs/backend/permission-integration.md
git commit -m "docs: document current menu tree and permission snapshot phase"
```

### Task 4: 设计阶段 B 的细颗粒权限 migration 与事实模型

**Files:**
- Create: `docs/superpowers/plans/2026-07-10-iam-fine-grained-permission-v4-design-notes.md`
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamFineGrainedPermissionMigrationContractTest.java`
- Create: `backend/xuan-iam/src/main/resources/db/migration/V4__add_fine_grained_permission_model.sql`

- [ ] **Step 1: 先写 migration contract 测试，锁定 V4 必须新增而不能改 V1/V2/V3**

```java
@Test
void expectsFineGrainedPermissionMigrationToUseV4() {
    Path migrationDir = Path.of("src/main/resources/db/migration");
    assertThat(Files.exists(migrationDir.resolve("V4__add_fine_grained_permission_model.sql"))).isTrue();
    assertThat(Files.exists(migrationDir.resolve("V3__extend_iam_tenant_bootstrap_admin_account.sql"))).isTrue();
}
```

- [ ] **Step 2: 运行测试确认先失败**

Run: `mvn -pl backend/xuan-iam -am "-Dtest=IamFineGrainedPermissionMigrationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL，因为 `V4__add_fine_grained_permission_model.sql` 尚不存在。

- [ ] **Step 3: 新增 V4 migration，第一版只补细颗粒权限事实表，不碰第一阶段接口**

```sql
CREATE TABLE IF NOT EXISTS iam_data_scope (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    code varchar(100) NOT NULL,
    name varchar(200) NOT NULL,
    scope_type varchar(40) NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_iam_data_scope PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS iam_role_data_scope (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    tenant_id bigint NOT NULL,
    role_id bigint NOT NULL,
    data_scope_id bigint NOT NULL,
    resource_code varchar(150) NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_iam_role_data_scope PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS iam_permission_rule (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    permission_code varchar(150) NOT NULL,
    resource_status varchar(60) NOT NULL,
    rule_type varchar(40) NOT NULL,
    rule_value jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_iam_permission_rule PRIMARY KEY (id)
);
```

- [ ] **Step 4: 跑 migration contract 与 IAM 测试**

Run: `mvn -pl backend/xuan-iam -am test`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-07-10-iam-fine-grained-permission-v4-design-notes.md backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamFineGrainedPermissionMigrationContractTest.java backend/xuan-iam/src/main/resources/db/migration/V4__add_fine_grained_permission_model.sql
git commit -m "feat: add v4 fine-grained permission model"
```

### Task 5: 让共享安全模块和业务服务消费细颗粒权限

**Files:**
- Modify: `backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/CurrentUser.java`
- Create: `backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/permission/PermissionSnapshot.java`
- Create: `backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/permission/PermissionSnapshotEvaluator.java`
- Modify: `backend/xuan-product` 或 `backend/xuan-procurement` 中一个试点模块的控制器/查询服务

- [ ] **Step 1: 先给共享模块定义细颗粒快照消费对象**

```java
public record PermissionSnapshot(
        Set<String> routePermissions,
        Set<String> buttonPermissions,
        Map<String, Set<String>> columnPermissions,
        Map<String, Set<String>> fieldPermissions,
        Map<String, Set<String>> stateActionRules,
        Map<String, Set<String>> dataScopes,
        Long authVersion
) {
}
```

- [ ] **Step 2: 增加统一判断器，而不是把规则散落到业务模块**

```java
public class PermissionSnapshotEvaluator {

    public boolean canViewColumn(PermissionSnapshot snapshot, String pageKey, String columnCode) {
        return snapshot.columnPermissions().getOrDefault(pageKey, Set.of()).contains(columnCode);
    }

    public boolean canEditField(PermissionSnapshot snapshot, String pageKey, String fieldCode) {
        return snapshot.fieldPermissions().getOrDefault(pageKey, Set.of()).contains(fieldCode);
    }

    public boolean canRunStateAction(PermissionSnapshot snapshot, String resourceCode, String status, String action) {
        return snapshot.stateActionRules().getOrDefault(resourceCode + ":" + status, Set.of()).contains(action);
    }
}
```

- [ ] **Step 3: 在试点业务模块落一个真实校验**

```java
@PreAuthorize("hasAuthority('procurement:order:update')")
public void updateOrder(UpdateProcurementOrderCommand command) {
    ProcurementOrder order = repository.get(command.id());
    if (!permissionSnapshotEvaluator.canRunStateAction(snapshot, "procurement:order", order.status().name(), "update")) {
        throw new BusinessException("PROCUREMENT_ORDER_UPDATE_FORBIDDEN", "当前状态不允许编辑采购单");
    }
}
```

- [ ] **Step 4: 跑共享模块和试点模块测试**

Run: `mvn -pl backend/xuan-common-security,backend/xuan-procurement -am test`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/CurrentUser.java backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/permission/PermissionSnapshot.java backend/xuan-common-security/src/main/java/com/xuan/erp/common/security/permission/PermissionSnapshotEvaluator.java backend/xuan-procurement
git commit -m "feat: consume fine-grained permission snapshot in shared security and pilot service"
```

---

## Self-Review

- 需求覆盖：计划覆盖了“先列项目计划”“先改哪个模块能实现菜单树和所有权限控制”“阶段 A 先不加 migration”“阶段 B 再落细颗粒权限 migration”这四个核心要求。
- 占位扫描：文中没有 `TODO/TBD` 式占位；阶段 A、阶段 B 的边界和文件路径都已写死。
- 类型一致性：第一阶段统一围绕 `IamCurrentPermissionSnapshotView` / `IamCurrentMenuNodeView`；第二阶段统一围绕 `PermissionSnapshot` / `PermissionSnapshotEvaluator`，避免前后命名漂移。
