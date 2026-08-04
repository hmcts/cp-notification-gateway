package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendEmailRequestFactory.aSendEmailRequest;

@ExtendWith(MockitoExtension.class)
class EmailClientFactoryTest {
    @Mock
    private GovNotifyClient govNotifyClient;
    @Mock
    private Office365Client office365Sender;

    @InjectMocks
    private EmailClientFactory senderFactory;

    @Test
    void should_route_a_standard_small_attachment_email_to_gov_notify() {
        final SendEmailRequest request = aSendEmailRequest().build();

        assertThat(senderFactory.selectFor(request)).isSameAs(govNotifyClient);
    }

    @Test
    void should_route_an_email_to_gov_notify_when_the_attachment_is_at_the_2mb_limit() {
        final SendEmailRequest request = aSendEmailRequest()
                .attachment(new byte[EmailClientFactory.GOV_NOTIFY_MAX_ATTACHMENT_2_MB]).build();

        assertThat(senderFactory.selectFor(request)).isSameAs(govNotifyClient);
    }

    @Test
    void should_route_an_email_to_office365_when_the_attachment_exceeds_the_2mb_limit() {
        final SendEmailRequest request = aSendEmailRequest()
                .attachment(new byte[EmailClientFactory.GOV_NOTIFY_MAX_ATTACHMENT_2_MB + 1]).build();

        assertThat(senderFactory.selectFor(request)).isSameAs(office365Sender);
    }
}
