package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.application.query.TenantInternalStatusView;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantInternalStatusResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantRequest;

public final class TenantAssembler {

    private TenantAssembler() {
    }

    public static CreateTenantCommand toCommand(CreateTenantRequest request) {
        return new CreateTenantCommand(
                request.code(),
                request.name(),
                request.contactName(),
                request.contactPhone(),
                request.remark(),
                request.idempotencyKey(),
                request.adminUsername(),
                request.adminPassword(),
                request.adminDisplayName(),
                request.adminEmail(),
                request.adminPhone(),
                request.planId(),
                request.planExpiresAt());
    }

    public static UpdateTenantCommand toCommand(UpdateTenantRequest request) {
        return new UpdateTenantCommand(request.name(), request.contactName(), request.contactPhone(), request.remark(), request.idempotencyKey());
    }

    public static ChangeTenantStatusCommand toCommand(ChangeTenantStatusRequest request) {
        return new ChangeTenantStatusCommand(request.reason(), request.operator(), request.idempotencyKey());
    }

    public static DeleteTenantCommand toCommand(DeleteRequest request) {
        return new DeleteTenantCommand(request.reason(), request.operator(), request.idempotencyKey());
    }

    public static TenantResponse toResponse(TenantDetailView view) {
        return new TenantResponse(
                view.id(),
                view.code(),
                view.name(),
                view.status(),
                view.contactName(),
                view.contactPhone(),
                view.provisionedAt(),
                view.enabledAt(),
                view.remark(),
                view.currentPlanAssignmentId(),
                view.currentPlanId(),
                view.currentPlanCode(),
                view.currentPlanName(),
                view.currentPlanExpiresAt(),
                view.primaryDomainId(),
                view.primaryDomain(),
                view.statusHistoryCount(),
                view.latestStatusChangeType(),
                view.latestStatusChangedAt()
        );
    }

    public static TenantInternalStatusResponse toInternalStatusResponse(TenantInternalStatusView view) {
        return new TenantInternalStatusResponse(
                view.tenantId(),
                view.code(),
                view.status(),
                view.loginAllowed(),
                view.loginDeniedReason(),
                view.currentPlanExpiresAt()
        );
    }
}
