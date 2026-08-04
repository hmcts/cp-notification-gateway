package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GovNotifyConfigTest {
    private static final String BASE_URL = "https://api.notifications.service.gov.uk";
    private static final String VALID_KEY =
            "cpngtest-00000000-0000-0000-0000-000000000000-11111111-1111-1111-1111-111111111111";

    private final GovNotifyConfig config = new GovNotifyConfig();

    @Test
    void fails_to_start_with_an_actionable_message_when_the_api_key_is_blank() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> config.notificationClient(BASE_URL, "  "))
                .withMessageContaining("cp.notification.govnotify.api-key");
    }

    @Test
    void builds_the_client_when_the_api_key_is_present() {
        assertThatCode(() -> config.notificationClient(BASE_URL, VALID_KEY)).doesNotThrowAnyException();
    }
}
