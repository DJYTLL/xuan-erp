package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 替换 IAM 用户角色命令。
 */
public record SetIamUserRolesCommand(
        Long tenantId,
        Long userId,
        List<Long> roleIds,
        String operator
) {
}
