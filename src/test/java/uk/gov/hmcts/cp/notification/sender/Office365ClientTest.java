package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendEmailRequestFactory.aSendEmailRequest;

class Office365ClientTest {

    private final Office365Client sender = new Office365Client();

    @Test
    void send_is_not_yet_supported() {
        assertThatThrownBy(() -> sender.send(aSendEmailRequest().build()))
                .isInstanceOf(Office365NotYetSupportedException.class);
    }
}
