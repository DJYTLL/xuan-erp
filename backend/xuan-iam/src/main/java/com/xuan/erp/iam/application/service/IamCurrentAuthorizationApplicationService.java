package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamStateActionRuleRepository;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * IAM 当前权限快照应用服务，负责返回前端可直接消费的当前菜单树和权限快照。
 */
@Service
public class IamCurrentAuthorizationApplicationService {

    private static final Set<String> PLATFORM_SUPER_ADMIN_USERNAMES = Set.of("super_admin", "superadmin");

    private final IamMenuRepository menuRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;
    private final IamColumnPermissionRepository columnPermissionRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamTenantPermissionEntitlementRepository tenantPermissionEntitlementRepository;
    private final IamStateActionRuleRepository stateActionRuleRepository;

    public IamCurrentAuthorizationApplicationService(
            IamMenuRepository menuRepository,
            IamPermissionRepository permissionRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamColumnPermissionRepository columnPermissionRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamTenantPermissionEntitlementRepository tenantPermissionEntitlementRepository) {
        this(
                menuRepository,
                permissionRepository,
                snapshotRepository,
                columnPermissionRepository,
                rolePermissionRepository,
                tenantPermissionEntitlementRepository,
                (tenantId, roleIds) -> Map.of());
    }

    @Autowired
    public IamCurrentAuthorizationApplicationService(
            IamMenuRepository menuRepository,
            IamPermissionRepository permissionRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamColumnPermissionRepository columnPermissionRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamTenantPermissionEntitlementRepository tenantPermissionEntitlementRepository,
            IamStateActionRuleRepository stateActionRuleRepository) {
        this.menuRepository = menuRepository;
        this.permissionRepository = permissionRepository;
        this.snapshotRepository = snapshotRepository;
        this.columnPermissionRepository = columnPermissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.tenantPermissionEntitlementRepository = tenantPermissionEntitlementRepository;
        this.stateActionRuleRepository = stateActionRuleRepository;
    }

    public IamCurrentPermissionSnapshotView getCurrentPermissionSnapshot(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.tenantId() == null) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }

        IamAuthorizationSnapshot snapshot = snapshotRepository.findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
                .orElse(null);
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserId(currentUser.tenantId(), currentUser.userId());
        List<String> permissionCodes = activePermissionCodes(snapshot, currentUser);
        Set<String> permissionCodeSet = new LinkedHashSet<>(permissionCodes);
        Set<String> menuCodeSet = activeMenuCodes(snapshot, permissionCodeSet);

        return new IamCurrentPermissionSnapshotView(
                buildMenuTree(menuCodeSet),
                permissionCodes,
                permissionCodes,
                activeColumnPermissions(snapshot, currentUser, roleIds),
                Map.of(),
                List.of(),
                activeStateActionRules(currentUser, roleIds),
                snapshot == null ? currentUser.authVersion() : snapshot.authVersion());
    }

    private Map<String, List<String>> activeStateActionRules(CurrentUser currentUser, List<Long> roleIds) {
        if (isSuperAdmin(currentUser)
                || currentUser.tenantId() == null
                || currentUser.tenantId() <= 0
                || roleIds == null
                || roleIds.isEmpty()) {
            return Map.of();
        }
        return stateActionRuleRepository.findMergedStateActionRulesByRoleIds(currentUser.tenantId(), roleIds);
    }

    private Map<String, Map<String, String>> activeColumnPermissions(
            IamAuthorizationSnapshot snapshot,
            CurrentUser currentUser,
            List<Long> roleIds) {
        if (isSuperAdmin(currentUser)) {
            return snapshot == null ? Map.of() : snapshot.columnSettings();
        }
        if (currentUser.tenantId() != null && currentUser.tenantId() > 0) {
            Map<String, Map<String, String>> merged = columnPermissionRepository
                    .findMergedColumnPermissionsByRoleIds(currentUser.tenantId(), roleIds);
            if (!merged.isEmpty()) {
                return merged;
            }
        }
        return snapshot == null ? Map.of() : snapshot.columnSettings();
    }

    private List<String> activePermissionCodes(IamAuthorizationSnapshot snapshot, CurrentUser currentUser) {
        Set<String> activePermissionCodes = permissionRepository.findActivePermissions().stream()
                .map(permission -> permission.code())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (isSuperAdmin(currentUser)) {
            return sortedPermissionCodesWithWildcard(activePermissionCodes);
        }
        Collection<String> preferredSource = preferredPermissionSource(snapshot, currentUser);
        List<String> filtered = preferredSource.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .filter(code -> activePermissionCodes.isEmpty() || activePermissionCodes.contains(code))
                .distinct()
                .sorted()
                .toList();
        if (snapshot != null) {
            return trimByTenantEntitlements(currentUser, filtered);
        }
        if (!filtered.isEmpty()) {
            return trimByTenantEntitlements(currentUser, filtered);
        }
        List<String> fallback = currentUser.permissions().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .filter(code -> activePermissionCodes.isEmpty() || activePermissionCodes.contains(code))
                .distinct()
                .sorted()
                .toList();
        return trimByTenantEntitlements(currentUser, fallback);
    }

    private Collection<String> preferredPermissionSource(IamAuthorizationSnapshot snapshot, CurrentUser currentUser) {
        List<String> rolePermissionCodes = rolePermissionRepository.findPermissionCodesByUserId(currentUser.tenantId(), currentUser.userId());
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserId(currentUser.tenantId(), currentUser.userId());
        if (!roleIds.isEmpty() || !rolePermissionCodes.isEmpty()) {
            return rolePermissionCodes;
        }
        return snapshot == null ? currentUser.permissions() : snapshot.permissionCodes();
    }

    private boolean isSuperAdmin(CurrentUser currentUser) {
        return Long.valueOf(0L).equals(currentUser.tenantId())
                && (currentUser.roles().contains("super_admin")
                || currentUser.permissions().contains("*")
                || PLATFORM_SUPER_ADMIN_USERNAMES.contains(currentUser.username()));
    }

    private List<String> trimByTenantEntitlements(CurrentUser currentUser, List<String> permissionCodes) {
        if (currentUser.tenantId() == null || currentUser.tenantId() <= 0 || permissionCodes.isEmpty()) {
            return permissionCodes;
        }
        Set<String> entitledPermissionCodes = tenantPermissionEntitlementRepository
                .findPermissionCodesByTenantId(currentUser.tenantId())
                .stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (entitledPermissionCodes.isEmpty()) {
            return List.of();
        }
        return permissionCodes.stream()
                .filter(entitledPermissionCodes::contains)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> sortedPermissionCodesWithWildcard(Set<String> activePermissionCodes) {
        return Stream.concat(Stream.of("*"), activePermissionCodes.stream().filter(code -> code != null && !code.isBlank()).sorted())
                .distinct()
                .toList();
    }

    private Set<String> activeMenuCodes(IamAuthorizationSnapshot snapshot, Set<String> permissionCodeSet) {
        List<IamMenu> activeMenus = menuRepository.findActiveMenus().stream()
                .filter(menu -> menu.enabled())
                .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                .toList();
        Set<String> menuCodes = permissionDerivedMenuCodes(activeMenus, permissionCodeSet);
        includeParentMenus(activeMenus, menuCodes);
        return menuCodes;
    }

    private Set<String> permissionDerivedMenuCodes(List<IamMenu> activeMenus, Set<String> permissionCodeSet) {
        if (permissionCodeSet.contains("*")) {
            return activeMenus.stream()
                    .map(IamMenu::code)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        }
        return activeMenus.stream()
                .filter(menu -> menu.permissionCode() == null || menu.permissionCode().isBlank() || permissionCodeSet.contains(menu.permissionCode()))
                .map(IamMenu::code)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private void includeParentMenus(List<IamMenu> activeMenus, Set<String> menuCodes) {
        Map<Long, IamMenu> menusById = activeMenus.stream()
                .collect(LinkedHashMap::new, (map, menu) -> map.put(menu.id(), menu), LinkedHashMap::putAll);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (IamMenu menu : activeMenus) {
                if (!menuCodes.contains(menu.code()) || menu.parentId() == null) {
                    continue;
                }
                IamMenu parent = menusById.get(menu.parentId());
                if (parent != null && menuCodes.add(parent.code())) {
                    changed = true;
                }
            }
        }
    }

    private List<IamCurrentMenuNodeView> buildMenuTree(Set<String> menuCodeSet) {
        List<IamMenu> activeMenus = menuRepository.findActiveMenus().stream()
                .filter(menu -> menu.enabled())
                .filter(menu -> menuCodeSet.contains(menu.code()))
                .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                .toList();
        Map<Long, List<IamMenu>> childrenByParentId = new LinkedHashMap<>();
        List<IamMenu> roots = new ArrayList<>();
        for (IamMenu menu : activeMenus) {
            if (menu.parentId() == null) {
                roots.add(menu);
                continue;
            }
            childrenByParentId.computeIfAbsent(menu.parentId(), ignored -> new ArrayList<>()).add(menu);
        }
        return roots.stream()
                .map(menu -> toMenuNode(menu, childrenByParentId))
                .filter(Objects::nonNull)
                .toList();
    }

    private IamCurrentMenuNodeView toMenuNode(IamMenu menu, Map<Long, List<IamMenu>> childrenByParentId) {
        List<IamCurrentMenuNodeView> children = childrenByParentId.getOrDefault(menu.id(), List.of()).stream()
                .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                .map(child -> toMenuNode(child, childrenByParentId))
                .filter(Objects::nonNull)
                .toList();
        if ((menu.path() == null || menu.path().isBlank()) && children.isEmpty()) {
            return null;
        }
        return new IamCurrentMenuNodeView(
                menu.code(),
                menu.title(),
                menu.i18nKey(),
                menu.path(),
                menu.icon(),
                menu.permissionCode(),
                menu.sortNo(),
                children);
    }
}
