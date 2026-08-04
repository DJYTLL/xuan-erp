package com.xuan.erp.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.TenantAdminBootstrapCommand;
import com.xuan.erp.tenant.application.command.TenantProvisionCallbackCommand;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskStepView;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskStepRepository;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 租户初始化编排应用服务，当前仅负责编排总任务与 IAM 引导步骤的最小落点。
 */
@Service
public class TenantProvisionApplicationService {

    private static final String TASK_TYPE = "TENANT_PROVISION";
    private static final String IAM_BOOTSTRAP_STEP_KEY = "IAM_BOOTSTRAP";
    private static final String TENANT_EVENT_TOPIC = "xuan-tenant-event";
    private static final String AGGREGATE_TYPE = "TENANT_PROVISION_TASK";
    private static final String SOURCE_SERVICE = "xuan-tenant";
    private static final String EVENT_PROVISIONING_STARTED = "TenantProvisioningStarted";
    private static final String EVENT_IAM_BOOTSTRAP_REQUESTED = "TenantIamBootstrapRequested";
    private static final String EVENT_IAM_STEP_COMPLETED = "TenantIamProvisionStepCompleted";
    private static final String EVENT_IAM_STEP_FAILED = "TenantIamProvisionStepFailed";
    private static final String EVENT_TENANT_PROVISIONED = "TenantProvisioned";

    private final TenantProvisionTaskRepository taskRepository;
    private final TenantProvisionTaskStepRepository taskStepRepository;
    private final TenantOutboxEventRepository outboxEventRepository;
    private final TenantRepository tenantRepository;
    private final TenantStatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantProvisionApplicationService(
            TenantProvisionTaskRepository taskRepository,
            TenantProvisionTaskStepRepository taskStepRepository,
            TenantOutboxEventRepository outboxEventRepository) {
        this(taskRepository, taskStepRepository, outboxEventRepository, null, null);
    }

    @Autowired
    public TenantProvisionApplicationService(
            TenantProvisionTaskRepository taskRepository,
            TenantProvisionTaskStepRepository taskStepRepository,
            TenantOutboxEventRepository outboxEventRepository,
            @Nullable TenantRepository tenantRepository,
            @Nullable TenantStatusHistoryRepository statusHistoryRepository) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.tenantRepository = tenantRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public TenantProvisionTaskView startProvisioning(Long tenantId, String taskKey, String idempotencyKey, String requestedBy) {
        return startProvisioning(tenantId, taskKey, idempotencyKey, requestedBy, null);
    }

    public TenantProvisionTaskView startProvisioning(
            Long tenantId,
            String taskKey,
            String idempotencyKey,
            String requestedBy,
            TenantAdminBootstrapCommand adminBootstrapCommand) {
        OffsetDateTime now = OffsetDateTime.now();
        String operator = requestedBy == null || requestedBy.isBlank() ? "system" : requestedBy.trim();
        String normalizedIdempotencyKey =
                idempotencyKey == null || idempotencyKey.isBlank() ? taskKey : idempotencyKey.trim();

        TenantProvisionTask task = taskRepository.save(new TenantProvisionTask(
                null,
                tenantId,
                taskKey,
                TASK_TYPE,
                ProvisionTaskStatus.PENDING,
                normalizedIdempotencyKey,
                TASK_TYPE,
                provisioningPayload(tenantId, adminBootstrapCommand),
                "{}",
                0,
                5,
                null,
                null,
                now,
                null,
                operator,
                now,
                operator,
                now,
                null,
                null,
                null
        ));

        TenantProvisionTaskStep step = taskStepRepository.save(new TenantProvisionTaskStep(
                null,
                tenantId,
                task.id(),
                IAM_BOOTSTRAP_STEP_KEY,
                IAM_BOOTSTRAP_STEP_KEY,
                ProvisionTaskStepStatus.PENDING,
                1,
                normalizedIdempotencyKey,
                provisioningPayload(tenantId, adminBootstrapCommand),
                "{}",
                0,
                3,
                null,
                null,
                null,
                null,
                operator,
                now,
                operator,
                now,
                null,
                null,
                null
        ));

        appendProvisioningStartedEvent(task, normalizedIdempotencyKey, operator, now);
        appendIamBootstrapRequestedEvent(task, step, adminBootstrapCommand, operator, now);

        return new TenantProvisionTaskView(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                task.status(),
                task.lastErrorCode(),
                task.lastErrorMessage(),
                List.of(toStepView(step))
        );
    }

    public List<TenantProvisionTaskView> listTasks(Long tenantId) {
        return taskRepository.findActiveByTenantId(tenantId).stream()
                .map(task -> new TenantProvisionTaskView(
                        task.id(),
                        task.tenantId(),
                        task.taskKey(),
                        task.taskType(),
                        task.status(),
                        task.lastErrorCode(),
                        task.lastErrorMessage(),
                        taskStepRepository.findByTaskId(task.id()).stream().map(this::toStepView).toList()
                ))
                .toList();
    }

    public void handleProvisionCallback(Long tenantId, TenantProvisionCallbackCommand command) {
        TenantProvisionTask task = requireCallbackTask(tenantId, command);
        TenantProvisionTaskStep step = requireStep(task.id(), requireText(command.provisionStep(), "初始化步骤不能为空"));
        if (!IAM_BOOTSTRAP_STEP_KEY.equals(step.stepKey())) {
            throw new BusinessException("TENANT_PROVISION_STEP_UNSUPPORTED", "当前仅支持 IAM_BOOTSTRAP 初始化步骤");
        }

        if (command.success()) {
            completeIamBootstrapStep(task, step, command);
            return;
        }

        failIamBootstrapStep(task, step, command);
    }

    public void retryTask(Long taskId, String stepKey, String operator, String reason) {
        requireText(reason, "重试原因不能为空");
        String normalizedOperator = operator(operator);
        OffsetDateTime now = OffsetDateTime.now();
        TenantProvisionTask task = requireTask(taskId);
        TenantProvisionTaskStep step = requireStep(taskId, stepKey);

        TenantProvisionTask retriedTask = taskRepository.save(new TenantProvisionTask(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                ProvisionTaskStatus.RUNNING,
                task.idempotencyKey(),
                step.stepName(),
                task.requestPayloadJson(),
                task.resultPayloadJson(),
                task.retryCount(),
                task.maxRetryCount(),
                null,
                null,
                task.startedAt() == null ? now : task.startedAt(),
                null,
                task.createdBy(),
                task.createdAt(),
                normalizedOperator,
                now,
                task.deletedBy(),
                task.deleteReason(),
                task.deletedAt()
        ));

        TenantProvisionTaskStep retriedStep = taskStepRepository.save(new TenantProvisionTaskStep(
                step.id(),
                step.tenantId(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                ProvisionTaskStepStatus.RUNNING,
                step.sequenceNo(),
                step.idempotencyKey(),
                step.requestPayloadJson(),
                step.resultPayloadJson(),
                step.retryCount(),
                step.maxRetryCount(),
                null,
                null,
                now,
                null,
                step.createdBy(),
                step.createdAt(),
                normalizedOperator,
                now,
                step.deletedBy(),
                step.deleteReason(),
                step.deletedAt()
        ));

        if (IAM_BOOTSTRAP_STEP_KEY.equals(retriedStep.stepKey())) {
            appendIamBootstrapRequestedEvent(
                    retriedTask,
                    retriedStep,
                    adminBootstrapCommandFromStepRequest(retriedStep),
                    normalizedOperator,
                    now);
        }
    }

    public void retryOutboxEvent(Long eventId, String operator, String reason) {
        requireText(reason, "回放原因不能为空");
        String normalizedOperator = operator(operator);
        OffsetDateTime now = OffsetDateTime.now();
        TenantOutboxEvent event = requireOutboxEvent(eventId);

        outboxEventRepository.save(new TenantOutboxEvent(
                event.id(),
                event.tenantId(),
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.topic(),
                event.payloadJson(),
                event.headersJson(),
                OutboxEventStatus.PENDING,
                event.retryCount(),
                event.maxRetryCount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                event.firstFailedAt(),
                null,
                event.createdBy(),
                event.createdAt(),
                normalizedOperator,
                now
        ));
    }

    private TenantProvisionTask requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TENANT_PROVISION_TASK_NOT_FOUND", "初始化任务不存在"));
    }

    private TenantProvisionTaskStep requireStep(Long taskId, String stepKey) {
        String normalizedStepKey = requireText(stepKey, "步骤键不能为空");
        return taskStepRepository.findActiveByTaskIdAndStepKey(taskId, normalizedStepKey)
                .orElseThrow(() -> new BusinessException("TENANT_PROVISION_STEP_NOT_FOUND", "初始化步骤不存在"));
    }

    private TenantOutboxEvent requireOutboxEvent(Long eventId) {
        return outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("TENANT_OUTBOX_EVENT_NOT_FOUND", "Outbox 事件不存在"));
    }

    private TenantProvisionTask requireCallbackTask(Long tenantId, TenantProvisionCallbackCommand command) {
        String taskKey = textOrNull(command.taskKey());
        String idempotencyKey = textOrNull(command.idempotencyKey());
        if (taskKey == null && idempotencyKey == null) {
            throw new BusinessException("TENANT_PROVISION_CALLBACK_MISSING_TASK_KEY", "初始化回执必须带任务键或幂等键");
        }
        if (taskKey != null) {
            return taskRepository.findActiveByTenantIdAndTaskKey(tenantId, taskKey)
                    .orElseThrow(() -> new BusinessException("TENANT_PROVISION_TASK_NOT_FOUND", "初始化任务不存在"));
        }
        return taskRepository.findActiveByTenantId(tenantId).stream()
                .filter(task -> idempotencyKey.equals(task.idempotencyKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TENANT_PROVISION_TASK_NOT_FOUND", "初始化任务不存在"));
    }

    private void completeIamBootstrapStep(TenantProvisionTask task, TenantProvisionTaskStep step, TenantProvisionCallbackCommand command) {
        requireCallbackEventType(command.eventType(), EVENT_IAM_STEP_COMPLETED);
        if (task.status() == ProvisionTaskStatus.SUCCEEDED && step.status() == ProvisionTaskStepStatus.SUCCEEDED) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        String operator = operator(command.operator());
        String resultJson = callbackResultPayload(command);

        taskStepRepository.save(new TenantProvisionTaskStep(
                step.id(),
                step.tenantId(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                ProvisionTaskStepStatus.SUCCEEDED,
                step.sequenceNo(),
                step.idempotencyKey(),
                step.requestPayloadJson(),
                resultJson,
                step.retryCount(),
                step.maxRetryCount(),
                null,
                null,
                step.startedAt() == null ? now : step.startedAt(),
                now,
                step.createdBy(),
                step.createdAt(),
                operator,
                now,
                step.deletedBy(),
                step.deleteReason(),
                step.deletedAt()
        ));

        TenantProvisionTask savedTask = taskRepository.save(new TenantProvisionTask(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                ProvisionTaskStatus.SUCCEEDED,
                task.idempotencyKey(),
                step.stepName(),
                task.requestPayloadJson(),
                resultJson,
                task.retryCount(),
                task.maxRetryCount(),
                null,
                null,
                task.startedAt() == null ? now : task.startedAt(),
                now,
                task.createdBy(),
                task.createdAt(),
                operator,
                now,
                task.deletedBy(),
                task.deleteReason(),
                task.deletedAt()
        ));

        markTenantProvisioned(savedTask.tenantId(), operator, now);
        appendTenantProvisionedEvent(savedTask, command.eventId(), operator, now);
    }

    private void failIamBootstrapStep(TenantProvisionTask task, TenantProvisionTaskStep step, TenantProvisionCallbackCommand command) {
        requireCallbackEventType(command.eventType(), EVENT_IAM_STEP_FAILED);
        if (task.status() == ProvisionTaskStatus.SUCCEEDED || step.status() == ProvisionTaskStepStatus.SUCCEEDED) {
            return;
        }
        if (task.status() == ProvisionTaskStatus.FAILED
                && step.status() == ProvisionTaskStepStatus.FAILED
                && hasProcessedCallback(step, command.eventId())) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        String operator = operator(command.operator());
        String errorCode = requireText(command.errorCode(), "失败错误码不能为空");
        String errorMessage = requireText(command.errorMessage(), "失败错误信息不能为空");
        String resultJson = callbackResultPayload(command);

        taskStepRepository.save(new TenantProvisionTaskStep(
                step.id(),
                step.tenantId(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                ProvisionTaskStepStatus.FAILED,
                step.sequenceNo(),
                step.idempotencyKey(),
                step.requestPayloadJson(),
                resultJson,
                step.retryCount() + 1,
                step.maxRetryCount(),
                errorCode,
                errorMessage,
                step.startedAt() == null ? now : step.startedAt(),
                now,
                step.createdBy(),
                step.createdAt(),
                operator,
                now,
                step.deletedBy(),
                step.deleteReason(),
                step.deletedAt()
        ));

        taskRepository.save(new TenantProvisionTask(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                ProvisionTaskStatus.FAILED,
                task.idempotencyKey(),
                step.stepName(),
                task.requestPayloadJson(),
                resultJson,
                task.retryCount() + 1,
                task.maxRetryCount(),
                errorCode,
                errorMessage,
                task.startedAt() == null ? now : task.startedAt(),
                now,
                task.createdBy(),
                task.createdAt(),
                operator,
                now,
                task.deletedBy(),
                task.deleteReason(),
                task.deletedAt()
        ));
    }

    private void markTenantProvisioned(Long tenantId, String operator, OffsetDateTime now) {
        if (tenantRepository == null || statusHistoryRepository == null) {
            return;
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在"));
        if (tenant.status() == TenantStatus.PROVISIONED || tenant.status() == TenantStatus.ENABLED) {
            return;
        }
        Tenant provisioned = tenantRepository.save(tenant.markProvisioned("IAM_BOOTSTRAP 初始化完成", operator, now));
        statusHistoryRepository.append(new TenantStatusHistory(
                null,
                provisioned.id(),
                tenant.status(),
                provisioned.status(),
                "PROVISION",
                "IAM_BOOTSTRAP 初始化完成",
                now,
                operator,
                null,
                null,
                "EVENT",
                operator,
                now
        ));
    }

    private void appendProvisioningStartedEvent(TenantProvisionTask task, String idempotencyKey, String operator, OffsetDateTime now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", task.tenantId());
        payload.put("taskId", task.id());
        payload.put("taskKey", task.taskKey());
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("provisionStep", IAM_BOOTSTRAP_STEP_KEY);
        payload.put("occurredAt", now.toString());
        payload.put("sourceService", SOURCE_SERVICE);
        appendOutboxEvent(task, EVENT_PROVISIONING_STARTED, payload, operator, now);
    }

    private void appendIamBootstrapRequestedEvent(
            TenantProvisionTask task,
            TenantProvisionTaskStep step,
            TenantAdminBootstrapCommand adminBootstrapCommand,
            String operator,
            OffsetDateTime now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", task.tenantId());
        payload.put("taskId", task.id());
        payload.put("taskKey", task.taskKey());
        payload.put("stepId", step.id());
        payload.put("provisionStep", step.stepKey());
        payload.put("idempotencyKey", step.idempotencyKey());
        if (adminBootstrapCommand != null) {
            payload.put("adminUsername", adminBootstrapCommand.adminUsername());
            payload.put("adminDisplayName", adminBootstrapCommand.adminDisplayName());
            if (adminBootstrapCommand.adminEmail() != null) {
                payload.put("adminEmail", adminBootstrapCommand.adminEmail());
            }
            if (adminBootstrapCommand.adminPhone() != null) {
                payload.put("adminPhone", adminBootstrapCommand.adminPhone());
            }
            if (adminBootstrapCommand.iamInitTemplateCode() != null) {
                payload.put("iamInitTemplateCode", adminBootstrapCommand.iamInitTemplateCode());
            }
            if (adminBootstrapCommand.columnPermissionTemplateCodes() != null) {
                payload.put("columnPermissionTemplateCodes", adminBootstrapCommand.columnPermissionTemplateCodes());
            }
            if (adminBootstrapCommand.defaultColumnPermissionTemplateCode() != null) {
                payload.put("defaultColumnPermissionTemplateCode", adminBootstrapCommand.defaultColumnPermissionTemplateCode());
            }
            payload.put("permissionHash", TenantPermissionSyncFingerprint.hash(
                    adminBootstrapCommand.iamInitTemplateCode(),
                    adminBootstrapCommand.columnPermissionTemplateCodes(),
                    adminBootstrapCommand.defaultColumnPermissionTemplateCode()));
        }
        payload.put("occurredAt", now.toString());
        payload.put("sourceService", SOURCE_SERVICE);
        appendOutboxEvent(task, EVENT_IAM_BOOTSTRAP_REQUESTED, payload, operator, now);
    }

    private void appendTenantProvisionedEvent(TenantProvisionTask task, String callbackEventId, String operator, OffsetDateTime now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", task.tenantId());
        payload.put("taskId", task.id());
        payload.put("taskKey", task.taskKey());
        payload.put("provisionStep", IAM_BOOTSTRAP_STEP_KEY);
        payload.put("callbackEventId", callbackEventId);
        payload.put("occurredAt", now.toString());
        payload.put("sourceService", SOURCE_SERVICE);
        appendOutboxEvent(task, EVENT_TENANT_PROVISIONED, payload, operator, now);
    }

    private void appendOutboxEvent(
            TenantProvisionTask task,
            String eventType,
            Map<String, Object> payload,
            String operator,
            OffsetDateTime now) {
        String eventId = UUID.randomUUID().toString();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        outboxEventRepository.append(new TenantOutboxEvent(
                null,
                task.tenantId(),
                eventId,
                AGGREGATE_TYPE,
                task.id(),
                eventType,
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
                now
        ));
    }

    private TenantProvisionTaskStepView toStepView(TenantProvisionTaskStep step) {
        return new TenantProvisionTaskStepView(
                step.id(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                step.status(),
                step.sequenceNo(),
                step.lastErrorCode(),
                step.lastErrorMessage()
        );
    }

    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TENANT_PROVISION_INVALID_REQUEST", message);
        }
        return value.trim();
    }

    private void requireCallbackEventType(String actualEventType, String expectedEventType) {
        String eventType = requireText(actualEventType, "回执事件类型不能为空");
        if (!expectedEventType.equals(eventType)) {
            throw new BusinessException("TENANT_PROVISION_CALLBACK_TYPE_MISMATCH", "初始化回执事件类型不匹配");
        }
    }

    private boolean hasProcessedCallback(TenantProvisionTaskStep step, String eventId) {
        String normalizedEventId = textOrNull(eventId);
        return normalizedEventId != null
                && step.resultPayloadJson() != null
                && step.resultPayloadJson().contains("\"eventId\":\"" + normalizedEventId + "\"");
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String textOrNull(Object value) {
        return value == null ? null : textOrNull(String.valueOf(value));
    }

    private TenantAdminBootstrapCommand adminBootstrapCommandFromStepRequest(TenantProvisionTaskStep step) {
        Map<String, Object> payload = jsonObjectOrEmpty(step.requestPayloadJson());
        String adminUsername = textOrNull(payload.get("adminUsername"));
        String adminPasswordHash = textOrNull(payload.get("adminPasswordHash"));
        String adminDisplayName = textOrNull(payload.get("adminDisplayName"));
        String adminEmail = textOrNull(payload.get("adminEmail"));
        String adminPhone = textOrNull(payload.get("adminPhone"));
        String iamInitTemplateCode = textOrNull(payload.get("iamInitTemplateCode"));
        List<String> columnPermissionTemplateCodes = stringList(payload.get("columnPermissionTemplateCodes"));
        String defaultColumnPermissionTemplateCode = textOrNull(payload.get("defaultColumnPermissionTemplateCode"));
        if (adminUsername == null && adminPasswordHash == null && adminDisplayName == null
                && adminEmail == null && adminPhone == null && iamInitTemplateCode == null
                && columnPermissionTemplateCodes == null && defaultColumnPermissionTemplateCode == null) {
            return null;
        }
        return new TenantAdminBootstrapCommand(
                adminUsername,
                adminPasswordHash,
                adminDisplayName,
                adminEmail,
                adminPhone,
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode
        );
    }

    private Map<String, Object> jsonObjectOrEmpty(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to parse tenant provisioning request payload", error);
        }
    }

    private String callbackResultPayload(TenantProvisionCallbackCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", requireText(command.eventId(), "回执事件 ID 不能为空"));
        payload.put("eventType", requireText(command.eventType(), "回执事件类型不能为空"));
        payload.put("provisionStep", requireText(command.provisionStep(), "初始化步骤不能为空"));
        payload.put("success", command.success());
        if (command.errorCode() != null) {
            payload.put("errorCode", command.errorCode());
        }
        if (command.errorMessage() != null) {
            payload.put("errorMessage", command.errorMessage());
        }
        if (command.resultPayload() != null && !command.resultPayload().isEmpty()) {
            payload.put("result", command.resultPayload());
        }
        return toJson(payload);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize tenant provisioning payload", error);
        }
    }

    private String provisioningPayload(Long tenantId, TenantAdminBootstrapCommand adminBootstrapCommand) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        if (adminBootstrapCommand != null) {
            payload.put("adminUsername", adminBootstrapCommand.adminUsername());
            payload.put("adminPasswordHash", adminBootstrapCommand.adminPasswordHash());
            payload.put("adminDisplayName", adminBootstrapCommand.adminDisplayName());
            if (adminBootstrapCommand.adminEmail() != null) {
                payload.put("adminEmail", adminBootstrapCommand.adminEmail());
            }
            if (adminBootstrapCommand.adminPhone() != null) {
                payload.put("adminPhone", adminBootstrapCommand.adminPhone());
            }
            if (adminBootstrapCommand.iamInitTemplateCode() != null) {
                payload.put("iamInitTemplateCode", adminBootstrapCommand.iamInitTemplateCode());
            }
            if (adminBootstrapCommand.columnPermissionTemplateCodes() != null) {
                payload.put("columnPermissionTemplateCodes", adminBootstrapCommand.columnPermissionTemplateCodes());
            }
            if (adminBootstrapCommand.defaultColumnPermissionTemplateCode() != null) {
                payload.put("defaultColumnPermissionTemplateCode", adminBootstrapCommand.defaultColumnPermissionTemplateCode());
            }
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize tenant provisioning payload", error);
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> items)) {
            return null;
        }
        return items.stream()
                .map(this::textOrNull)
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }
}
