package uk.gov.hmcts.cp.notification.result;

public sealed interface ResultEvent permits NotificationSentEvent, NotificationFailedEvent {
    String eventName();
}
