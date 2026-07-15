package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteGateway;
import com.xuan.erp.common.audit.AuditWriteReceipt;
import org.springframework.stereotype.Component;

/**
 * 基于 Feign 的审计写入网关适配器。
 */
@Component
public class FeignAuditWriteGateway implements AuditWriteGateway {

    private final AuditWriteClient client;

    public FeignAuditWriteGateway(AuditWriteClient client) {
        this.client = client;
    }

    @Override
    public AuditWriteReceipt write(AuditWriteEvent event) {
        ApiResponse<AuditWriteClientResponse> response = client.write(event);
        AuditWriteClientResponse data = response == null ? null : response.data();
        return new AuditWriteReceipt(data == null ? null : data.id());
    }
}
