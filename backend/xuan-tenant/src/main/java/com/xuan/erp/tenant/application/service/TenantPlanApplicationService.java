package com.xuan.erp.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantPlanStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantPlanAssignmentRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
/**
 * 租户套餐应用服务，负责套餐的查询、创建、更新、启停用与删除。
 */
@Service
public class TenantPlanApplicationService {

    private final TenantPlanRepository tenantPlanRepository;
    private final TenantPlanAssignmentRepository tenantPlanAssignmentRepository;
    private final TenantPlanAssignmentApplicationService tenantPlanAssignmentApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 注入租户套餐仓储。
     */
    public TenantPlanApplicationService(TenantPlanRepository tenantPlanRepository) {
        this(tenantPlanRepository, null, null);
    }

    @Autowired
    public TenantPlanApplicationService(
            TenantPlanRepository tenantPlanRepository,
            @Nullable TenantPlanAssignmentRepository tenantPlanAssignmentRepository,
            @Nullable TenantPlanAssignmentApplicationService tenantPlanAssignmentApplicationService) {
        this.tenantPlanRepository = tenantPlanRepository;
        this.tenantPlanAssignmentRepository = tenantPlanAssignmentRepository;
        this.tenantPlanAssignmentApplicationService = tenantPlanAssignmentApplicationService;
    }

    /**
     * 查询全部有效套餐，并转换为详情视图返回。
     */
    public List<TenantPlanDetailView> listPlans() {
        return tenantPlanRepository.findActivePlans().stream()
                .map(this::toDetailView)
                .toList();
    }

    /**
     * 按套餐 ID 查询套餐详情。
     */
    public TenantPlanDetailView getPlan(Long planId) {
        return toDetailView(requirePlan(planId));
    }

    /**
     * 查询当前使用指定 IAM 初始化模板编码的租户 ID。
     */
    public List<Long> findActiveTenantIdsByIamInitTemplateCode(String iamInitTemplateCode) {
        if (tenantPlanAssignmentRepository == null) {
            return List.of();
        }
        String templateCode = requireText(iamInitTemplateCode, "IAM 初始化模板编码不能为空");
        LinkedHashSet<Long> tenantIds = new LinkedHashSet<>();
        for (TenantPlan plan : tenantPlanRepository.findActivePlans()) {
            if (!templateCode.equals(iamInitTemplateCode(plan))) {
                continue;
            }
            for (TenantPlanAssignment assignment : tenantPlanAssignmentRepository.findActiveByPlanId(plan.id())) {
                if (assignment.tenantId() != null && assignment.tenantId() > 0) {
                    tenantIds.add(assignment.tenantId());
                }
            }
        }
        return tenantIds.stream().sorted().toList();
    }

    /**
     * 创建新套餐，并补齐默认计费周期、价格、币种和功能标记等默认值。
     */
    public TenantPlanDetailView createPlan(CreateTenantPlanCommand command) {
        String code = requireText(command.code(), "套餐编码不能为空").toLowerCase();
        tenantPlanRepository.findActiveByCode(code)
                .ifPresent(existing -> {
                    throw new BusinessException("TENANT_PLAN_CODE_EXISTS", "套餐编码已存在");
                });
        OffsetDateTime now = OffsetDateTime.now();
        TenantPlan saved = tenantPlanRepository.save(new TenantPlan(
                null,
                code,
                requireText(command.name(), "套餐名称不能为空"),
                TenantPlanStatus.ENABLED,
                command.billingCycle() == null ? BillingCycle.MONTHLY : command.billingCycle(),
                command.priceAmount() == null ? BigDecimal.ZERO : command.priceAmount(),
                defaultText(command.currency(), "CNY"),
                command.maxUserCount(),
                command.maxWarehouseCount(),
                command.maxStorageGb(),
                defaultText(command.featureFlagsJson(), "{}"),
                command.sortNo() == null ? 0 : command.sortNo(),
                command.remark(),
                "system",
                now,
                "system",
                now,
                null,
                null,
                null
        ));
        return toDetailView(saved);
    }

    /**
     * 更新套餐基础信息，未传入的可选字段继续沿用原值。
     */
    public TenantPlanDetailView updatePlan(Long planId, UpdateTenantPlanCommand command) {
        TenantPlan plan = requirePlan(planId);
        OffsetDateTime now = OffsetDateTime.now();
        TenantPlan saved = tenantPlanRepository.save(new TenantPlan(
                plan.id(),
                plan.code(),
                requireText(command.name(), "套餐名称不能为空"),
                plan.status(),
                command.billingCycle() == null ? plan.billingCycle() : command.billingCycle(),
                command.priceAmount() == null ? plan.priceAmount() : command.priceAmount(),
                defaultText(command.currency(), plan.currency()),
                command.maxUserCount(),
                command.maxWarehouseCount(),
                command.maxStorageGb(),
                defaultText(command.featureFlagsJson(), plan.featureFlagsJson()),
                command.sortNo() == null ? plan.sortNo() : command.sortNo(),
                command.remark(),
                plan.createdBy(),
                plan.createdAt(),
                "system",
                now,
                plan.deletedBy(),
                plan.deleteReason(),
                plan.deletedAt()
        ));
        publishIamTemplateSyncForActiveAssignments(saved);
        return toDetailView(saved);
    }

    /**
     * 将指定套餐启用。
     */
    public TenantPlanDetailView enablePlan(Long planId, ChangeTenantPlanStatusCommand command) {
        TenantPlan plan = requirePlan(planId);
        return changeStatus(plan, TenantPlanStatus.ENABLED, command);
    }

    /**
     * 将指定套餐停用，并要求必须提供停用原因。
     */
    public TenantPlanDetailView disablePlan(Long planId, ChangeTenantPlanStatusCommand command) {
        requireText(command.reason(), "停用原因不能为空");
        TenantPlan plan = requirePlan(planId);
        return changeStatus(plan, TenantPlanStatus.DISABLED, command);
    }

    /**
     * 逻辑删除套餐，写入删除原因、删除人和删除时间。
     */
    public void deletePlan(Long planId, DeleteTenantCommand command) {
        TenantPlan plan = requirePlan(planId);
        String reason = requireText(command.reason(), "删除原因不能为空");
        OffsetDateTime now = OffsetDateTime.now();
        tenantPlanRepository.save(new TenantPlan(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.status(),
                plan.billingCycle(),
                plan.priceAmount(),
                plan.currency(),
                plan.maxUserCount(),
                plan.maxWarehouseCount(),
                plan.maxStorageGb(),
                plan.featureFlagsJson(),
                plan.sortNo(),
                plan.remark(),
                plan.createdBy(),
                plan.createdAt(),
                operator(command.operator()),
                now,
                operator(command.operator()),
                reason,
                now
        ));
    }

    /**
     * 统一处理套餐状态切换，并刷新修改审计字段。
     */
    private TenantPlanDetailView changeStatus(TenantPlan plan, TenantPlanStatus status, ChangeTenantPlanStatusCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        TenantPlan saved = tenantPlanRepository.save(new TenantPlan(
                plan.id(),
                plan.code(),
                plan.name(),
                status,
                plan.billingCycle(),
                plan.priceAmount(),
                plan.currency(),
                plan.maxUserCount(),
                plan.maxWarehouseCount(),
                plan.maxStorageGb(),
                plan.featureFlagsJson(),
                plan.sortNo(),
                plan.remark(),
                plan.createdBy(),
                plan.createdAt(),
                operator(command.operator()),
                now,
                plan.deletedBy(),
                plan.deleteReason(),
                plan.deletedAt()
        ));
        return toDetailView(saved);
    }

    /**
     * 确保套餐存在；若不存在则抛出套餐不存在异常。
     */
    private TenantPlan requirePlan(Long planId) {
        return tenantPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("TENANT_PLAN_NOT_FOUND", "租户套餐不存在"));
    }

    /**
     * 将套餐领域对象转换为应用层详情视图。
     */
    private TenantPlanDetailView toDetailView(TenantPlan plan) {
        return new TenantPlanDetailView(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.status(),
                plan.billingCycle(),
                plan.priceAmount(),
                plan.currency(),
                plan.maxUserCount(),
                plan.maxWarehouseCount(),
                plan.maxStorageGb(),
                plan.featureFlagsJson(),
                plan.sortNo(),
                plan.remark()
        );
    }

    private void publishIamTemplateSyncForActiveAssignments(TenantPlan plan) {
        if (tenantPlanAssignmentRepository == null || tenantPlanAssignmentApplicationService == null) {
            return;
        }
        String iamInitTemplateCode = tenantPlanAssignmentApplicationService.iamInitTemplateCode(plan);
        List<String> columnPermissionTemplateCodes = tenantPlanAssignmentApplicationService.columnPermissionTemplateCodes(plan);
        String defaultColumnPermissionTemplateCode = tenantPlanAssignmentApplicationService.defaultColumnPermissionTemplateCode(plan);
        if (iamInitTemplateCode == null && columnPermissionTemplateCodes == null) {
            return;
        }
        for (TenantPlanAssignment assignment : tenantPlanAssignmentRepository.findActiveByPlanId(plan.id())) {
            tenantPlanAssignmentApplicationService.appendIamTemplateSyncRequest(
                    assignment.tenantId(),
                    plan.id(),
                    assignment.id(),
                    iamInitTemplateCode,
                    columnPermissionTemplateCodes,
                    defaultColumnPermissionTemplateCode,
                    "system");
        }
    }

    private String iamInitTemplateCode(TenantPlan plan) {
        if (tenantPlanAssignmentApplicationService != null) {
            return tenantPlanAssignmentApplicationService.iamInitTemplateCode(plan);
        }
        if (plan.featureFlagsJson() == null || plan.featureFlagsJson().isBlank()) {
            return null;
        }
        try {
            JsonNode value = objectMapper.readTree(plan.featureFlagsJson()).path("iamInitTemplateCode");
            if (!value.isTextual()) {
                return null;
            }
            String text = value.asText();
            return text == null || text.isBlank() ? null : text.trim();
        } catch (JsonProcessingException error) {
            throw new BusinessException("TENANT_PLAN_FEATURE_FLAGS_INVALID", "租户套餐功能标记不是合法 JSON");
        }
    }

    /**
     * 校验必填文本字段；为空时抛出业务异常。
     */
    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TENANT_PLAN_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    /**
     * 为可选文本字段提供默认值，并去除首尾空白。
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 规范化操作人字段，未传值时统一使用 system。
     */
    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
