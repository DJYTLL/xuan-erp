package com.xuan.erp.tenant.domain.model.type;

public enum ConfigValueType implements CodeEnum {
    STRING,
    NUMBER,
    BOOLEAN,
    JSON;

    @Override
    public String code() {
        return name().toLowerCase();
    }

    public static ConfigValueType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("ConfigValueType code must not be blank");
        }
        for (ConfigValueType value : values()) {
            if (value.code().equals(code.trim().toLowerCase())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported ConfigValueType code: " + code);
    }
}
