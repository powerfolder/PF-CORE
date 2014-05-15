/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
package de.dal33t.powerfolder.transfer;

import java.util.Date;

/**
 * Convenient class to limit bandwidth (for example for streams). A
 * BandwidthLimiter starts out with 0 available and doesn't increase that. So it
 * needs some kind of "provider" which sets the amount of available bandwidth.
 * The BandwidthProvider class is an example of such. Instances start with no
 * limit. $Id$
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.3 $
 */
public class BandwidthLimiter {

    public static final long UNLIMITED = -1;

    public static final BandwidthLimiter WAN_OUTPUT_BANDWIDTH_LIMITER =
            new BandwidthLimiter(BandwidthLimiterInfo.WAN_OUTPUT);
    public static final BandwidthLimiter WAN_INPUT_BANDWIDTH_LIMITER =
            new BandwidthLimiter(BandwidthLimiterInfo.WAN_INPUT);
    public static final BandwidthLimiter LAN_OUTPUT_BANDWIDTH_LIMITER =
            new BandwidthLimiter(BandwidthLimiterInfo.LAN_OUTPUT);
    public static final BandwidthLimiter LAN_INPUT_BANDWIDTH_LIMITER =
            new BandwidthLimiter(BandwidthLimiterInfo.LAN_INPUT);

    /**
     * The amount of bandwidth initially set by setAvailable().
     * This is used to create stats and is NOT modified by bandwidth requests.
     */
    private long initialAvailable = UNLIMITED;

    /**
     * The amount of bandwidth remaining.
     */
    private long available = UNLIMITED;

    private final Object monitor = new Object();
    private final BandwidthLimiterInfo id;

    private BandwidthLimiter(BandwidthLimiterInfo id) {
        this.id = id;
    }

    public BandwidthLimiterInfo getId() {
        return id;
    }

    /**
     * Requests bandwidth on a medium. Blocks until bandwidth is available.
     *
     * @param size
     *            the amount requested
     * @return the amount of bandwidth granted.
     */
    public long requestBandwidth(long size)
        throws InterruptedException
    {
    	synchronized (monitor) {
            while (available == 0) {
                monitor.wait();
            }
	        long amount = available < 0 ? size : Math.min(available, size);
	        if (available >= 0) {
	            available -= amount;
	        }
	        return amount;
		}
    }

    /**
     * Sets the amount of available "bandwidth". As a side-effect this call will
     * wake Threads waiting in requestBandwidth().
     *
     * @param amount
     *            the amount to set available. An amount < 0 states that there
     *            is no limit.
     * @return a stat record of how much bandwidth there was initially
     * and how much was left over.
     */
    public BandwidthStat setAvailable(long amount) {
        BandwidthStat bandwidthStat;

        synchronized (monitor) {
            // Create a stat of how much bandwidth there was initially
            // and how much there is now.
            bandwidthStat = new BandwidthStat(new Date(), id,
                    initialAvailable, available);

            // Set the new amount
            initialAvailable = amount;
            available = amount;

            // Let everyone know.
            if (available != 0) {
                monitor.notifyAll();
            }
		}

        return bandwidthStat;
    }

    /**
     * Returns the amount of "bandwidth" available
     *
     * @return the "bandwidth"
     */
    public long getAvailable() {
    	synchronized (monitor) {
            return available;
		}
    }

    /**
     * Returns an amount of "bandwidth" back to the limiter. Called after an
     * invocation of requestBandwidth which didn't use all of the requested
     * bandwidth.
     *
     * @param amount
     */
    public void returnAvailable(int amount) {
    	synchronized (monitor) {
            if (available >= 0) {
                available += amount;
            }
		}
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        BandwidthLimiter that = (BandwidthLimiter) obj;

        if (id != that.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BandwidthLimiter{" +
                "id=" + id +
                '}';
    }
}
