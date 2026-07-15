package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 保存角色权限授权命令，权限编码由应用服务解析为权限主键后落库。
 */
public record SetIamRolePermissionsCommand(
        Long tenantId,
        Long roleId,
        List<String> permissionCodes,
        String operator
) {
}
