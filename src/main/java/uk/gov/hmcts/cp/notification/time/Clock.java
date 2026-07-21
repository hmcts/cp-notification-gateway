package uk.gov.hmcts.cp.notification.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Component
public class Clock {
    private final java.time.Clock delegate;

    public Clock() {
        this(java.time.Clock.systemUTC());
    }

    public Clock(final java.time.Clock delegate) {
        this.delegate = delegate;
    }

    public OffsetDateTime offsetDateTime() {
        return OffsetDateTime.now(delegate);
    }

    public ZonedDateTime zonedDateTime() {
        return ZonedDateTime.now(delegate);
    }

    public Instant instant() {
        return delegate.instant();
    }
}
