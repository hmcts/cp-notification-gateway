package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;

import uk.gov.hmcts.cp.notification.integration.repository.NotificationTestRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationEntityFactory.aNotificationEntity;

public class NotificationQuerySteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private NotificationTestRepository notifications;

    @Autowired
    private TestRestTemplate restTemplate;

    @Given("a notification has been recorded for a recipient")
    public void a_notification_has_been_recorded_for_a_recipient() {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime recordedAt = OffsetDateTime.parse("2026-07-01T09:00:00Z");
        notifications.save(aNotificationEntity()
                .notificationId(id)
                .notificationType("EMAIL")
                .status("FAILED")
                .sendToAddress("recipient@example.com")
                .statusCode(503)
                .errorMessage("Attachment not found")
                .clientContext("mi-reportdata")
                .resultQueue("ng-result-correspondence")
                .createdAt(recordedAt)
                .updatedAt(recordedAt)
                .build());
        context.setNotificationId(id.toString());
        context.setRecordedStatus("FAILED");
        context.setRecordedCreatedAt(recordedAt);
    }

    @When("the operator looks up that notification by its id")
    public void the_operator_looks_up_that_notification_by_its_id() {
        context.setLastResponseBody(restTemplate
                .getForEntity("/notifications/{id}", String.class, context.getNotificationId())
                .getBody());
    }

    @Then("the full notification record is returned")
    public void the_full_notification_record_is_returned() {
        assertThatJson(context.getLastResponseBody())
                .isEqualTo(load("fixtures/query/notification-record-response.json",
                        Map.of("notificationId", context.getNotificationId())));
    }

    @Then("searching by that notification's status and creation date returns that notification")
    public void searching_by_status_and_creation_date_returns_that_notification() {
        final OffsetDateTime recordedAt = context.getRecordedCreatedAt();
        final String body = restTemplate.getForEntity(
                "/notifications?status={status}&createdFrom={from}&createdTo={to}",
                String.class,
                context.getRecordedStatus(),
                recordedAt.minusDays(1).toString(),
                recordedAt.plusDays(1).toString()).getBody();

        assertThatJson(body).node("content[0].notificationId").isEqualTo(context.getNotificationId());
    }
}
