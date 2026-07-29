package uk.gov.hmcts.cp.notification.time;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClockTest {

    private static final Instant FIXED = Instant.parse("2026-07-16T10:15:30Z");

    @Nested
    class FromAFixedClock {

        private final Clock clock = new Clock(java.time.Clock.fixed(FIXED, ZoneOffset.UTC));

        @Test
        void instant_returns_the_configured_instant() {
            assertThat(clock.instant()).isEqualTo(FIXED);
        }

        @Test
        void offset_date_time_is_the_instant_at_utc() {
            assertThat(clock.offsetDateTime()).isEqualTo(OffsetDateTime.ofInstant(FIXED, ZoneOffset.UTC));
            assertThat(clock.offsetDateTime().getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        void zoned_date_time_is_the_instant_at_utc() {
            assertThat(clock.zonedDateTime()).isEqualTo(ZonedDateTime.ofInstant(FIXED, ZoneOffset.UTC));
            assertThat(clock.zonedDateTime().getZone()).isEqualTo(ZoneOffset.UTC);
        }
    }

    @Nested
    class FromTheDefaultConstructor {

        @Test
        void uses_a_utc_system_clock() {
            final Clock systemClock = new Clock();

            assertThat(systemClock.offsetDateTime().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(systemClock.zonedDateTime().getZone()).isEqualTo(ZoneOffset.UTC);
        }
    }
}
