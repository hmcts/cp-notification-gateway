package uk.gov.hmcts.cp.notification.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Component
public class Clock {
    private final java.time.Clock clock;

    public Clock() {
        this(java.time.Clock.systemUTC());
    }

    public Clock(final java.time.Clock clock) {
        this.clock = clock;
    }

    public OffsetDateTime offsetDateTime() {
        return OffsetDateTime.now(clock);
    }

    public ZonedDateTime zonedDateTime() {
        return ZonedDateTime.now(clock);
    }

    public Instant instant() {
        return clock.instant();
    }
}
