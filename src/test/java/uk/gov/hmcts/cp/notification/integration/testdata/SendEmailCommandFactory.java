package uk.gov.hmcts.cp.notification.integration.testdata;

import uk.gov.hmcts.cp.notification.command.SendEmailCommand;

import java.util.UUID;

public final class SendEmailCommandFactory {

    private SendEmailCommandFactory() {
    }

    public static SendEmailCommand.SendEmailCommandBuilder aSendEmailCommand() {
        return SendEmailCommand.builder()
                .notificationId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .sendToAddress("user@example.com")
                .fileUri("https://sa.blob.core.windows.net/mi-reportdata/report.csv");
    }
}
