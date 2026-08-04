package com.xuan.erp.tenant.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * 租户套餐权限边界指纹生成器。
 */
public final class TenantPermissionSyncFingerprint {

    private TenantPermissionSyncFingerprint() {
    }

    public static String hash(
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode) {
        return sha256(canonical(iamInitTemplateCode, columnPermissionTemplateCodes, defaultColumnPermissionTemplateCode));
    }

    public static String canonical(
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode) {
        List<String> normalizedColumnCodes = columnPermissionTemplateCodes == null
                ? List.of()
                : columnPermissionTemplateCodes.stream()
                .map(TenantPermissionSyncFingerprint::textOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return "iamInitTemplateCode=" + textOrDefault(iamInitTemplateCode)
                + "|columnPermissionTemplateCodes=" + String.join(",", normalizedColumnCodes)
                + "|defaultColumnPermissionTemplateCode=" + textOrDefault(defaultColumnPermissionTemplateCode);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }

    private static String textOrDefault(String value) {
        String text = textOrNull(value);
        return text == null ? "-" : text;
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
