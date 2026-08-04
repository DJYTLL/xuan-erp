package com.xuan.erp.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.repository.TenantIamBootstrapGateway;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
/**
 * 租户套餐分配应用服务，提供套餐分配记录的增删改查入口。
 */
@Service
public class TenantPlanAssignmentApplicationService {

    private static final String RESOURCE_NAME = "tenant-plan-assignments";
    private static final String TENANT_EVENT_TOPIC = "xuan-tenant-event";
    private static final String AGGREGATE_TYPE = "TENANT_PLAN_ASSIGNMENT";
    private static final String EVENT_IAM_BOOTSTRAP_REQUESTED = "TenantIamBootstrapRequested";
    private static final String IAM_BOOTSTRAP_STEP_KEY = "IAM_BOOTSTRAP";
    private static final String SOURCE_SERVICE = "xuan-tenant";

    private final TenantResourceApplicationService resourceService;
    private final TenantPlanRepository tenantPlanRepository;
    private final TenantOutboxEventRepository outboxEventRepository;
    private final TenantIamBootstrapGateway iamBootstrapGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 注入底层通用租户资源应用服务。
     */
    public TenantPlanAssignmentApplicationService(TenantResourceApplicationService resourceService) {
        this(resourceService, null, null, null);
    }

    public TenantPlanAssignmentApplicationService(
            TenantResourceApplicationService resourceService,
            @Nullable TenantPlanRepository tenantPlanRepository,
            @Nullable TenantOutboxEventRepository outboxEventRepository) {
        this(resourceService, tenantPlanRepository, outboxEventRepository, null);
    }

    @Autowired
    public TenantPlanAssignmentApplicationService(
            TenantResourceApplicationService resourceService,
            @Nullable TenantPlanRepository tenantPlanRepository,
            @Nullable TenantOutboxEventRepository outboxEventRepository,
            @Nullable TenantIamBootstrapGateway iamBootstrapGateway) {
        this.resourceService = resourceService;
        this.tenantPlanRepository = tenantPlanRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.iamBootstrapGateway = iamBootstrapGateway;
    }

    /**
     * 查询全部套餐分配记录。
     */
    public List<Map<String, Object>> listAssignments() {
        return resourceService.list(RESOURCE_NAME);
    }

    /**
     * 按主键查询单条套餐分配记录。
     */
    public Map<String, Object> getAssignment(Long id) {
        return resourceService.get(RESOURCE_NAME, id);
    }

    /**
     * 新增一条套餐分配记录。
     */
    public Map<String, Object> createAssignment(Map<String, Object> values) {
        Map<String, Object> saved = resourceService.create(RESOURCE_NAME, normalizeRequiredDefaults(values));
        appendIamTemplateSyncRequest(saved, saved);
        return saved;
    }

    /**
     * 更新指定套餐分配记录。
     */
    public Map<String, Object> updateAssignment(Long id, Map<String, Object> values) {
        Map<String, Object> saved = resourceService.update(RESOURCE_NAME, id, normalizeRequiredDefaults(values));
        Map<String, Object> fallback = new LinkedHashMap<>(values);
        fallback.put("id", id);
        appendIamTemplateSyncRequest(saved, fallback);
        return saved;
    }

    /**
     * 逻辑删除指定套餐分配记录，并记录删除原因与操作人。
     */
    public void deleteAssignment(Long id, String reason, String operator) {
        resourceService.delete(RESOURCE_NAME, id, reason, operator);
    }

    /**
     * 前端未选择生效时间时按立即生效处理，避免通用 CRUD 把 null 写入非空字段。
     */
    private Map<String, Object> normalizeRequiredDefaults(Map<String, Object> values) {
        Map<String, Object> normalized = new LinkedHashMap<>(values);
        OffsetDateTime now = OffsetDateTime.now();
        if (isBlank(normalized.get("status"))) {
            normalized.put("status", "ACTIVE");
        }
        normalizeDateAlias(normalized, "effectiveAt", "effective_at", now);
        normalizeDateAlias(normalized, "assignedAt", "assigned_at", now);
        return normalized;
    }

    private void normalizeDateAlias(Map<String, Object> values, String camelKey, String snakeKey, OffsetDateTime defaultValue) {
        Object camelValue = values.get(camelKey);
        Object snakeValue = values.get(snakeKey);
        if (!isBlank(camelValue)) {
            return;
        }
        if (!isBlank(snakeValue)) {
            values.put(camelKey, snakeValue);
            return;
        }
        values.put(camelKey, defaultValue);
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    private void appendIamTemplateSyncRequest(Map<String, Object> savedValues, Map<String, Object> fallbackValues) {
        if (tenantPlanRepository == null || outboxEventRepository == null) {
            return;
        }
        Long tenantId = longValue(firstNonBlank(savedValues, fallbackValues, "tenant_id", "tenantId"));
        Long planId = longValue(firstNonBlank(savedValues, fallbackValues, "plan_id", "planId"));
        if (tenantId == null || planId == null) {
            return;
        }
        TenantPlan plan = tenantPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("TENANT_PLAN_NOT_FOUND", "租户套餐不存在"));
        String iamInitTemplateCode = iamInitTemplateCode(plan);
        List<String> columnPermissionTemplateCodes = columnPermissionTemplateCodes(plan);
        String defaultColumnPermissionTemplateCode = defaultColumnPermissionTemplateCode(plan);
        if (iamInitTemplateCode == null && columnPermissionTemplateCodes == null) {
            return;
        }
        Long assignmentId = longValue(firstNonBlank(savedValues, fallbackValues, "id"));
        String operator = textOrDefault(firstNonBlank(savedValues, fallbackValues, "assigned_by", "assignedBy"), "system");
        appendIamTemplateSyncRequest(
                tenantId,
                planId,
                assignmentId,
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode,
                operator);
    }

    void appendIamTemplateSyncRequest(Long tenantId, Long planId, Long assignmentId, String iamInitTemplateCode, String operator) {
        appendIamTemplateSyncRequest(tenantId, planId, assignmentId, iamInitTemplateCode, null, null, operator);
    }

    void appendIamTemplateSyncRequest(
            Long tenantId,
            Long planId,
            Long assignmentId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String operator) {
        appendIamTemplateSyncRequest(
                tenantId,
                planId,
                assignmentId,
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode,
                operator,
                true);
    }

    void appendIamTemplateSyncRequest(
            Long tenantId,
            Long planId,
            Long assignmentId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String operator,
            boolean syncImmediately) {
        OffsetDateTime now = OffsetDateTime.now();
        String eventId = UUID.randomUUID().toString();
        String taskKey = "tenant-plan-assignment:" + (assignmentId == null ? planId : assignmentId) + ":iam-template";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        if (assignmentId != null) {
            payload.put("assignmentId", assignmentId);
        }
        payload.put("planId", planId);
        payload.put("eventType", EVENT_IAM_BOOTSTRAP_REQUESTED);
        payload.put("taskKey", taskKey);
        payload.put("provisionStep", IAM_BOOTSTRAP_STEP_KEY);
        String permissionHash = TenantPermissionSyncFingerprint.hash(
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode);
        payload.put("idempotencyKey", taskKey + ":" + permissionHash);
        payload.put("iamInitTemplateCode", iamInitTemplateCode);
        if (columnPermissionTemplateCodes != null) {
            payload.put("columnPermissionTemplateCodes", columnPermissionTemplateCodes);
        }
        if (defaultColumnPermissionTemplateCode != null) {
            payload.put("defaultColumnPermissionTemplateCode", defaultColumnPermissionTemplateCode);
        }
        payload.put("permissionHash", permissionHash);
        payload.put("callbackRequired", false);
        payload.put("occurredAt", now.toString());
        payload.put("sourceService", SOURCE_SERVICE);
        updateAssignmentSyncState(assignmentId, permissionHash, "REPAIRING", now, null, null, operator);
        outboxEventRepository.append(new TenantOutboxEvent(
                null,
                tenantId,
                eventId,
                AGGREGATE_TYPE,
                assignmentId == null ? planId : assignmentId,
                EVENT_IAM_BOOTSTRAP_REQUESTED,
                TENANT_EVENT_TOPIC,
                toJson(payload),
                toJson(Map.of("sourceService", SOURCE_SERVICE)),
                OutboxEventStatus.PENDING,
                0,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                operator,
                now,
                operator,
                now));
        if (!syncImmediately) {
            return;
        }
        try {
            bootstrapIamTemplateImmediately(
                    tenantId,
                    iamInitTemplateCode,
                    columnPermissionTemplateCodes,
                    defaultColumnPermissionTemplateCode,
                    permissionHash,
                    operator);
            updateAssignmentSyncState(assignmentId, permissionHash, "SYNCED", OffsetDateTime.now(), null, null, operator);
        } catch (RuntimeException error) {
            updateAssignmentSyncState(
                    assignmentId,
                    permissionHash,
                    "FAILED",
                    OffsetDateTime.now(),
                    error instanceof BusinessException businessException ? businessException.code() : error.getClass().getSimpleName(),
                    error.getMessage(),
                    operator);
            throw error;
        }
    }

    public void repairTenantPermissionSync(Long tenantId, String operator) {
        if (tenantPlanRepository == null || outboxEventRepository == null) {
            throw new BusinessException("TENANT_PERMISSION_SYNC_NOT_CONFIGURED", "租户权限同步未配置");
        }
        Map<String, Object> assignment = latestActiveAssignment(tenantId);
        Long assignmentId = longValue(assignment.get("id"));
        Long planId = longValue(firstNonBlank(assignment, assignment, "plan_id", "planId"));
        if (assignmentId == null || planId == null) {
            throw new BusinessException("TENANT_PLAN_ASSIGNMENT_NOT_FOUND", "租户当前未绑定有效套餐");
        }
        TenantPlan plan = tenantPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("TENANT_PLAN_NOT_FOUND", "租户套餐不存在"));
        appendIamTemplateSyncRequest(
                tenantId,
                planId,
                assignmentId,
                iamInitTemplateCode(plan),
                columnPermissionTemplateCodes(plan),
                defaultColumnPermissionTemplateCode(plan),
                textOrDefault(operator, "system"));
    }

    public void repairPendingPermissionSyncs(int limit) {
        if (tenantPlanRepository == null || outboxEventRepository == null) {
            return;
        }
        resourceService.list(RESOURCE_NAME).stream()
                .filter(this::needsPermissionSyncRepair)
                .sorted(Comparator.comparing(row -> String.valueOf(row.getOrDefault("id", ""))))
                .limit(Math.max(limit, 1))
                .forEach(row -> {
                    Long tenantId = longValue(firstNonBlank(row, row, "tenant_id", "tenantId"));
                    Long planId = longValue(firstNonBlank(row, row, "plan_id", "planId"));
                    Long assignmentId = longValue(row.get("id"));
                    if (tenantId == null || planId == null || assignmentId == null) {
                        return;
                    }
                    tenantPlanRepository.findById(planId).ifPresent(plan -> appendIamTemplateSyncRequest(
                            tenantId,
                            planId,
                            assignmentId,
                            iamInitTemplateCode(plan),
                            columnPermissionTemplateCodes(plan),
                            defaultColumnPermissionTemplateCode(plan),
                            "tenant-permission-sync-scheduler",
                            false));
                });
    }

    private void bootstrapIamTemplateImmediately(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String permissionHash,
            String operator) {
        if (iamBootstrapGateway == null) {
            return;
        }
        try {
            iamBootstrapGateway.bootstrapTenant(
                    tenantId,
                    iamInitTemplateCode,
                    columnPermissionTemplateCodes,
                    defaultColumnPermissionTemplateCode,
                    permissionHash,
                    operator);
        } catch (BusinessException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new BusinessException(
                    "TENANT_IAM_TEMPLATE_SYNC_FAILED",
                    "IAM 权限同步失败，已保留 Outbox 重试事件：" + error.getMessage());
        }
    }

    private Map<String, Object> latestActiveAssignment(Long tenantId) {
        return resourceService.list(RESOURCE_NAME).stream()
                .filter(row -> tenantId.equals(longValue(firstNonBlank(row, row, "tenant_id", "tenantId"))))
                .filter(row -> "ACTIVE".equals(textOrDefault(firstNonBlank(row, row, "status"), "")))
                .max(Comparator.comparing(row -> String.valueOf(row.getOrDefault("assigned_at", row.getOrDefault("assignedAt", "")))))
                .orElseThrow(() -> new BusinessException("TENANT_PLAN_ASSIGNMENT_NOT_FOUND", "租户当前未绑定有效套餐"));
    }

    private boolean needsPermissionSyncRepair(Map<String, Object> row) {
        String status = textOrDefault(firstNonBlank(row, row, "permission_sync_status", "permissionSyncStatus"), "PENDING_REPAIR");
        return "PENDING_REPAIR".equals(status) || "FAILED".equals(status);
    }

    private void updateAssignmentSyncState(
            Long assignmentId,
            String permissionHash,
            String status,
            OffsetDateTime now,
            String errorCode,
            String errorMessage,
            String operator) {
        if (assignmentId == null) {
            return;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("permissionSyncExpectedHash", permissionHash);
        values.put("permissionSyncStatus", status);
        values.put("permissionSyncLastCheckedAt", now);
        if ("SYNCED".equals(status)) {
            values.put("permissionSyncLastSyncedAt", now);
        }
        values.put("permissionSyncLastErrorCode", errorCode);
        values.put("permissionSyncLastErrorMessage", errorMessage == null ? null : abbreviate(errorMessage, 500));
        values.put("updatedBy", textOrDefault(operator, "system"));
        values.put("updatedAt", now);
        resourceService.update(RESOURCE_NAME, assignmentId, values);
    }

    List<String> columnPermissionTemplateCodes(TenantPlan plan) {
        JsonNode value = featureFlags(plan).path("columnPermissionTemplateCodes");
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw new BusinessException("TENANT_PLAN_FEATURE_FLAGS_INVALID", "套餐列权限模板编码必须是 JSON 数组");
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        value.forEach(item -> {
            if (item.isTextual()) {
                String code = textOrNull(item.asText());
                if (code != null) {
                    codes.add(code);
                }
            }
        });
        return List.copyOf(codes);
    }

    String defaultColumnPermissionTemplateCode(TenantPlan plan) {
        JsonNode value = featureFlags(plan).path("defaultColumnPermissionTemplateCode");
        if (!value.isTextual()) {
            return null;
        }
        return textOrNull(value.asText());
    }

    String iamInitTemplateCode(TenantPlan plan) {
        JsonNode value = featureFlags(plan).path("iamInitTemplateCode");
        if (!value.isTextual()) {
            return null;
        }
        return textOrNull(value.asText());
    }

    private JsonNode featureFlags(TenantPlan plan) {
        if (plan.featureFlagsJson() == null || plan.featureFlagsJson().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(plan.featureFlagsJson());
        } catch (JsonProcessingException error) {
            throw new BusinessException("TENANT_PLAN_FEATURE_FLAGS_INVALID", "租户套餐功能标记不是合法 JSON");
        }
    }

    private Object firstNonBlank(Map<String, Object> values, Map<String, Object> fallbackValues, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key) && !isBlank(values.get(key))) {
                return values.get(key);
            }
            if (fallbackValues.containsKey(key) && !isBlank(fallbackValues.get(key))) {
                return fallbackValues.get(key);
            }
        }
        return null;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    private String textOrDefault(Object value, String defaultValue) {
        String text = value == null ? null : textOrNull(String.valueOf(value));
        return text == null ? defaultValue : text;
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize IAM template sync payload", error);
        }
    }
}
