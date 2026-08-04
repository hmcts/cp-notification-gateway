package uk.gov.hmcts.cp.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class Office365Client implements EmailClient {
    @Override
    public SendResult send(final SendEmailRequest request) {
        throw new Office365NotYetSupportedException(
                "Office 365 send route (attachment exceeds Gov.Notify's 2MB limit) is delivered by "
                        + "NG-S10 (FR-018/019); not available in NG-S02");
    }
}
