package uk.gov.hmcts.cp.notification.sender;

import org.springframework.stereotype.Component;

import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class GovNotifyClient implements EmailClient {
    private static final String MATERIAL_URL = "material_url";
    private static final String CSV_EXTENSION = ".csv";

    private final NotificationClient notificationClient;

    public GovNotifyClient(final NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public SendResult send(final SendEmailRequest request) {
        try {
            final SendEmailResponse response = notificationClient.sendEmail(
                    request.templateId() == null ? null : request.templateId().toString(),
                    request.emailAddress(),
                    personalisation(request),
                    request.notificationId() == null ? null : request.notificationId().toString(),
                    emailReplyToId(request));
            return new SendResult(response.getNotificationId().toString());
        } catch (final NotificationClientException e) {
            throw new GovNotifyException(e.getHttpResult(), e.getMessage(), e);
        }
    }

    public NotificationStatus checkStatus(final String reference) {
        try {
            final Notification notification = notificationClient.getNotificationById(reference);
            return NotificationStatus.fromStatus(notification.getStatus());
        } catch (final NotificationClientException e) {
            throw new GovNotifyException(e.getHttpResult(), e.getMessage(), e);
        }
    }

    private static Map<String, Object> personalisation(final SendEmailRequest request) {
        final Map<String, Object> personalisation = new HashMap<>();
        if (request.personalisation() != null) {
            personalisation.putAll(request.personalisation());
        }
        final byte[] attachment = request.attachment();
        if (attachment != null && attachment.length > 0) {
            try {
                personalisation.put(MATERIAL_URL,
                        NotificationClient.prepareUpload(attachment, isCsv(request.attachmentFilename())));
            } catch (final NotificationClientException e) {
                throw new GovNotifyException(e.getHttpResult(), e.getMessage(), e);
            }
        }
        return personalisation;
    }

    private static boolean isCsv(final String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(CSV_EXTENSION);
    }

    private static String emailReplyToId(final SendEmailRequest request) {
        return request.replyToAddressId() == null ? "" : request.replyToAddressId().toString();
    }
}
