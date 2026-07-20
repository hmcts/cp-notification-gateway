package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService;

import static java.util.Map.of;
import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.loadBytes;
import static uk.gov.hmcts.cp.notification.integration.stubs.ASBTestClient.anAsbTestClient;
import static uk.gov.hmcts.cp.notification.integration.stubs.AzureBlobFileStoreStub.anAzureBlobFileStore;
import static uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService.aGovUkNotifyService;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport.RESULT_QUEUE;

public class CommandSteps {

    @Autowired
    private ScenarioContext context;

    private final GovUkNotifyStubService govUkNotify = aGovUkNotifyService();

    @Given("a send-email-notification command for a recipient with an attachment")
    public void a_send_email_notification_command_for_a_recipient_with_an_attachment() {
        final String notificationId = randomUUID().toString();
        final String templateId = randomUUID().toString();
        final String externalReference = randomUUID().toString();
        final byte[] attachmentBytes = loadBytes("attachments/report.csv");

        final String blobName = "report-" + notificationId + ".csv";
        final String fileUri = anAzureBlobFileStore()
                .containing(blobName, attachmentBytes)
                .uriOf(blobName);

        govUkNotify
                .sendEmailNotificationWillReturnSuccess(externalReference)
                .getNotificationStatusWillReturnSuccess(externalReference);

        context.setNotificationId(notificationId);
        context.setTemplateId(templateId);
        context.setExternalReference(externalReference);
        context.setAttachmentBytes(attachmentBytes);
        context.setCommandJson(load("commands/send-email-with-attachment.json", of(
                "notificationId", notificationId,
                "templateId", templateId,
                "fileUri", fileUri)));
    }

    @Given("a send-email-notification command whose attachment is missing")
    public void a_send_email_notification_command_whose_attachment_is_missing() {
        final String notificationId = randomUUID().toString();
        final String templateId = randomUUID().toString();
        final String fileUri = anAzureBlobFileStore().uriOf("missing-" + notificationId + ".csv");

        context.setNotificationId(notificationId);
        context.setTemplateId(templateId);
        context.setCommandJson(load("commands/send-email-missing-attachment.json", of(
                "notificationId", notificationId,
                "templateId", templateId,
                "fileUri", fileUri)));
    }

    @Given("the originator provides a reply queue")
    public void the_originator_provides_a_reply_queue() {
        context.setReplyQueue(RESULT_QUEUE);
    }

    @When("the gateway processes the command")
    public void the_gateway_processes_the_command() {
        anAsbTestClient().sendToCommandQueue(context.getCommandJson(), context.getReplyQueue());
    }
}
