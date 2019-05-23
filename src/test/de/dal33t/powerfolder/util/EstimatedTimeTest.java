/*
 * Copyright 2004 - 2019 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EstimatedTimeTest {
    private static final int DELTA_TIME_MILLIS = 100;
    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;
    private static final int NEGATIVE_DELTA = -1;
    private EstimatedTime estimatedTime;

    @Test
    public void shouldReturnValues() {
        estimatedTime = new EstimatedTime(DELTA_TIME_MILLIS, ACTIVE);
        assertEquals(DELTA_TIME_MILLIS, estimatedTime.getDeltaTimeMillis());
        assertEquals(ACTIVE,estimatedTime.isActive());
    }

    @Test
    public void testToString(){
        estimatedTime =new EstimatedTime(DELTA_TIME_MILLIS, INACTIVE);
        assertEquals("",estimatedTime.toString());

        estimatedTime =new EstimatedTime(NEGATIVE_DELTA, ACTIVE);
        assertEquals(Translation.get("estimation.unknown"),estimatedTime.toString());

        estimatedTime =new EstimatedTime(DELTA_TIME_MILLIS, ACTIVE);
        assertEquals(Format.formatDeltaTime(DELTA_TIME_MILLIS),estimatedTime.toString());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException(){
        estimatedTime.isActive();
    }
}
