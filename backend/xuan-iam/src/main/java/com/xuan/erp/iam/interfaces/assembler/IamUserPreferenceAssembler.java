package com.xuan.erp.iam.interfaces.assembler;

import com.xuan.erp.iam.application.query.IamUserPreferenceView;
import com.xuan.erp.iam.interfaces.dto.IamUserPreferenceResponse;

/**
 * IAM 用户通用偏好接口装配器。
 */
public final class IamUserPreferenceAssembler {

    private IamUserPreferenceAssembler() {
    }

    public static IamUserPreferenceResponse toResponse(IamUserPreferenceView view) {
        return new IamUserPreferenceResponse(view.preferenceKey(), view.value(), view.updatedAt());
    }
}
