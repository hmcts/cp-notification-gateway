package uk.gov.hmcts.cp.notification.sender;

public final class GovNotifyFailureClassifier {
    private static final int BAD_REQUEST = 400;
    private static final int REQUEST_ENTITY_TOO_LARGE = 413;
    private static final int NON_HTTP = 0;
    private static final String SSL_HANDSHAKE = "SSLHandshakeException";

    private GovNotifyFailureClassifier() {
    }

    public static boolean isTemporary(final int httpStatus, final String message) {
        if (httpStatus == BAD_REQUEST || httpStatus == REQUEST_ENTITY_TOO_LARGE) {
            return false;
        }
        if (httpStatus == NON_HTTP) {
            return message != null && message.contains(SSL_HANDSHAKE);
        }
        return true;
    }
}
