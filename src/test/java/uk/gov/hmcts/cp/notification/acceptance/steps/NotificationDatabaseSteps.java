package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.cp.notification.integration.repository.NotificationTestRepository;

import static java.time.Duration.ofSeconds;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class NotificationDatabaseSteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private NotificationTestRepository notifications;

    @Then("the notification is recorded as {word}")
    public void the_notification_is_recorded_as(final String status) {
        final String notificationId = context.getNotificationId();
        await().atMost(ofSeconds(60)).untilAsserted(() ->
                assertThat(notifications.findById(fromString(notificationId)))
                        .hasValueSatisfying(row -> assertThat(row.getStatus()).isEqualTo(status)));
    }
}
