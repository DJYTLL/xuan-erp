package com.xuan.erp.audit.domain.model.type;

import java.util.Arrays;
import java.util.Locale;

public enum AuditWriteStatus {
    SUCCESS,
    FAILED;

    public String code() {
        return name();
    }

    public static AuditWriteStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return SUCCESS;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的审计状态: " + code));
    }
}
