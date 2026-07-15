package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.service.TenantPlanAssignmentApplicationService;
import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.service.TenantResourceCatalog;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TenantPlanAssignmentApplicationServiceTest {

    @Test
    void createAssignmentDefaultsRequiredDateFieldsWhenRequestOmitsThem() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        TenantPlanAssignmentApplicationService service = createService(repository);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", 2L);
        values.put("planId", 1L);
        values.put("status", null);
        values.put("effectiveAt", null);
        values.put("assignedAt", null);

        service.createAssignment(values);

        assertEquals("tenant_plan_assignment", repository.lastResource().tableName());
        assertEquals("ACTIVE", repository.lastValues().get("status"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("effective_at"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("assigned_at"));
    }

    @Test
    void updateAssignmentDefaultsRequiredDateFieldsWhenRequestOmitsThem() {
        RecordingResourceRepository repository = new RecordingResourceRepository();
        TenantPlanAssignmentApplicationService service = createService(repository);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("planId", 3L);
        values.put("status", "");
        values.put("effectiveAt", null);
        values.put("assignedAt", null);

        service.updateAssignment(33L, values);

        assertEquals("tenant_plan_assignment", repository.lastResource().tableName());
        assertEquals("ACTIVE", repository.lastValues().get("status"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("effective_at"));
        assertInstanceOf(OffsetDateTime.class, repository.lastValues().get("assigned_at"));
    }

    private TenantPlanAssignmentApplicationService createService(RecordingResourceRepository repository) {
        return new TenantPlanAssignmentApplicationService(new TenantResourceApplicationService(
                TenantResourceCatalog.defaultCatalog(),
                repository
        ));
    }

    private static final class RecordingResourceRepository implements TenantResourceRepository {

        private TenantResourceDefinition lastResource;
        private Map<String, Object> lastValues = Map.of();

        @Override
        public List<Map<String, Object>> list(TenantResourceDefinition resource) {
            lastResource = resource;
            return List.of();
        }

        @Override
        public Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id) {
            lastResource = resource;
            return Optional.empty();
        }

        @Override
        public Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values) {
            lastResource = resource;
            lastValues = new LinkedHashMap<>(values);
            return values;
        }

        @Override
        public Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values) {
            lastResource = resource;
            lastValues = new LinkedHashMap<>(values);
            return values;
        }

        @Override
        public void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator) {
            lastResource = resource;
        }

        TenantResourceDefinition lastResource() {
            return lastResource;
        }

        Map<String, Object> lastValues() {
            return lastValues;
        }
    }
}
