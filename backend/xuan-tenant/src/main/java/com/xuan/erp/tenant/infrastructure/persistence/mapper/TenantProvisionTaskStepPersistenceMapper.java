package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantProvisionTaskStepRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantProvisionTaskStepPersistenceMapper {

    List<TenantProvisionTaskStepRecord> findByTaskId(@Param("taskId") Long taskId);

    TenantProvisionTaskStepRecord findActiveByTaskIdAndStepKey(@Param("taskId") Long taskId, @Param("stepKey") String stepKey);

    int insert(TenantProvisionTaskStepRecord record);

    int update(TenantProvisionTaskStepRecord record);
}
