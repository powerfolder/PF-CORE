package de.dal33t.powerfolder.transfer;

/**
 * Convenient class to limit bandwidth (for example for streams). A
 * BandwidthLimiter starts out with 0 available and doesn't increase that. So it
 * needs some kind of "provider" which sets the amount of available bandwidth.
 * The BandwidthProvider class is an example of such. Instances start with no
 * limit. $Id: BandwidthLimiter.java,v 1.3 2006/03/06 01:19:28 bytekeeper Exp $
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.3 $
 */
public class BandwidthLimiter {
    protected long available = -1;

    /**
     * Requests bandwidth on a medium. Blocks until bandwidth is available.
     * 
     * @param size
     *            the amount requested
     * @return the amount of bandwidth granted.
     */
    public synchronized long requestBandwidth(long size)
        throws InterruptedException
    {
        while (available == 0) {
            wait();
        }
        long amount = available < 0 ? size : Math.min(available, size);
        if (available >= 0) {
            available -= amount;
        }
        return amount;
    }

    /**
     * Sets the amount of available"bandwidth". As a side-effect this call will
     * wake Threads waiting in requestBandwidth().
     * 
     * @param amount
     *            the amount to set available. An amount < 0 states that there
     *            is no limit.
     */
    public synchronized void setAvailable(long amount) {
        available = amount;
        if (available != 0) {
            notifyAll();
        }
    }

    /**
     * Returns the amount of "bandwidth" available
     * 
     * @return the "bandwidth"
     */
    public long getAvailable() {
        return available;
    }

    /**
     * Returns an amount of "bandwidth" back to the limiter. Called after an
     * invocation of requestBandwidth which didn't use all of the requested
     * bandwidth.
     * 
     * @param i
     */
    public synchronized void returnAvailable(int amount) {
        if (available >= 0) {
            available += amount;
        }
    }
}
