package uk.gov.hmcts.cp.notification.sender;

import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record SendEmailRequest(
        UUID notificationId,
        UUID templateId,
        String emailAddress,
        Map<String, Object> personalisation,
        byte[] attachment,
        String attachmentFilename,
        String replyToAddress,
        UUID replyToAddressId) {
}
