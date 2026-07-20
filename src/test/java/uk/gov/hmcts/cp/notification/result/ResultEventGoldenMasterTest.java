package uk.gov.hmcts.cp.notification.result;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema.NOTIFICATION_FAILED;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema.NOTIFICATION_SENT;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema.assertConformsTo;

class ResultEventGoldenMasterTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Nested
    class NotificationSent {

        @Test
        void carries_the_golden_master_event_name() {
            assertThat(NotificationSentEvent.EVENT_NAME)
                    .isEqualTo("public.notificationnotify.events.notification-sent");
        }

        @Test
        void copies_every_populated_field_verbatim() {
            final String json = MAPPER.writeValueAsString(NotificationSentEvent.builder()
                    .notificationId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .sentTime(OffsetDateTime.parse("2026-07-20T10:15:30Z"))
                    .completedAt(OffsetDateTime.parse("2026-07-20T10:15:31Z"))
                    .sendToAddress("recipient@example.com")
                    .replyToAddress("noreply@justice.gov.uk")
                    .emailSubject("Your NCES extract")
                    .emailBody("Please find your report attached.")
                    .clientContext("mi-reportdata")
                    .build());

            assertThatJson(json).isEqualTo(load("result-events/notification-sent-full.json"));
            assertConformsTo(NOTIFICATION_SENT, json);
        }

        @Test
        void emits_only_the_required_fields_when_optionals_are_absent() {
            final String json = MAPPER.writeValueAsString(NotificationSentEvent.builder()
                    .notificationId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .sentTime(OffsetDateTime.parse("2026-07-20T10:15:30Z"))
                    .build());

            assertThatJson(json).isEqualTo(load("result-events/notification-sent-minimal.json"));
            assertConformsTo(NOTIFICATION_SENT, json);
        }
    }

    @Nested
    class NotificationFailed {

        @Test
        void carries_the_golden_master_event_name() {
            assertThat(NotificationFailedEvent.EVENT_NAME)
                    .isEqualTo("public.notificationnotify.events.notification-failed");
        }

        @Test
        void builds_the_payload_field_by_field_dropping_the_failed_task() {
            final String json = MAPPER.writeValueAsString(NotificationFailedEvent.builder()
                    .notificationId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .failedTime(OffsetDateTime.parse("2026-07-20T10:16:00Z"))
                    .errorMessage("Gov.Notify responded with status 'permanent-failure'")
                    .statusCode(400)
                    .clientContext("mi-reportdata")
                    .build());

            assertThatJson(json).isEqualTo(load("result-events/notification-failed-full.json"));
            assertThatJson(json).node("failedTask").isAbsent();
            assertConformsTo(NOTIFICATION_FAILED, json);
        }

        @Test
        void omits_absent_optional_fields_rather_than_emitting_nulls() {
            final String json = MAPPER.writeValueAsString(NotificationFailedEvent.builder()
                    .notificationId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .failedTime(OffsetDateTime.parse("2026-07-20T10:16:00Z"))
                    .errorMessage("attachment could not be retrieved")
                    .build());

            assertThatJson(json).isEqualTo(load("result-events/notification-failed-minimal.json"));
            assertThatJson(json).node("statusCode").isAbsent();
            assertThatJson(json).node("clientContext").isAbsent();
            assertConformsTo(NOTIFICATION_FAILED, json);
        }
    }
}
