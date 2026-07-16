package uk.gov.hmcts.cp.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailClientFactory {
    private final GovNotifyClient govNotifyClient;
    @SuppressWarnings("unused")
    private final Office365Client office365Sender;

    public EmailClient createFor(final SendEmailRequest request) {
        return govNotifyClient;
    }
}
