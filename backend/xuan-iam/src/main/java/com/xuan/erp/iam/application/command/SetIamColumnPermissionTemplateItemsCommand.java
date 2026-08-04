package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 替换 IAM 列权限模板明细命令。
 */
public record SetIamColumnPermissionTemplateItemsCommand(
        Long templateId,
        List<IamColumnPermissionTemplateItemCommand> items,
        String operator
) {
}
