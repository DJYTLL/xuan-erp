package com.xuan.erp.tenant.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 租户权限同步自愈任务。
 */
@Component
public class TenantPermissionSyncRepairScheduler {

    private final TenantPlanAssignmentApplicationService assignmentApplicationService;
    private final boolean enabled;
    private final int repairLimit;

    public TenantPermissionSyncRepairScheduler(
            TenantPlanAssignmentApplicationService assignmentApplicationService,
            @Value("${xuan.tenant.permission-sync.auto-repair-enabled:true}") boolean enabled,
            @Value("${xuan.tenant.permission-sync.auto-repair-limit:20}") int repairLimit) {
        this.assignmentApplicationService = assignmentApplicationService;
        this.enabled = enabled;
        this.repairLimit = repairLimit;
    }

    @Scheduled(fixedDelayString = "${xuan.tenant.permission-sync.auto-repair-delay:PT5M}")
    public void repairPendingPermissionSyncs() {
        if (!enabled) {
            return;
        }
        assignmentApplicationService.repairPendingPermissionSyncs(repairLimit);
    }
}
