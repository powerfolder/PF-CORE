package de.dal33t.powerfolder.security;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.Assert.*;

public class OnlineStorageSubscriptionTest {

    private static OnlineStorageSubscription sub(Date from, Date till) {
        OnlineStorageSubscription s = new OnlineStorageSubscription();
        s.setValidFrom(from);
        s.setValidTill(till);
        return s;
    }

    @Test
    public void nullDates_returnTrue() {
        assertTrue(sub(null, null).isServicedMoreThanOneDay());
        assertTrue(sub(new Date(), null).isServicedMoreThanOneDay());
        assertTrue(sub(null, new Date()).isServicedMoreThanOneDay());
    }

    @Test
    public void zeroOrNegativeDuration_returnFalse() {
        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");

        // zero duration
        assertFalse(sub(Date.from(t0), Date.from(t0)).isServicedMoreThanOneDay());

        // negative duration
        assertFalse(sub(Date.from(t0), Date.from(t0.minus(1, ChronoUnit.HOURS)))
                .isServicedMoreThanOneDay());
    }

    @Test
    public void within24h_returnFalse() {
        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");
        Instant tLess = t0.plus(23, ChronoUnit.HOURS)
                .plus(59, ChronoUnit.MINUTES)
                .plus(59, ChronoUnit.SECONDS);
        assertFalse(sub(Date.from(t0), Date.from(tLess)).isServicedMoreThanOneDay());
    }

    @Test
    public void exactly24h_returnFalse() {
        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");
        Instant t24 = t0.plus(24, ChronoUnit.HOURS);
        assertFalse(sub(Date.from(t0), Date.from(t24)).isServicedMoreThanOneDay());
    }

    @Test
    public void over24h_returnTrue() {
        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");
        Instant tOver = t0.plus(24, ChronoUnit.HOURS).plusMillis(1);
        assertTrue(sub(Date.from(t0), Date.from(tOver)).isServicedMoreThanOneDay());
    }
}
