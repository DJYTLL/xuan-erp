package com.xuan.erp.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamUserPreferenceView;
import com.xuan.erp.iam.application.service.IamUserPreferenceApplicationService;
import com.xuan.erp.iam.domain.model.IamUserPreference;
import com.xuan.erp.iam.domain.repository.IamUserPreferenceRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamUserPreferenceApplicationServiceTest {

    @Test
    void returnsEmptyPreferenceWhenRecordDoesNotExist() {
        InMemoryPreferenceRepository repository = new InMemoryPreferenceRepository();
        IamUserPreferenceApplicationService service = new IamUserPreferenceApplicationService(repository, new ObjectMapper());

        IamUserPreferenceView view = service.getPreference(currentUser(), "shell.layout");

        assertEquals("shell.layout", view.preferenceKey());
        assertTrue(view.value().isEmpty());
        assertNull(view.updatedAt());
    }

    @Test
    void savesPreferenceAndPreservesCreatedMetadataOnUpdate() {
        InMemoryPreferenceRepository repository = new InMemoryPreferenceRepository();
        IamUserPreferenceApplicationService service = new IamUserPreferenceApplicationService(repository, new ObjectMapper());

        IamUserPreferenceView created = service.savePreference(currentUser(), "shell.layout", Map.of("sidebarCollapsed", true));
        IamUserPreference firstSaved = repository.lastSaved;
        IamUserPreferenceView updated = service.savePreference(currentUser(), "shell.layout", Map.of("sidebarCollapsed", false));
        IamUserPreference secondSaved = repository.lastSaved;

        assertEquals(Boolean.TRUE, created.value().get("sidebarCollapsed"));
        assertEquals(Boolean.FALSE, updated.value().get("sidebarCollapsed"));
        assertEquals(firstSaved.createdBy(), secondSaved.createdBy());
        assertEquals(firstSaved.createdAt(), secondSaved.createdAt());
    }

    @Test
    void rejectsBlankPreferenceKey() {
        InMemoryPreferenceRepository repository = new InMemoryPreferenceRepository();
        IamUserPreferenceApplicationService service = new IamUserPreferenceApplicationService(repository, new ObjectMapper());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.savePreference(currentUser(), "   ", Map.of("sidebarCollapsed", true)));

        assertEquals("IAM_INVALID_ARGUMENT", error.code());
    }

    @Test
    void rejectsMissingCurrentUser() {
        InMemoryPreferenceRepository repository = new InMemoryPreferenceRepository();
        IamUserPreferenceApplicationService service = new IamUserPreferenceApplicationService(repository, new ObjectMapper());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.getPreference(null, "shell.layout"));

        assertEquals("IAM_UNAUTHORIZED", error.code());
    }

    private static CurrentUser currentUser() {
        return new CurrentUser(7L, 1001L, "admin", java.util.Set.of("tenant_admin"), 5L, java.util.Set.of("iam:view"));
    }

    private static final class InMemoryPreferenceRepository implements IamUserPreferenceRepository {
        private final Map<String, IamUserPreference> store = new LinkedHashMap<>();
        private long nextId = 1L;
        private IamUserPreference lastSaved;

        @Override
        public Optional<IamUserPreference> findByTenantIdAndUserIdAndPreferenceKey(Long tenantId, Long userId, String preferenceKey) {
            return Optional.ofNullable(store.get(key(tenantId, userId, preferenceKey)));
        }

        @Override
        public IamUserPreference save(IamUserPreference preference) {
            OffsetDateTime now = OffsetDateTime.now();
            IamUserPreference saved = new IamUserPreference(
                    preference.id() == null ? nextId++ : preference.id(),
                    preference.tenantId(),
                    preference.userId(),
                    preference.preferenceKey(),
                    preference.preferenceJson(),
                    preference.createdBy(),
                    preference.createdAt() == null ? now : preference.createdAt(),
                    preference.updatedBy(),
                    preference.updatedAt() == null ? now : preference.updatedAt());
            store.put(key(saved.tenantId(), saved.userId(), saved.preferenceKey()), saved);
            lastSaved = saved;
            return saved;
        }

        private String key(Long tenantId, Long userId, String preferenceKey) {
            return tenantId + ":" + userId + ":" + preferenceKey;
        }
    }
}
