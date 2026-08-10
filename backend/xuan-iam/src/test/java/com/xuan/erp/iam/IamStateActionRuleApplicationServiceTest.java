package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamResourceActionCommand;
import com.xuan.erp.iam.application.command.CreateIamResourceStateCommand;
import com.xuan.erp.iam.application.command.IamRoleStateActionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamRoleStateActionRulesCommand;
import com.xuan.erp.iam.application.service.IamStateActionRuleApplicationService;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamStateActionRuleManagementRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IAM 状态动作权限应用服务测试，验证状态、动作和角色矩阵维护规则。
 */
class IamStateActionRuleApplicationServiceTest {

    @Test
    void createsResourceStateAndRejectsDuplicateStateCode() {
        InMemoryStateActionRuleManagementRepository managementRepository = new InMemoryStateActionRuleManagementRepository();
        IamStateActionRuleApplicationService service = service(managementRepository);

        IamResourceState state = service.createState(new CreateIamResourceStateCommand(
                0L,
                " sales-order ",
                " draft ",
                "草稿",
                "可编辑未提交订单",
                10,
                true,
                "{\"tag\":\"info\"}",
                "security-admin"));

        assertEquals("sales-order", state.resourceKey());
        assertEquals("DRAFT", state.stateCode());
        assertEquals("security-admin", managementRepository.operator);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createState(new CreateIamResourceStateCommand(
                        0L, "sales-order", "DRAFT", "重复草稿", null, 20, true, "{}", "security-admin")));
        assertEquals("IAM_RESOURCE_STATE_CODE_EXISTS", error.code());
    }

    @Test
    void createsResourceActionAndRequiresExistingBasePermissionWhenProvided() {
        InMemoryStateActionRuleManagementRepository managementRepository = new InMemoryStateActionRuleManagementRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        permissionRepository.save(permission("sales-order:submit"));
        IamStateActionRuleApplicationService service = service(managementRepository, permissionRepository);

        IamResourceAction action = service.createAction(new CreateIamResourceActionCommand(
                0L,
                "sales-order",
                "submit",
                "提交",
                "sales-order:submit",
                "提交销售订单",
                20,
                true,
                "{}",
                "security-admin"));

        assertEquals("submit", action.actionCode());
        assertEquals("sales-order:submit", action.permissionCode());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createAction(new CreateIamResourceActionCommand(
                        0L, "sales-order", "audit", "审核", "sales-order:audit", null, 30, true, "{}", "security-admin")));
        assertEquals("IAM_PERMISSION_NOT_FOUND", error.code());
    }

    @Test
    void replacesRoleStateActionRulesAndRejectsUnknownStateOrAction() {
        InMemoryStateActionRuleManagementRepository managementRepository = new InMemoryStateActionRuleManagementRepository();
        managementRepository.states.put("0:sales-order:DRAFT", state(1L, 0L, "sales-order", "DRAFT"));
        managementRepository.actions.put("0:sales-order:submit", action(1L, 0L, "sales-order", "submit", null));
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        roleRepository.save(role(9L, 1001L, "sales_manager"));
        IamStateActionRuleApplicationService service = service(managementRepository, new InMemoryPermissionRepository(), roleRepository);

        List<IamRoleStateActionRule> rules = service.replaceRoleStateActionRules(new SetIamRoleStateActionRulesCommand(
                1001L,
                9L,
                List.of(new IamRoleStateActionRuleCommand("sales-order", "draft", "submit")),
                "security-admin"));

        assertEquals(List.of("submit"), rules.stream().map(IamRoleStateActionRule::actionCode).toList());
        assertEquals("security-admin", managementRepository.operator);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceRoleStateActionRules(new SetIamRoleStateActionRulesCommand(
                        1001L,
                        9L,
                        List.of(new IamRoleStateActionRuleCommand("sales-order", "APPROVED", "submit")),
                        "security-admin")));
        assertEquals("IAM_RESOURCE_STATE_NOT_FOUND", error.code());
    }

    @Test
    void rejectsReplacingPlatformRoleStateActionRules() {
        InMemoryStateActionRuleManagementRepository managementRepository = new InMemoryStateActionRuleManagementRepository();
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository();
        roleRepository.save(role(1L, 0L, "super_admin"));
        IamStateActionRuleApplicationService service = service(managementRepository, new InMemoryPermissionRepository(), roleRepository);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceRoleStateActionRules(new SetIamRoleStateActionRulesCommand(
                        0L,
                        1L,
                        List.of(),
                        "security-admin")));

        assertEquals("IAM_PLATFORM_ROLE_READ_ONLY", error.code());
    }

    private static IamStateActionRuleApplicationService service(
            InMemoryStateActionRuleManagementRepository managementRepository) {
        return service(managementRepository, new InMemoryPermissionRepository(), new InMemoryRoleRepository());
    }

    private static IamStateActionRuleApplicationService service(
            InMemoryStateActionRuleManagementRepository managementRepository,
            InMemoryPermissionRepository permissionRepository) {
        return service(managementRepository, permissionRepository, new InMemoryRoleRepository());
    }

    private static IamStateActionRuleApplicationService service(
            InMemoryStateActionRuleManagementRepository managementRepository,
            InMemoryPermissionRepository permissionRepository,
            InMemoryRoleRepository roleRepository) {
        return new IamStateActionRuleApplicationService(
                managementRepository,
                roleRepository,
                permissionRepository,
                new NoopRolePermissionRepository(),
                new NoopUserRepository(),
                new NoopAuthorizationSnapshotRepository(),
                new NoopColumnPermissionRepository());
    }

    private static IamResourceState state(Long id, Long tenantId, String resourceKey, String stateCode) {
        return new IamResourceState(id, tenantId, resourceKey, stateCode, stateCode, null, 10, true, "{}");
    }

    private static IamResourceAction action(Long id, Long tenantId, String resourceKey, String actionCode, String permissionCode) {
        return new IamResourceAction(id, tenantId, resourceKey, actionCode, actionCode, permissionCode, null, 10, true, "{}");
    }

    private static IamRole role(Long id, Long tenantId, String code) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T00:00:00+08:00");
        return new IamRole(id, tenantId, code, code, null, true, "system", now, "system", now, null, null, null);
    }

    private static IamPermission permission(String code) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T00:00:00+08:00");
        return new IamPermission(null, code, code, "xuan-sales", "sales-order", null, true, "system", now, "system", now, null, null, null);
    }

    private static final class InMemoryStateActionRuleManagementRepository implements IamStateActionRuleManagementRepository {
        private final Map<String, IamResourceState> states = new LinkedHashMap<>();
        private final Map<String, IamResourceAction> actions = new LinkedHashMap<>();
        private final Map<String, List<IamRoleStateActionRule>> roleRules = new LinkedHashMap<>();
        private long nextId = 1;
        private String operator;

        @Override
        public List<IamResourceState> findStates(Long tenantId, String resourceKey, Boolean enabled) {
            return states.values().stream().toList();
        }

        @Override
        public Optional<IamResourceState> findStateById(Long stateId) {
            return states.values().stream().filter(state -> state.id().equals(stateId)).findFirst();
        }

        @Override
        public Optional<IamResourceState> findState(Long tenantId, String resourceKey, String stateCode) {
            return Optional.ofNullable(states.get(tenantId + ":" + resourceKey + ":" + stateCode))
                    .or(() -> Optional.ofNullable(states.get("0:" + resourceKey + ":" + stateCode)));
        }

        @Override
        public IamResourceState saveState(IamResourceState state, String operator) {
            Long id = state.id() == null ? nextId++ : state.id();
            IamResourceState saved = new IamResourceState(
                    id,
                    state.tenantId(),
                    state.resourceKey(),
                    state.stateCode(),
                    state.stateName(),
                    state.description(),
                    state.sortNo(),
                    state.enabled(),
                    state.metadataJson());
            states.put(saved.tenantId() + ":" + saved.resourceKey() + ":" + saved.stateCode(), saved);
            this.operator = operator;
            return saved;
        }

        @Override
        public void setStateEnabled(Long stateId, boolean enabled, String operator) {
        }

        @Override
        public List<IamResourceAction> findActions(Long tenantId, String resourceKey, Boolean enabled) {
            return actions.values().stream().toList();
        }

        @Override
        public Optional<IamResourceAction> findActionById(Long actionId) {
            return actions.values().stream().filter(action -> action.id().equals(actionId)).findFirst();
        }

        @Override
        public Optional<IamResourceAction> findAction(Long tenantId, String resourceKey, String actionCode) {
            return Optional.ofNullable(actions.get(tenantId + ":" + resourceKey + ":" + actionCode))
                    .or(() -> Optional.ofNullable(actions.get("0:" + resourceKey + ":" + actionCode)));
        }

        @Override
        public IamResourceAction saveAction(IamResourceAction action, String operator) {
            Long id = action.id() == null ? nextId++ : action.id();
            IamResourceAction saved = new IamResourceAction(
                    id,
                    action.tenantId(),
                    action.resourceKey(),
                    action.actionCode(),
                    action.actionName(),
                    action.permissionCode(),
                    action.description(),
                    action.sortNo(),
                    action.enabled(),
                    action.metadataJson());
            actions.put(saved.tenantId() + ":" + saved.resourceKey() + ":" + saved.actionCode(), saved);
            this.operator = operator;
            return saved;
        }

        @Override
        public void setActionEnabled(Long actionId, boolean enabled, String operator) {
        }

        @Override
        public List<IamRoleStateActionRule> findRoleRules(Long tenantId, Long roleId) {
            return roleRules.getOrDefault(tenantId + ":" + roleId, List.of());
        }

        @Override
        public void replaceRoleRules(Long tenantId, Long roleId, List<IamRoleStateActionRule> rules, String operator) {
            roleRules.put(tenantId + ":" + roleId, List.copyOf(rules));
            this.operator = operator;
        }
    }

    private static final class InMemoryPermissionRepository implements IamPermissionRepository {
        private final Map<String, IamPermission> permissions = new LinkedHashMap<>();

        @Override
        public Optional<IamPermission> findByCode(String code) {
            return Optional.ofNullable(permissions.get(code)).filter(IamPermission::active);
        }

        @Override
        public List<IamPermission> findActivePermissions() {
            return permissions.values().stream().toList();
        }

        @Override
        public IamPermission save(IamPermission permission) {
            permissions.put(permission.code(), permission);
            return permission;
        }
    }

    private static final class InMemoryRoleRepository implements IamRoleRepository {
        private final Map<Long, IamRole> roles = new LinkedHashMap<>();

        @Override
        public Optional<IamRole> findById(Long id) {
            return Optional.ofNullable(roles.get(id));
        }

        @Override
        public List<IamRole> findActiveRoles(Long tenantId) {
            return roles.values().stream().filter(role -> role.tenantId().equals(tenantId)).toList();
        }

        @Override
        public IamRole save(IamRole role) {
            roles.put(role.id(), role);
            return role;
        }
    }

    private static final class NoopRolePermissionRepository implements IamRolePermissionRepository {
        @Override public List<String> findPermissionCodesByRoleId(Long tenantId, Long roleId) { return List.of(); }
        @Override public void replaceRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds, String operator) {}
        @Override public void removeRolePermissionsOutsideTenantEntitlements(Long tenantId, String operator) {}
        @Override public List<Long> findUserIdsByRoleId(Long tenantId, Long roleId) { return List.of(); }
        @Override public List<Long> findRoleIdsByUserId(Long tenantId, Long userId) { return List.of(); }
        @Override public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) { return List.of(); }
        @Override public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds, String operator) {}
    }

    private static final class NoopUserRepository implements IamUserRepository {
        @Override public Optional<com.xuan.erp.iam.domain.model.IamUser> findById(Long id) { return Optional.empty(); }
        @Override public Optional<com.xuan.erp.iam.domain.model.IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) { return Optional.empty(); }
        @Override public List<com.xuan.erp.iam.domain.model.IamUser> findActiveUsers(Long tenantId) { return List.of(); }
        @Override public com.xuan.erp.iam.domain.model.IamUser save(com.xuan.erp.iam.domain.model.IamUser user) { return user; }
    }

    private static final class NoopAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {
        @Override public Optional<com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) { return Optional.empty(); }
        @Override public com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot save(com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot snapshot) { return snapshot; }
    }

    private static final class NoopColumnPermissionRepository implements IamColumnPermissionRepository {
        @Override public Map<String, Map<String, String>> findMergedColumnPermissionsByRoleIds(Long tenantId, List<Long> roleIds) { return Map.of(); }
    }
}
