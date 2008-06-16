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
    protected long available = -1;
    private Object monitor = new Object();

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
     * Sets the amount of available"bandwidth". As a side-effect this call will
     * wake Threads waiting in requestBandwidth().
     * 
     * @param amount
     *            the amount to set available. An amount < 0 states that there
     *            is no limit.
     */
    public void setAvailable(long amount) {
    	synchronized (monitor) {
            available = amount;
            if (available != 0) {
                monitor.notifyAll();
            }
		}
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
     * @param i
     */
    public void returnAvailable(int amount) {
    	synchronized (monitor) {
            if (available >= 0) {
                available += amount;
            }
		}
    }
}
