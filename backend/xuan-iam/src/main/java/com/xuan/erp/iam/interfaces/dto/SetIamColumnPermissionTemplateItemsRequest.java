package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 保存 IAM 列权限模板规则请求体。
 */
@Schema(description = "保存 IAM 列权限模板规则请求体")
public record SetIamColumnPermissionTemplateItemsRequest(
        @Schema(description = "字段规则集合")
        List<IamColumnPermissionTemplateItemRequest> items,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
