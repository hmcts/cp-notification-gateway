package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovNotifyFailureClassifierTest {

    @Test
    void treats_bad_request_not_found_and_too_large_as_permanent() {
        assertThat(GovNotifyFailureClassifier.isTemporary(400, "bad request")).isFalse();
        assertThat(GovNotifyFailureClassifier.isTemporary(404, "no result found")).isFalse();
        assertThat(GovNotifyFailureClassifier.isTemporary(413, "too large")).isFalse();
    }

    @Test
    void treats_server_rate_limit_and_auth_errors_as_temporary() {
        assertThat(GovNotifyFailureClassifier.isTemporary(500, "server error")).isTrue();
        assertThat(GovNotifyFailureClassifier.isTemporary(429, "too many requests")).isTrue();
        assertThat(GovNotifyFailureClassifier.isTemporary(401, "unauthorised")).isTrue();
        assertThat(GovNotifyFailureClassifier.isTemporary(403, "forbidden")).isTrue();
    }

    @Test
    void treats_a_non_http_error_as_permanent_unless_it_is_an_ssl_handshake_failure() {
        assertThat(GovNotifyFailureClassifier.isTemporary(0, "connection reset")).isFalse();
        assertThat(GovNotifyFailureClassifier.isTemporary(0, null)).isFalse();
        assertThat(GovNotifyFailureClassifier.isTemporary(0, "javax.net.ssl.SSLHandshakeException: ...")).isTrue();
    }
}
