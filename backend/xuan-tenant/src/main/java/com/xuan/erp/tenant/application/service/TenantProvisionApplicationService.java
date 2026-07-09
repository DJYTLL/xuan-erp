package com.xuan.erp.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.TenantAdminBootstrapCommand;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskStepView;
import com.xuan.erp.tenant.application.query.TenantProvisionTaskView;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskStepRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 租户初始化编排应用服务，当前仅负责编排总任务与 IAM 引导步骤的最小落点。
 */
@Service
public class TenantProvisionApplicationService {

    private static final String TASK_TYPE = "TENANT_PROVISION";
    private static final String IAM_BOOTSTRAP_STEP_KEY = "IAM_BOOTSTRAP";

    private final TenantProvisionTaskRepository taskRepository;
    private final TenantProvisionTaskStepRepository taskStepRepository;
    @SuppressWarnings("unused")
    private final TenantOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantProvisionApplicationService(
            TenantProvisionTaskRepository taskRepository,
            TenantProvisionTaskStepRepository taskStepRepository,
            TenantOutboxEventRepository outboxEventRepository) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.outboxEventRepository = outboxEventRepository;
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

        return new TenantProvisionTaskView(
                task.id(),
                task.tenantId(),
                task.taskKey(),
                task.taskType(),
                task.status(),
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
                        taskStepRepository.findByTaskId(task.id()).stream().map(this::toStepView).toList()
                ))
                .toList();
    }

    public void retryTask(Long taskId, String stepKey, String operator, String reason) {
        requireText(reason, "重试原因不能为空");
        String normalizedOperator = operator(operator);
        OffsetDateTime now = OffsetDateTime.now();
        TenantProvisionTask task = requireTask(taskId);
        TenantProvisionTaskStep step = requireStep(taskId, stepKey);

        taskRepository.save(new TenantProvisionTask(
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

        taskStepRepository.save(new TenantProvisionTaskStep(
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

    private TenantProvisionTaskStepView toStepView(TenantProvisionTaskStep step) {
        return new TenantProvisionTaskStepView(
                step.id(),
                step.provisionTaskId(),
                step.stepKey(),
                step.stepName(),
                step.status(),
                step.sequenceNo()
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
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize tenant provisioning payload", error);
        }
    }
}
