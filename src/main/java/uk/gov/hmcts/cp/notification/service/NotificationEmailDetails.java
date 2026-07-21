package uk.gov.hmcts.cp.notification.service;

public record NotificationEmailDetails(String emailSubject, String emailBody, String replyToAddress) {
}
