package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusTest {

    @Test
    void maps_known_provider_statuses_to_the_matching_enum() {
        assertThat(NotificationStatus.fromStatus("delivered")).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(NotificationStatus.fromStatus("temporary-failure")).isEqualTo(NotificationStatus.TEMPORARY_FAILURE);
        assertThat(NotificationStatus.fromStatus("permanent-failure")).isEqualTo(NotificationStatus.PERMANENT_FAILURE);
        assertThat(NotificationStatus.fromStatus("technical-failure")).isEqualTo(NotificationStatus.FAILED);
        assertThat(NotificationStatus.fromStatus("virus-scan-failed")).isEqualTo(NotificationStatus.VIRUS_SCAN_FAILED);
    }

    @Test
    void maps_an_unknown_status_to_unexpected_failure() {
        assertThat(NotificationStatus.fromStatus("something-new")).isEqualTo(NotificationStatus.UNEXPECTED_FAILURE);
        assertThat(NotificationStatus.fromStatus(null)).isEqualTo(NotificationStatus.UNEXPECTED_FAILURE);
    }

    @ParameterizedTest
    @CsvSource({
            "technical-failure,true",
            "permanent-failure,true",
            "virus-scan-failed,true",
            "temporary-failure,false",
            "delivered,false",
            "sending,false",
            "created,false",
            "accepted,false",
            "received,false",
            "validation-failed,false",
            "pending-virus-check,false"
    })
    void classifies_only_the_three_terminal_states_as_failed(final String status, final boolean failed) {
        final NotificationStatus notificationStatus = NotificationStatus.fromStatus(status);

        assertThat(notificationStatus.isFailed()).isEqualTo(failed);
        assertThat(notificationStatus.isInProgress()).isEqualTo(!failed);
    }
}
