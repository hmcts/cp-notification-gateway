package uk.gov.hmcts.cp.notification.blob;

public class DisallowedBlobHostException extends PermanentBlobException {
    private static final long serialVersionUID = 1L;

    public DisallowedBlobHostException(final String message) {
        super(message);
    }
}
