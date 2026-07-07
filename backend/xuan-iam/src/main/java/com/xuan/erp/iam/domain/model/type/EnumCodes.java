package com.xuan.erp.iam.domain.model.type;

import java.util.Locale;

final class EnumCodes {

    private EnumCodes() {
    }

    static <E extends Enum<E> & CodeEnum> E fromCode(Class<E> enumType, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(enumType.getSimpleName() + " code must not be blank");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (E item : enumType.getEnumConstants()) {
            if (item.code().equals(normalized)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unsupported " + enumType.getSimpleName() + " code: " + code);
    }
}
