package uk.gov.hmcts.cp.notification.task;

import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo.executionInfo;
import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus.COMPLETED;
import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus.INPROGRESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.cp.notification.service.NotificationStatusService;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;

import java.util.UUID;

/**
 * Shared retry/failure scaffolding for notification tasks.
 *
 * <p>Every notification task classifies a failure as either <em>transient</em> (retry until the
 * task-manager retry window is exhausted, then record a FAILED status) or <em>permanent</em>
 * (record FAILED immediately, no retry). Centralising the decision here guarantees the two
 * behaviours are identical across tasks and — crucially — that an exhausted transient failure is
 * never dropped silently: it always terminates in {@link NotificationStatusService#markFailed}.
 *
 * <p>Without this, an unhandled exception escaping a task's {@code execute} is caught by the
 * task-manager's generic catch-all, retried, and then quietly released once retries run out —
 * leaving the notification stuck in its pre-send state with no FAILED status recorded.
 */
@Component
public class TransientFailurePolicy {
    private static final Logger LOG = LoggerFactory.getLogger(TransientFailurePolicy.class);

    private final NotificationStatusService statusService;

    public TransientFailurePolicy(final NotificationStatusService statusService) {
        this.statusService = statusService;
    }

    /**
     * Handle a transient failure: retry while the task-manager retry window still has attempts
     * remaining, otherwise mark the notification FAILED with {@code exhaustedReason} and complete.
     *
     * @param notificationId  the notification being processed
     * @param statusCode      the HTTP/status code of the failure, or {@code null} if none applies
     * @param exhaustedReason the failure reason recorded once retries are exhausted
     * @param executionInfo   the current execution
     * @return {@code INPROGRESS} + retry while attempts remain, otherwise {@code COMPLETED}
     */
    public ExecutionInfo retryOrFail(
            final UUID notificationId,
            final Integer statusCode,
            final String exhaustedReason,
            final ExecutionInfo executionInfo) {
        final ExecutionInfo result;
        if (retriesExhausted(executionInfo)) {
            LOG.warn("Notification {} did not resolve (statusCode {}) after exhausting retries — marking FAILED",
                    notificationId, statusCode);
            result = fail(notificationId, statusCode, exhaustedReason, executionInfo);
        } else {
            LOG.info("Transient failure for notification {} (statusCode {}) — will retry", notificationId, statusCode);
            result = retry(executionInfo);
        }
        return result;
    }

    /**
     * Handle a permanent failure: record FAILED immediately and complete, with no retry.
     */
    public ExecutionInfo fail(
            final UUID notificationId,
            final Integer statusCode,
            final String reason,
            final ExecutionInfo executionInfo) {
        statusService.markFailed(notificationId, statusCode, reason);
        return completed(executionInfo);
    }

    public static ExecutionInfo completed(final ExecutionInfo executionInfo) {
        return executionInfo().from(executionInfo).withExecutionStatus(COMPLETED).build();
    }

    public static ExecutionInfo retry(final ExecutionInfo executionInfo) {
        return executionInfo().from(executionInfo)
                .withExecutionStatus(INPROGRESS)
                .withShouldRetry(true)
                .build();
    }

    private static boolean retriesExhausted(final ExecutionInfo executionInfo) {
        final Integer remaining = executionInfo.getRetryAttemptsRemaining();
        return remaining != null && remaining <= 0;
    }
}
