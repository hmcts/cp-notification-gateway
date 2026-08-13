package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusTest {

    @Nested
    class Mapping {

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
    }

    @Nested
    class FailureClassification {

        @ParameterizedTest
        @CsvSource({
                "technical-failure,true",
                "permanent-failure,true",
                "virus-scan-failed,true",
                "temporary-failure,true",
                "validation-failed,true",
                "not found,true",
                "something-unknown,true",
                "delivered,false",
                "sending,false",
                "created,false",
                "accepted,false",
                "received,false",
                "pending-virus-check,false"
        })
        void classifies_every_non_delivered_non_inflight_status_as_failed(final String status, final boolean failed) {
            final NotificationStatus notificationStatus = NotificationStatus.fromStatus(status);

            final boolean inProgress = !failed && notificationStatus != NotificationStatus.DELIVERED;

            assertThat(notificationStatus.isFailed()).isEqualTo(failed);
            assertThat(notificationStatus.isInProgress()).isEqualTo(inProgress);
        }

        @ParameterizedTest
        @EnumSource(NotificationStatus.class)
        void classifies_every_status_into_exactly_one_of_success_in_progress_or_failed(
                final NotificationStatus status) {
            final int buckets = (status == NotificationStatus.DELIVERED ? 1 : 0)
                    + (status.isInProgress() ? 1 : 0)
                    + (status.isFailed() ? 1 : 0);

            assertThat(buckets)
                    .as("%s must be classified as exactly one of delivered/in-progress/failed", status)
                    .isEqualTo(1);
        }
    }
}
