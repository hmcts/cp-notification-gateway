package uk.gov.hmcts.cp.notification.sender;

import java.util.EnumSet;
import java.util.Set;

public enum NotificationStatus {
    ACCEPTED("accepted"),
    CREATED("created"),
    SENDING("sending"),
    DELIVERED("delivered"),
    TEMPORARY_FAILURE("temporary-failure"),
    PERMANENT_FAILURE("permanent-failure"),
    FAILED("technical-failure"),
    NOT_FOUND("not found"),
    INVALID_REQUEST("validation-failed"),
    UNEXPECTED_FAILURE("unexpected"),
    RECEIVED("received"),
    PENDING_VIRUS_CHECK("pending-virus-check"),
    VIRUS_SCAN_FAILED("virus-scan-failed");

    private static final Set<NotificationStatus> IN_PROGRESS_STATES =
            EnumSet.of(ACCEPTED, CREATED, SENDING, RECEIVED, PENDING_VIRUS_CHECK);

    private final String status;

    NotificationStatus(final String status) {
        this.status = status;
    }

    public static NotificationStatus fromStatus(final String status) {
        NotificationStatus result = UNEXPECTED_FAILURE;
        for (final NotificationStatus value : values()) {
            if (value.status.equals(status)) {
                result = value;
                break;
            }
        }
        return result;
    }

    public String getStatus() {
        return status;
    }

    public boolean isInProgress() {
        return IN_PROGRESS_STATES.contains(this);
    }

    public boolean isFailed() {
        return this != DELIVERED && !isInProgress();
    }
}
