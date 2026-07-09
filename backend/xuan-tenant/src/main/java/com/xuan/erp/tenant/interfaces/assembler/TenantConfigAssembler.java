package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.application.command.CreateTenantConfigCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantConfigCommand;
import com.xuan.erp.tenant.application.query.TenantConfigDetailView;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;

public final class TenantConfigAssembler {

    private TenantConfigAssembler() {
    }

    public static CreateTenantConfigCommand toCreateCommand(TenantConfigRequest request) {
        return new CreateTenantConfigCommand(
                request.tenantId(),
                request.configKey(),
                request.configValue(),
                request.valueType(),
                request.description(),
                Boolean.TRUE.equals(request.publicConfig()),
                Boolean.TRUE.equals(request.sensitive()),
                Boolean.TRUE.equals(request.encrypted())
        );
    }

    public static UpdateTenantConfigCommand toUpdateCommand(TenantConfigRequest request) {
        return new UpdateTenantConfigCommand(
                request.configValue(),
                request.valueType(),
                request.description(),
                Boolean.TRUE.equals(request.publicConfig()),
                Boolean.TRUE.equals(request.sensitive()),
                Boolean.TRUE.equals(request.encrypted())
        );
    }

    public static TenantConfigResponse toResponse(TenantConfigDetailView view) {
        return new TenantConfigResponse(
                view.id(),
                view.tenantId(),
                view.configKey(),
                view.configValue(),
                view.valueType().code(),
                view.description(),
                view.publicConfig(),
                view.sensitive(),
                view.encrypted()
        );
    }
}
