package uk.gov.hmcts.cp.notification.web;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record NotificationView(
        UUID notificationId,
        String notificationType,
        String status,
        String sendToAddress,
        Integer statusCode,
        String errorMessage,
        String clientContext,
        String resultQueue,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
