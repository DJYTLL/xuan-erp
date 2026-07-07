package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.application.command.ChangeTenantPlanStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantPlanStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantPlanRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantPlanRequest;

public final class TenantPlanAssembler {

    private TenantPlanAssembler() {
    }

    public static CreateTenantPlanCommand toCommand(CreateTenantPlanRequest request) {
        return new CreateTenantPlanCommand(
                request.code(),
                request.name(),
                request.billingCycle(),
                request.priceAmount(),
                request.currency(),
                request.maxUserCount(),
                request.maxWarehouseCount(),
                request.maxStorageGb(),
                request.featureFlagsJson(),
                request.sortNo(),
                request.remark()
        );
    }

    public static UpdateTenantPlanCommand toCommand(UpdateTenantPlanRequest request) {
        return new UpdateTenantPlanCommand(
                request.name(),
                request.billingCycle(),
                request.priceAmount(),
                request.currency(),
                request.maxUserCount(),
                request.maxWarehouseCount(),
                request.maxStorageGb(),
                request.featureFlagsJson(),
                request.sortNo(),
                request.remark()
        );
    }

    public static ChangeTenantPlanStatusCommand toCommand(ChangeTenantPlanStatusRequest request) {
        return new ChangeTenantPlanStatusCommand(request.reason(), request.operator());
    }

    public static TenantPlanResponse toResponse(TenantPlanDetailView view) {
        return new TenantPlanResponse(
                view.id(),
                view.code(),
                view.name(),
                view.status(),
                view.billingCycle(),
                view.priceAmount(),
                view.currency(),
                view.maxUserCount(),
                view.maxWarehouseCount(),
                view.maxStorageGb(),
                view.featureFlagsJson(),
                view.sortNo(),
                view.remark()
        );
    }
}
