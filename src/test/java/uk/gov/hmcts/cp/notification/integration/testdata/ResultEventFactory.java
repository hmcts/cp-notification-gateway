package uk.gov.hmcts.cp.notification.integration.testdata;

import uk.gov.hmcts.cp.notification.result.NotificationFailedEvent;
import uk.gov.hmcts.cp.notification.result.NotificationSentEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ResultEventFactory {

    private ResultEventFactory() {
    }

    public static NotificationSentEvent.NotificationSentEventBuilder aNotificationSentEvent() {
        return NotificationSentEvent.builder()
                .notificationId(UUID.randomUUID())
                .sentTime(OffsetDateTime.parse("2026-07-20T10:15:30Z"))
                .sendToAddress("recipient@example.com")
                .clientContext("mi-reportdata");
    }

    public static NotificationFailedEvent.NotificationFailedEventBuilder aNotificationFailedEvent() {
        return NotificationFailedEvent.builder()
                .notificationId(UUID.randomUUID())
                .failedTime(OffsetDateTime.parse("2026-07-20T10:16:00Z"))
                .errorMessage("Gov.Notify responded with status 'permanent-failure'")
                .statusCode(400)
                .clientContext("mi-reportdata");
    }
}
