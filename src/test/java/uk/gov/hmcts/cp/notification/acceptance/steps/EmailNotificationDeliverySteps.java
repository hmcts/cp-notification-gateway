package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.cp.notification.integration.Fixtures;
import uk.gov.hmcts.cp.notification.integration.repository.JobTestRepository;
import uk.gov.hmcts.cp.notification.integration.repository.NotificationTestRepository;
import uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService;
import uk.gov.hmcts.cp.notification.integration.stubs.support.WireMockSupport;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.cp.notification.integration.stubs.ASBTestClient.anAsbTestClient;
import static uk.gov.hmcts.cp.notification.integration.stubs.AzureBlobFileStoreStub.anAzureBlobFileStore;
import static uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService.aGovUkNotifyService;

public class EmailNotificationDeliverySteps {
    @Autowired
    private NotificationTestRepository notifications;

    @Autowired
    private JobTestRepository jobs;

    private final GovUkNotifyStubService govUkNotify = aGovUkNotifyService();

    private String notificationId;
    private String templateId;
    private String commandJson;
    private byte[] attachmentBytes;
    private String externalReference;

    @Before
    public void resetProviderStubs() {
        WireMockSupport.reset();
    }

    @Given("a send-email-notification command for a recipient with an attachment")
    public void a_send_email_notification_command_for_a_recipient_with_an_attachment() {
        notificationId = UUID.randomUUID().toString();
        templateId = UUID.randomUUID().toString();
        externalReference = UUID.randomUUID().toString();
        attachmentBytes = Fixtures.loadBytes("attachments/report.csv");

        final String blobName = "report-" + notificationId + ".csv";
        final String fileUri = anAzureBlobFileStore()
                .containing(blobName, attachmentBytes)
                .uriOf(blobName);

        govUkNotify
                .sendEmailNotificationWillReturnSuccess(externalReference)
                .getNotificationStatusWillReturnSuccess(externalReference);

        commandJson = Fixtures.load("commands/send-email-with-attachment.json", Map.of(
                "notificationId", notificationId,
                "templateId", templateId,
                "fileUri", fileUri));
    }

    @Given("a send-email-notification command whose attachment is missing")
    public void a_send_email_notification_command_whose_attachment_is_missing() {
        notificationId = UUID.randomUUID().toString();
        templateId = UUID.randomUUID().toString();

        final String fileUri = anAzureBlobFileStore().uriOf("missing-" + notificationId + ".csv");

        commandJson = Fixtures.load("commands/send-email-missing-attachment.json", Map.of(
                "notificationId", notificationId,
                "templateId", templateId,
                "fileUri", fileUri));
    }

    @When("the gateway processes the command")
    public void the_gateway_processes_the_command() {
        anAsbTestClient().sendToCommandQueue(commandJson);
    }

    @Then("the email is sent via the Gov.UK Notify provider")
    public void the_email_is_sent_via_the_gov_uk_notify_provider() {
        final String expectedRequest = Fixtures.load("gov-notify/expected-send-email-request.json", Map.of(
                "templateId", templateId,
                "notificationId", notificationId,
                "attachmentBase64", Base64.getEncoder().encodeToString(attachmentBytes)));
        await().atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> govUkNotify.sendEmailRequestMatches(expectedRequest));
    }

    @Then("the delivery status is polled from the provider")
    public void the_delivery_status_is_polled_from_the_provider() {
        await().atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> govUkNotify.deliveryStatusWasPolledFor(externalReference));
    }

    @Then("the notification is recorded as {word}")
    public void the_notification_is_recorded_as(final String status) {
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(notifications.findById(UUID.fromString(notificationId)))
                        .hasValueSatisfying(row -> assertThat(row.getStatus()).isEqualTo(status)));
    }

    @Then("no email is sent via the Gov.UK Notify provider")
    public void no_email_is_sent_via_the_gov_uk_notify_provider() {
        govUkNotify.emailNotificationWasNotSent();
    }

    @After
    public void cleanUp() {
        jobs.deleteAll();
        notifications.deleteAll();
    }
}
