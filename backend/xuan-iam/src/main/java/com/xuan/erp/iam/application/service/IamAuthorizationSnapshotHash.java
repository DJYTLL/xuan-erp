package com.xuan.erp.iam.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class IamAuthorizationSnapshotHash {

    private IamAuthorizationSnapshotHash() {
    }

    static String from(List<Long> roleIds, List<String> permissionCodes) {
        return from(roleIds, permissionCodes, List.of());
    }

    static String from(List<Long> roleIds, List<String> permissionCodes, List<String> menuCodes) {
        String canonical = "permissions=" + normalizedStrings(permissionCodes)
                + "|menus=" + normalizedStrings(menuCodes)
                + "|roles=" + normalizedLongs(roleIds);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private static List<Long> normalizedLongs(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .sorted()
                .toList();
    }

    private static List<String> normalizedStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }
}
