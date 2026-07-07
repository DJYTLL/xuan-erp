package com.xuan.erp.tenant.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantPlanStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TenantPlanApplicationService {

    private final TenantPlanRepository tenantPlanRepository;

    public TenantPlanApplicationService(TenantPlanRepository tenantPlanRepository) {
        this.tenantPlanRepository = tenantPlanRepository;
    }

    public List<TenantPlanDetailView> listPlans() {
        return tenantPlanRepository.findActivePlans().stream()
                .map(this::toDetailView)
                .toList();
    }

    public TenantPlanDetailView getPlan(Long planId) {
        return toDetailView(requirePlan(planId));
    }

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
        return toDetailView(saved);
    }

    public TenantPlanDetailView enablePlan(Long planId, ChangeTenantPlanStatusCommand command) {
        TenantPlan plan = requirePlan(planId);
        return changeStatus(plan, TenantPlanStatus.ENABLED, command);
    }

    public TenantPlanDetailView disablePlan(Long planId, ChangeTenantPlanStatusCommand command) {
        requireText(command.reason(), "停用原因不能为空");
        TenantPlan plan = requirePlan(planId);
        return changeStatus(plan, TenantPlanStatus.DISABLED, command);
    }

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

    private TenantPlan requirePlan(Long planId) {
        return tenantPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("TENANT_PLAN_NOT_FOUND", "租户套餐不存在"));
    }

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

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TENANT_PLAN_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }
}
