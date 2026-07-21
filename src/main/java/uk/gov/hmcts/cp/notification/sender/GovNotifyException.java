package uk.gov.hmcts.cp.notification.sender;

public class GovNotifyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    public GovNotifyException(final int httpStatus, final String message, final Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
