package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.service.TenantApplicationService;
import com.xuan.erp.tenant.application.service.TenantConfigApplicationService;
import com.xuan.erp.tenant.application.service.TenantContactApplicationService;
import com.xuan.erp.tenant.application.service.TenantDomainApplicationService;
import com.xuan.erp.tenant.application.service.TenantPlanApplicationService;
import com.xuan.erp.tenant.application.service.TenantPlanAssignmentApplicationService;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.CreateTenantPlanCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.application.query.TenantPlanDetailView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantConfig;
import com.xuan.erp.tenant.domain.model.TenantContact;
import com.xuan.erp.tenant.domain.model.TenantDomain;
import com.xuan.erp.tenant.domain.model.TenantOutboxEvent;
import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantProvisionTaskStep;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.repository.TenantConfigRepository;
import com.xuan.erp.tenant.domain.repository.TenantOutboxEventRepository;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.ConfigValueType;
import com.xuan.erp.tenant.domain.model.type.OutboxEventStatus;
import com.xuan.erp.tenant.domain.model.type.PlanAssignmentStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;
import com.xuan.erp.tenant.domain.model.type.TenantContactType;
import com.xuan.erp.tenant.domain.model.type.TenantDomainStatus;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantRecord;
import com.xuan.erp.tenant.infrastructure.persistence.repository.TenantPlanRepositoryAdapter;
import com.xuan.erp.tenant.interfaces.assembler.TenantAssembler;
import com.xuan.erp.tenant.interfaces.assembler.TenantConfigAssembler;
import com.xuan.erp.tenant.interfaces.assembler.TenantContactAssembler;
import com.xuan.erp.tenant.interfaces.assembler.TenantDomainAssembler;
import com.xuan.erp.tenant.interfaces.assembler.TenantPlanAssembler;
import com.xuan.erp.tenant.interfaces.controller.TenantController;
import com.xuan.erp.tenant.interfaces.controller.TenantConfigController;
import com.xuan.erp.tenant.interfaces.controller.TenantContactController;
import com.xuan.erp.tenant.interfaces.controller.TenantDomainController;
import com.xuan.erp.tenant.interfaces.controller.TenantInternalStatusController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanAssignmentController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanController;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantContactResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantDomainResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantInternalStatusResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanResponse;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantDddSkeletonTest {

    @Test
    void exposesDomainModelsForEveryTenantTable() {
        List<Class<?>> models = List.of(
                Tenant.class,
                TenantPlan.class,
                TenantPlanAssignment.class,
                TenantDomain.class,
                TenantContact.class,
                TenantStatusHistory.class,
                TenantConfig.class,
                TenantProvisionTask.class,
                TenantProvisionTaskStep.class,
                TenantOutboxEvent.class
        );

        assertEquals(10, models.size());
    }

    @Test
    void exposesStatusTypesFromTenantSchema() {
        assertEquals(TenantStatus.ENABLED, TenantStatus.fromCode("enabled"));
        assertEquals(TenantPlanStatus.ENABLED, TenantPlanStatus.fromCode("ENABLED"));
        assertEquals(BillingCycle.MONTHLY, BillingCycle.fromCode("monthly"));
        assertEquals(PlanAssignmentStatus.ACTIVE, PlanAssignmentStatus.fromCode("ACTIVE"));
        assertEquals(TenantDomainStatus.PENDING, TenantDomainStatus.fromCode("pending"));
        assertEquals(TenantContactType.ADMIN, TenantContactType.fromCode("admin"));
        assertEquals(ConfigValueType.JSON, ConfigValueType.fromCode("json"));
        assertEquals(ProvisionTaskStatus.SUCCEEDED, ProvisionTaskStatus.fromCode("succeeded"));
        assertEquals(ProvisionTaskStepStatus.SKIPPED, ProvisionTaskStepStatus.fromCode("skipped"));
        assertEquals(OutboxEventStatus.DEAD_LETTERED, OutboxEventStatus.fromCode("dead_lettered"));
    }

    @Test
    void exposesFourLayerPorts() {
        List<Class<?>> ports = List.of(
                TenantRepository.class,
                TenantPlanRepository.class,
                TenantConfigRepository.class,
                TenantProvisionTaskRepository.class,
                TenantOutboxEventRepository.class,
                TenantApplicationService.class,
                TenantConfigApplicationService.class,
                TenantContactApplicationService.class,
                TenantDomainApplicationService.class,
                TenantPlanApplicationService.class,
                TenantPlanAssignmentApplicationService.class,
                CreateTenantCommand.class,
                CreateTenantPlanCommand.class,
                TenantDetailView.class,
                TenantPlanDetailView.class,
                TenantRecord.class,
                TenantPlanRepositoryAdapter.class,
                TenantController.class,
                TenantConfigController.class,
                TenantContactController.class,
                TenantDomainController.class,
                TenantInternalStatusController.class,
                TenantPlanAssignmentController.class,
                TenantPlanController.class,
                TenantAssembler.class,
                TenantConfigAssembler.class,
                TenantContactAssembler.class,
                TenantDomainAssembler.class,
                TenantPlanAssembler.class,
                TenantConfigResponse.class,
                TenantContactResponse.class,
                TenantDomainResponse.class,
                TenantInternalStatusResponse.class,
                TenantPlanAssignmentResponse.class,
                TenantPlanResponse.class,
                TenantResponse.class
        );

        assertEquals(36, ports.size());
    }

    @Test
    void allServiceModulesExposeStandardDddPackageSkeleton() {
        Path backend = Path.of("..").toAbsolutePath().normalize();
        List<ServiceModule> modules = List.of(
                new ServiceModule("xuan-gateway", "gateway"),
                new ServiceModule("xuan-tenant", "tenant"),
                new ServiceModule("xuan-iam", "iam"),
                new ServiceModule("xuan-audit", "audit"),
                new ServiceModule("xuan-product", "product"),
                new ServiceModule("xuan-party", "party"),
                new ServiceModule("xuan-warehouse", "warehouse"),
                new ServiceModule("xuan-inventory", "inventory"),
                new ServiceModule("xuan-sales", "sales"),
                new ServiceModule("xuan-procurement", "procurement"),
                new ServiceModule("xuan-finance", "finance"),
                new ServiceModule("xuan-document", "document"),
                new ServiceModule("xuan-manufacturing", "manufacturing"),
                new ServiceModule("xuan-query", "query")
        );
        List<String> packages = List.of(
                "interfaces/controller",
                "interfaces/dto",
                "interfaces/assembler",
                "application/service",
                "application/command",
                "application/query",
                "application/event",
                "domain/model",
                "domain/service",
                "domain/repository",
                "domain/event",
                "domain/factory",
                "infrastructure/persistence/entity",
                "infrastructure/persistence/mapper",
                "infrastructure/persistence/assembler",
                "infrastructure/persistence/repository",
                "infrastructure/rpc",
                "infrastructure/mq",
                "infrastructure/config"
        );

        for (ServiceModule module : modules) {
            Path basePackage = backend.resolve(module.name())
                    .resolve("src/main/java/com/xuan/erp")
                    .resolve(module.packageName());
            for (String packagePath : packages) {
                Path packageInfo = basePackage.resolve(packagePath).resolve("package-info.java");
                assertTrue(
                        Files.isRegularFile(packageInfo),
                        () -> module.name() + " 缺少 DDD 包骨架：" + packageInfo
                );
            }
        }
    }

    private record ServiceModule(String name, String packageName) {
    }
}
