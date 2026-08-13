package uk.gov.hmcts.cp.notification.task;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.cp.notification.service.NotificationStatusService;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransientFailurePolicyTest {

    @Mock
    private NotificationStatusService statusService;

    @InjectMocks
    private TransientFailurePolicy policy;

    @Test
    void retryOrFail_should_retry_without_touching_status_when_attempts_remain() {
        final UUID id = UUID.randomUUID();

        final ExecutionInfo result = policy.retryOrFail(id, 500, "did not recover", jobWithRetriesRemaining(2));

        verifyNoInteractions(statusService);
        assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.INPROGRESS);
        assertThat(result.isShouldRetry()).isTrue();
    }

    @Test
    void retryOrFail_should_retry_when_retry_attempts_are_unset() {
        final UUID id = UUID.randomUUID();

        final ExecutionInfo result = policy.retryOrFail(id, 500, "did not recover", jobWithRetriesRemaining(null));

        verifyNoInteractions(statusService);
        assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.INPROGRESS);
        assertThat(result.isShouldRetry()).isTrue();
    }

    @Test
    void retryOrFail_should_mark_failed_and_complete_when_retries_are_exhausted() {
        final UUID id = UUID.randomUUID();

        final ExecutionInfo result = policy.retryOrFail(id, 503, "did not recover", jobWithRetriesRemaining(0));

        verify(statusService).markFailed(id, 503, "did not recover");
        assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
    }

    @Test
    void fail_should_mark_failed_and_complete_without_retry() {
        final UUID id = UUID.randomUUID();

        final ExecutionInfo result = policy.fail(id, 400, "bad request", jobWithRetriesRemaining(3));

        verify(statusService).markFailed(id, 400, "bad request");
        assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.isShouldRetry()).isFalse();
    }

    @Test
    void fail_should_accept_a_null_status_code() {
        final UUID id = UUID.randomUUID();

        policy.fail(id, null, "terminal status", jobWithRetriesRemaining(3));

        verify(statusService).markFailed(id, null, "terminal status");
    }

    private static ExecutionInfo jobWithRetriesRemaining(final Integer retriesRemaining) {
        return ExecutionInfo.executionInfo()
                .withJobData(Json.createObjectBuilder().add("notificationId", UUID.randomUUID().toString()).build())
                .withAssignedTaskName(SendEmailTask.TASK_NAME)
                .withAssignedTaskStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withExecutionStatus(ExecutionStatus.STARTED)
                .withRetryAttemptsRemaining(retriesRemaining)
                .build();
    }
}
