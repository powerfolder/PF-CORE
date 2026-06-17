/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.security;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
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
