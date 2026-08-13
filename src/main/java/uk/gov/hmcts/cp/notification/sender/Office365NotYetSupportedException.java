package uk.gov.hmcts.cp.notification.sender;

public class Office365NotYetSupportedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Office365NotYetSupportedException(final String message) {
        super(message);
    }
}
