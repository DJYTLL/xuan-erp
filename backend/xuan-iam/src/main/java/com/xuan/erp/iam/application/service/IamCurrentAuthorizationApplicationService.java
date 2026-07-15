package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamCurrentMenuNodeView;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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

    public IamCurrentAuthorizationApplicationService(
            IamMenuRepository menuRepository,
            IamPermissionRepository permissionRepository,
            IamAuthorizationSnapshotRepository snapshotRepository) {
        this.menuRepository = menuRepository;
        this.permissionRepository = permissionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public IamCurrentPermissionSnapshotView getCurrentPermissionSnapshot(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.tenantId() == null) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }

        IamAuthorizationSnapshot snapshot = snapshotRepository.findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
                .orElse(null);
        List<String> permissionCodes = activePermissionCodes(snapshot, currentUser);
        Set<String> permissionCodeSet = new LinkedHashSet<>(permissionCodes);
        Set<String> menuCodeSet = activeMenuCodes(snapshot, permissionCodeSet);

        return new IamCurrentPermissionSnapshotView(
                buildMenuTree(menuCodeSet),
                permissionCodes,
                permissionCodes,
                snapshot == null ? Map.of() : snapshot.columnSettings(),
                Map.of(),
                List.of(),
                Map.of(),
                snapshot == null ? currentUser.authVersion() : snapshot.authVersion());
    }

    private List<String> activePermissionCodes(IamAuthorizationSnapshot snapshot, CurrentUser currentUser) {
        Set<String> activePermissionCodes = permissionRepository.findActivePermissions().stream()
                .map(permission -> permission.code())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (isSuperAdmin(currentUser)) {
            return sortedPermissionCodesWithWildcard(activePermissionCodes);
        }
        Collection<String> preferredSource = snapshot == null ? currentUser.permissions() : snapshot.permissionCodes();
        List<String> filtered = preferredSource.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .filter(code -> activePermissionCodes.isEmpty() || activePermissionCodes.contains(code))
                .distinct()
                .sorted()
                .toList();
        if (snapshot != null) {
            return filtered;
        }
        if (!filtered.isEmpty()) {
            return filtered;
        }
        return currentUser.permissions().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private boolean isSuperAdmin(CurrentUser currentUser) {
        return currentUser.roles().contains("super_admin")
                || currentUser.permissions().contains("*")
                || (Long.valueOf(0L).equals(currentUser.tenantId()) && PLATFORM_SUPER_ADMIN_USERNAMES.contains(currentUser.username()));
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
        Set<String> permissionDerivedMenuCodes = permissionDerivedMenuCodes(activeMenus, permissionCodeSet);
        if (snapshot != null && snapshot.menuCodes() != null && !snapshot.menuCodes().isEmpty()) {
            Set<String> menuCodes = new LinkedHashSet<>(snapshot.menuCodes());
            menuCodes.addAll(permissionDerivedMenuCodes);
            includeParentMenus(activeMenus, menuCodes);
            return menuCodes;
        }
        Set<String> menuCodes = new LinkedHashSet<>(permissionDerivedMenuCodes);
        includeParentMenus(activeMenus, menuCodes);
        return menuCodes;
    }

    private Set<String> permissionDerivedMenuCodes(List<IamMenu> activeMenus, Set<String> permissionCodeSet) {
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
                .toList();
    }

    private IamCurrentMenuNodeView toMenuNode(IamMenu menu, Map<Long, List<IamMenu>> childrenByParentId) {
        List<IamCurrentMenuNodeView> children = childrenByParentId.getOrDefault(menu.id(), List.of()).stream()
                .sorted(Comparator.comparingInt(IamMenu::sortNo).thenComparing(IamMenu::code))
                .map(child -> toMenuNode(child, childrenByParentId))
                .toList();
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
