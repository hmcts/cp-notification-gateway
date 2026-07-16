package uk.gov.hmcts.cp.notification.integration.testdata;

import uk.gov.hmcts.cp.notification.sender.SendEmailRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class SendEmailRequestFactory {

    private SendEmailRequestFactory() {
    }

    public static SendEmailRequest.SendEmailRequestBuilder aSendEmailRequest() {
        return SendEmailRequest.builder()
                .notificationId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .emailAddress("recipient@example.com")
                .personalisation(Map.of())
                .attachment("report".getBytes(StandardCharsets.UTF_8));
    }
}
