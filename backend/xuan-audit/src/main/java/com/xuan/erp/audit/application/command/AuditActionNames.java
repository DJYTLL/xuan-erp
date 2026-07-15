package com.xuan.erp.audit.application.command;

import java.util.regex.Pattern;

public final class AuditActionNames {

    private static final int MAX_ACTION_LENGTH = 100;
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "^[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*$"
    );

    private AuditActionNames() {
    }

    public static String normalize(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("审计动作不能为空");
        }
        String normalized = action.trim();
        if (normalized.length() > MAX_ACTION_LENGTH) {
            throw new IllegalArgumentException("审计动作长度不能超过 100");
        }
        if (!ACTION_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("审计动作必须使用 <业务域>:<资源>:<动作> 格式，例如 iam:user:create");
        }
        return normalized;
    }
}
