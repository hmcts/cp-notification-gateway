package uk.gov.hmcts.cp.notification.sender;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.cp.notification.command.SendEmailCommand;

@Component
@RequiredArgsConstructor
public class EmailSender {
    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);

    private final EmailClientFactory emailClientFactory;

    public SendResult sendEmail(final SendEmailCommand command, final byte[] attachment) {
        final SendEmailRequest request = new SendEmailRequest(
                command.notificationId(),
                command.templateId(),
                command.sendToAddress(),
                command.personalisation(),
                attachment,
                attachment == null ? null : filenameFrom(command.fileUri()),
                command.replyToAddress(),
                command.replyToAddressId());

        final EmailClient client = emailClientFactory.selectFor(request);
        final SendResult result = client.send(request);
        LOG.info("Sent notification {} via {} (reference {})",
                command.notificationId(), client.getClass().getSimpleName(), result.reference());
        return result;
    }

    private static String filenameFrom(final String fileUri) {
        String filename = null;
        if (fileUri != null) {
            final String path = fileUri.split("\\?", 2)[0];
            final int lastSlash = path.lastIndexOf('/');
            filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
        return filename;
    }
}
