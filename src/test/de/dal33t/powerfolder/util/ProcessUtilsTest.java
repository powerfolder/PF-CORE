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
package de.dal33t.powerfolder.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Test for {@link ProcessUtils#runBounded(long, String...)}.
 */
public class ProcessUtilsTest {

    @Test
    public void testHangingCommandIsBoundedByTimeout() {
        String[] hang = OSUtil.isWindowsSystem()
            ? new String[] { "ping", "-n", "30", "127.0.0.1" }
            : new String[] { "sleep", "30" };
        long start = System.nanoTime();
        String out = ProcessUtils.runBounded(1, hang);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertNull(out, "a timed-out command returns null");
        assertTrue(elapsedMillis < 2500, "returned after " + elapsedMillis + " ms");
    }

    @Test
    public void testShortCommandReturnsOutput() {
        String[] echo = OSUtil.isWindowsSystem()
            ? new String[] { "cmd", "/c", "echo", "hello" }
            : new String[] { "echo", "hello" };
        String out = ProcessUtils.runBounded(5, echo);
        assertNotNull(out);
        assertTrue(out.contains("hello"), out);
    }

    @Test
    public void testUnknownCommandReturnsNull() {
        assertNull(ProcessUtils.runBounded(2, "no-such-command-" + System.nanoTime()));
    }
}
