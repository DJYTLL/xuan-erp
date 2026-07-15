package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamMenuCommand;
import com.xuan.erp.iam.application.command.UpdateIamMenuCommand;
import com.xuan.erp.iam.domain.model.IamMenu;
import com.xuan.erp.iam.domain.repository.IamMenuRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 菜单应用服务，负责全局菜单目录查询用例。
 */
public class IamMenuApplicationService {

    private final IamMenuRepository menuRepository;

    public IamMenuApplicationService(IamMenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public List<IamMenu> listMenus() {
        return menuRepository.findActiveMenus();
    }

    public IamMenu createMenu(CreateIamMenuCommand command) {
        String code = requireText(command.code(), "菜单编码不能为空");
        if (menuRepository.findByCode(code).isPresent()) {
            throw new BusinessException("IAM_MENU_CODE_EXISTS", "菜单编码已存在");
        }
        Long parentId = validParentId(command.parentId());
        OffsetDateTime now = OffsetDateTime.now();
        return menuRepository.save(new IamMenu(
                null,
                code,
                parentId,
                requireText(command.title(), "菜单标题不能为空"),
                trimToNull(command.i18nKey()),
                trimToNull(command.path()),
                trimToNull(command.icon()),
                trimToNull(command.permissionCode()),
                command.sortNo() == null ? 0 : command.sortNo(),
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null));
    }

    public IamMenu updateMenu(Long id, UpdateIamMenuCommand command) {
        IamMenu existing = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("IAM_MENU_NOT_FOUND", "菜单不存在"));
        Long parentId = validParentId(command.parentId());
        OffsetDateTime now = OffsetDateTime.now();
        return menuRepository.save(new IamMenu(
                existing.id(),
                existing.code(),
                parentId,
                requireText(command.title(), "菜单标题不能为空"),
                trimToNull(command.i18nKey()),
                trimToNull(command.path()),
                trimToNull(command.icon()),
                trimToNull(command.permissionCode()),
                command.sortNo() == null ? existing.sortNo() : command.sortNo(),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    public IamMenu setMenuEnabled(Long id, boolean enabled) {
        IamMenu existing = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("IAM_MENU_NOT_FOUND", "菜单不存在"));
        OffsetDateTime now = OffsetDateTime.now();
        return menuRepository.save(new IamMenu(
                existing.id(),
                existing.code(),
                existing.parentId(),
                existing.title(),
                existing.i18nKey(),
                existing.path(),
                existing.icon(),
                existing.permissionCode(),
                existing.sortNo(),
                enabled,
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    private Long validParentId(Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (parentId <= 0 || menuRepository.findById(parentId).isEmpty()) {
            throw new BusinessException("IAM_MENU_PARENT_NOT_FOUND", "父级菜单不存在");
        }
        return parentId;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
