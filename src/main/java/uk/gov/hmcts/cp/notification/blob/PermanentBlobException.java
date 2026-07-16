package uk.gov.hmcts.cp.notification.blob;

public class PermanentBlobException extends RuntimeException {
    private final transient Integer statusCode;

    public PermanentBlobException(final String message) {
        this(message, null, null);
    }

    public PermanentBlobException(final String message, final Integer statusCode, final Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
