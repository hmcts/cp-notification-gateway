package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService;

import static java.time.Duration.ofSeconds;
import static java.util.Map.of;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;
import static uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService.aGovUkNotifyService;

public class GovNotifySteps {

    @Autowired
    private ScenarioContext context;

    private final GovUkNotifyStubService govUkNotify = aGovUkNotifyService();

    @Then("the email is sent via the Gov.UK Notify provider")
    public void the_email_is_sent_via_the_gov_uk_notify_provider() {
        final String expectedRequest = load("fixtures/gov-notify/expected-send-email-request.json", of(
                "templateId", context.getTemplateId(),
                "notificationId", context.getNotificationId()));
        final byte[] attachmentBytes = context.getAttachmentBytes();
        await().atMost(ofSeconds(60))
                .untilAsserted(() -> govUkNotify.sendEmailRequestMatches(expectedRequest, attachmentBytes));
    }

    @Then("the delivery status is polled from the provider")
    public void the_delivery_status_is_polled_from_the_provider() {
        final String externalReference = context.getExternalReference();
        await().atMost(ofSeconds(60))
                .untilAsserted(() -> govUkNotify.deliveryStatusWasPolledFor(externalReference));
    }

    @Then("no email is sent via the Gov.UK Notify provider")
    public void no_email_is_sent_via_the_gov_uk_notify_provider() {
        govUkNotify.emailNotificationWasNotSent();
    }
}
