package com.xuan.erp.iam.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamUserPreferenceView;
import com.xuan.erp.iam.domain.model.IamUserPreference;
import com.xuan.erp.iam.domain.repository.IamUserPreferenceRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 用户通用偏好应用服务，负责当前用户偏好读取和保存。
 */
@Service
public class IamUserPreferenceApplicationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final IamUserPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    public IamUserPreferenceApplicationService(
            IamUserPreferenceRepository preferenceRepository,
            ObjectMapper objectMapper) {
        this.preferenceRepository = preferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public IamUserPreferenceView getPreference(CurrentUser currentUser, String preferenceKey) {
        CurrentUser resolvedCurrentUser = requireCurrentUser(currentUser);
        String resolvedPreferenceKey = requirePreferenceKey(preferenceKey);
        return preferenceRepository.findByTenantIdAndUserIdAndPreferenceKey(
                        resolvedCurrentUser.tenantId(),
                        resolvedCurrentUser.userId(),
                        resolvedPreferenceKey)
                .map(this::toView)
                .orElseGet(() -> new IamUserPreferenceView(resolvedPreferenceKey, Map.of(), null));
    }

    @Transactional
    public IamUserPreferenceView savePreference(
            CurrentUser currentUser,
            String preferenceKey,
            Map<String, Object> value) {
        CurrentUser resolvedCurrentUser = requireCurrentUser(currentUser);
        String resolvedPreferenceKey = requirePreferenceKey(preferenceKey);
        OffsetDateTime now = OffsetDateTime.now();
        IamUserPreference existing = preferenceRepository.findByTenantIdAndUserIdAndPreferenceKey(
                        resolvedCurrentUser.tenantId(),
                        resolvedCurrentUser.userId(),
                        resolvedPreferenceKey)
                .orElse(null);
        IamUserPreference saved = preferenceRepository.save(new IamUserPreference(
                existing == null ? null : existing.id(),
                resolvedCurrentUser.tenantId(),
                resolvedCurrentUser.userId(),
                resolvedPreferenceKey,
                writeJson(value),
                existing == null ? resolvedCurrentUser.username() : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                resolvedCurrentUser.username(),
                now));
        return toView(saved);
    }

    private CurrentUser requireCurrentUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.tenantId() == null) {
            throw new BusinessException("IAM_UNAUTHORIZED", "当前请求未包含 IAM 登录上下文");
        }
        return currentUser;
    }

    private String requirePreferenceKey(String preferenceKey) {
        if (preferenceKey == null || preferenceKey.isBlank()) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "偏好键不能为空");
        }
        return preferenceKey.trim();
    }

    private IamUserPreferenceView toView(IamUserPreference preference) {
        return new IamUserPreferenceView(
                preference.preferenceKey(),
                readJson(preference.preferenceJson()),
                preference.updatedAt());
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("用户偏好 JSON 序列化失败", ex);
        }
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }
}
