package com.xuan.erp.iam.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改 IAM 用户基础资料请求体，密码不允许通过该接口修改。
 */
@Schema(description = "修改 IAM 用户基础资料请求体")
public record UpdateIamUserRequest(
        @Schema(description = "租户 ID", example = "1001")
        Long tenantId,
        @Schema(description = "显示名", example = "采购员")
        String displayName,
        @Schema(description = "邮箱", example = "buyer@example.com")
        String email,
        @Schema(description = "手机号", example = "13900000000")
        String phone,
        @Schema(description = "是否启用", example = "true")
        Boolean enabled,
        @Schema(description = "备注", example = "负责采购下单")
        String remark,
        @Schema(description = "操作人", example = "security-admin")
        String operator
) {
}
