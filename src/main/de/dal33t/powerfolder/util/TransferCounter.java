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

import java.io.Serializable;
import java.util.Date;

import de.dal33t.powerfolder.message.FileChunk;

/**
 * A helper class to determine process status of a transfer TODO: calculate
 * current CPS
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class TransferCounter implements Serializable {
    private static final long serialVersionUID = 100L;
    private static final long CURRENT_CPS_CALCULATION_PERIOD = 10000;

    private Date since;
    private long bytesTransferred;
    private long bytesAlreadyTransferred;
    private long bytesExpected;

    private Date counter1Since, counter2Since;
    private long counter1Bytes, counter2Bytes;
    private boolean counter1Active;

    public TransferCounter() {

    }

    /**
     * Builds a new transfer counter with a expected transfer range.
     *
     * @param alreadyTransferred
     *            the bytes already transferred
     * @param bytesExcpected
     *            the total bytes to be transferred (e.g. filesize)
     */
    public TransferCounter(long alreadyTransferred, long bytesExcpected) {
        this();
        this.bytesAlreadyTransferred = alreadyTransferred;
        this.bytesExpected = bytesExcpected;
    }

    /**
     * Called when a transfer bound to this counter has been started.
     */
    public synchronized void startedTransfer() {
        counter1Since = new Date();
        counter2Since = new Date();
        counter1Bytes = 0;
        counter2Bytes = 0;
    }

    /**
     * Called when a transfer bound to this counter has stopped
     */
    public synchronized void stoppedTransfer() {
        counter1Since = counter2Since = null;
    }

    /**
     * Returns if a transfer is active on this counter.
     *
     * @return true if a transfer is in progress
     */
    public synchronized boolean isTransferring() {
        return counter1Since != null && counter2Since != null;
    }

    /**
     * Adds the count of bytes to the counter
     *
     * @param chunk
     *            the transferred chunk
     */
    public void chunkTransferred(FileChunk chunk) {
        bytesTransferred(chunk.data.length);
    }

    /**
     * Adds the count of bytes to the counter
     *
     * @param count
     *            the transferred bytes count
     */
    public void bytesTransferred(long count) {
        if (since == null) {
            since = new Date();
        }

        if (!isTransferring()) {
            // Actually startedTransfer should have been called before,
            // but somehow it wasn't. So start it now and ignore the first
            // transferred bytes for the calculations.
            startedTransfer();
        } else {
            counter1Bytes += count;
            counter2Bytes += count;
        }

        this.bytesTransferred += count;
    }

    /**
     * Returns the total transferred bytes till now
     *
     * @return
     */
    public long getBytesTransferred() {
        return bytesTransferred;
    }

    /**
     * Returns the total expected size in bytes
     *
     * @return
     */
    public long getBytesExpected() {
        return bytesExpected;
    }

    /**
     * Calculates the average rate of the transfer in CPS (Bytes per second)
     *
     * @return
     */
    public double calculateAverageCPS() {
        if (since == null) {
            return 0;
        }
        long took = System.currentTimeMillis() - since.getTime();
        return ((double) bytesTransferred) * 1000 / took;
    }

    /**
     * Calculates the average rate of the transfer in KB/s (KiloBytes per
     * second)
     *
     * @return
     */
    public double calculateAverageKBS() {
        return calculateAverageCPS() / 1024;
    }

    /**
     * Calculates the current cps rate
     *
     * @return
     */
    public synchronized double calculateCurrentCPS() {
        if (counter1Since == null || counter1Since == null) {
            return 0;
        }
        long bytes = (counter1Active) ? counter1Bytes : counter2Bytes;
        Date activeSince = (counter1Active) ? counter1Since : counter2Since;
        Date counterSince = (counter1Active) ? counter2Since : counter1Since;

        long took = System.currentTimeMillis() - activeSince.getTime();
        if (System.currentTimeMillis() - counterSince.getTime() > CURRENT_CPS_CALCULATION_PERIOD)
        {
            switchActiveCounter();
        }
        if (took == 0) {
            return 0;
        } else {
            return ((double) bytes) * 1000 / took;
        }
    }

    /**
     * Calculates the current rate in KB/s
     *
     * @return
     */
    public double calculateCurrentKBS() {
        return calculateCurrentCPS() / 1024;
    }

    /**
     * Calculates the completion state, only available if initalized with
     * expected file size. Value between 0 and 100.
     *
     * @return
     */
    public double calculateCompletionPercentage() {
        if (bytesExpected == 0) {
            return 100;
        }

        double bytesTransferredTotal = bytesAlreadyTransferred
            + bytesTransferred;
        return bytesTransferredTotal / bytesExpected * 100;
    }

    /**
     * Calculates the estimated time to complete this counter in milliseconds.
     *
     * @return
     */
    public long calculateEstimatedMillisToCompletion() {
        // TODO: Maybe improve the formula below.
        if (calculateAverageCPS() < 0.00001)
            return 0;
        long result = (long) ((bytesExpected - bytesAlreadyTransferred - bytesTransferred) * 1000 / calculateAverageCPS());
        return result > 0 ? result : 0;
    }

    /**
     * Switches the active counter, and resets it. used for calculating the
     * current cps rate
     */
    private void switchActiveCounter() {
        if (counter1Active) {
            // now use counter 2
            counter1Active = false;
            // and reset counter 1
            counter1Bytes = 0;
            counter1Since.setTime(System.currentTimeMillis());
        } else {
            // now use counter 1
            counter1Active = true;
            // and reset counter 2
            counter2Bytes = 0;
            counter2Since.setTime(System.currentTimeMillis());
        }
    }

    // General ****************************************************************

    public String toString() {
        return "TransferCounter {bytes: "
            + Format.formatBytesShort(getBytesTransferred()) + " current: "
            + Format.formatBytesShort((long) (1024L * calculateCurrentKBS()))
            + "/s}";
    }
}