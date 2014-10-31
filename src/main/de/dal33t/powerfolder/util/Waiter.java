/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 *
 * $Id$
 */
package de.dal33t.powerfolder.util;

/**
 * Simple class for waiting some time.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.7 $
 */
public class Waiter {
    private long timeoutTime;
    private long waitTime;
    private boolean interrupted;

    /**
     * Initializes a new waiter which timesout in timeout ms
     *
     * @param timeout
     *            ms to timeout
     */
    public Waiter(long timeout) {
        interrupted = false;
        waitTime = timeout;
        timeoutTime = System.currentTimeMillis() + timeout;
    }

    public long getTimoutTimeMS() {
        return waitTime;
    }

    /**
     * Answers if this waiter is timed-out
     *
     * @return true if timeout
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() > timeoutTime || interrupted;
    }

    /**
     * Waits a short time
     */
    public void waitABit() {
        waitABit(100);
    }

    public static void waitABit(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Waiter was interrupted @ "
                + Thread.currentThread().getName(), e);
        }
    }
}
