package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantRequest;

public final class TenantAssembler {

    private TenantAssembler() {
    }

    public static CreateTenantCommand toCommand(CreateTenantRequest request) {
        return new CreateTenantCommand(request.code(), request.name(), request.contactName(), request.contactPhone(), request.remark());
    }

    public static UpdateTenantCommand toCommand(UpdateTenantRequest request) {
        return new UpdateTenantCommand(request.name(), request.contactName(), request.contactPhone(), request.remark());
    }

    public static ChangeTenantStatusCommand toCommand(ChangeTenantStatusRequest request) {
        return new ChangeTenantStatusCommand(request.reason(), request.operator());
    }

    public static DeleteTenantCommand toCommand(DeleteRequest request) {
        return new DeleteTenantCommand(request.reason(), request.operator());
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
                view.remark()
        );
    }
}
