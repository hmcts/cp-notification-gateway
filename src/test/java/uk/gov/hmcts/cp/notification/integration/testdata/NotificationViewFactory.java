package uk.gov.hmcts.cp.notification.integration.testdata;

import uk.gov.hmcts.cp.notification.web.NotificationView;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NotificationViewFactory {

    private NotificationViewFactory() {
    }

    public static NotificationView.NotificationViewBuilder aNotificationView() {
        return NotificationView.builder()
                .notificationId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .notificationType("EMAIL")
                .status("FAILED")
                .sendToAddress("recipient@example.com")
                .statusCode(503)
                .errorMessage("Attachment not found")
                .clientContext("mi-reportdata")
                .resultQueue("ng-result-correspondence")
                .createdAt(OffsetDateTime.parse("2026-07-01T09:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-07-01T09:05:00Z"));
    }
}
