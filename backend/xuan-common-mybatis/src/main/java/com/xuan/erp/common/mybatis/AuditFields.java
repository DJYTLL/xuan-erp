package com.xuan.erp.common.mybatis;

import java.time.OffsetDateTime;

public interface AuditFields {

    Long tenantId();

    OffsetDateTime createdAt();

    OffsetDateTime updatedAt();

    OffsetDateTime deletedAt();
}
