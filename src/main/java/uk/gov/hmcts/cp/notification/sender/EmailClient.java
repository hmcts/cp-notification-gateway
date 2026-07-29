package uk.gov.hmcts.cp.notification.sender;

public interface EmailClient {
    SendResult send(SendEmailRequest request);
}
