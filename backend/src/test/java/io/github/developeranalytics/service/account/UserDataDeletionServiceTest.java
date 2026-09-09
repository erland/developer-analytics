package io.github.developeranalytics.service.account;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@Tag("privacy")
class UserDataDeletionServiceTest {

    @Test
    void wrapsTransactionalFailureInStableDeletionFailure() {
        RuntimeException databaseFailure = new RuntimeException("transaction timeout");
        UserDataDeletionService service = new UserDataDeletionService();
        service.persistence = new FailingPersistenceService(databaseFailure);

        DataDeletionFailedException failure = assertThrows(
                DataDeletionFailedException.class,
                () -> service.deleteUser(UUID.randomUUID())
        );

        assertSame(databaseFailure, failure.getCause());
    }

    @Test
    void preservesNotFoundSemantics() {
        NotFoundException missing = new NotFoundException("missing");
        UserDataDeletionService service = new UserDataDeletionService();
        service.persistence = new FailingPersistenceService(missing);

        assertSame(missing, assertThrows(
                NotFoundException.class,
                () -> service.deleteUser(UUID.randomUUID())
        ));
    }

    @Test
    void returnsSuccessfulDeletionResultUnchanged() {
        UUID userId = UUID.randomUUID();
        UserDataDeletionService.DeletionResult expected =
                new UserDataDeletionService.DeletionResult(
                        userId,
                        Map.of("contributions", 42L),
                        0
                );
        UserDataDeletionService service = new UserDataDeletionService();
        service.persistence = new SuccessfulPersistenceService(expected);

        assertEquals(expected, service.deleteUser(userId));
    }

    private static final class FailingPersistenceService extends UserDataDeletionPersistenceService {
        private final RuntimeException failure;

        private FailingPersistenceService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public UserDataDeletionService.DeletionResult deleteUser(UUID userId) {
            throw failure;
        }
    }

    private static final class SuccessfulPersistenceService extends UserDataDeletionPersistenceService {
        private final UserDataDeletionService.DeletionResult result;

        private SuccessfulPersistenceService(UserDataDeletionService.DeletionResult result) {
            this.result = result;
        }

        @Override
        public UserDataDeletionService.DeletionResult deleteUser(UUID userId) {
            return result;
        }
    }
}
