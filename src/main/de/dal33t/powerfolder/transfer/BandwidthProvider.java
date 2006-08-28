package de.dal33t.powerfolder.transfer;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.util.Loggable;

/**
 * A BandwidthProvider can be used to periodically assign BandwidthLimiters a
 * given amount of bandwidth. It uses a one Thread solution to perform this.
 * $Id: BandwidthProvider.java,v 1.5 2006/04/23 18:21:18 bytekeeper Exp $
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.5 $
 */
public class BandwidthProvider extends Loggable {
    // ms between bandwidth "pushs"
    public static final int PERIOD = 1000;

    private Map<BandwidthLimiter, Long> limits = new WeakHashMap<BandwidthLimiter, Long>();
    private Timer timer;

    public BandwidthProvider() {
    }

    public void start() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                synchronized (limits) {
                    for (Map.Entry<BandwidthLimiter, Long> me : limits
                        .entrySet())
                    {
                        if (me.getKey() == null)
                            continue;
                        me.getKey().setAvailable(
                            me.getValue() > 0
                                ? PERIOD * me.getValue() / 1000
                                : -1);
                    }
                }
            }
        }, 0, PERIOD);
    }

    public void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Sets the bps for the given limiter.
     * 
     * @param limiter
     * @param bps
     *            the number of bandwidth per second to apply. 0 will set the
     *            limiter to unlimited bandwidth. If you want to stop granting
     *            bandwidth, remove the limiter. If the parameter is negativ,
     *            nothing happens.
     */
    public void setLimitBPS(BandwidthLimiter limiter, long bps) {
        if (bps >= 0) {
            synchronized (limits) {
                limits.put(limiter, bps);
            }
            log().verbose("Bandwidth limiter initalized, max CPS: " + bps);
        }
    }

    /**
     * Returns the limit for a given limiter.
     * 
     * @param limiter
     * @return the bps limit for the given limiter
     */
    public long getLimitBPS(BandwidthLimiter limiter) {
        synchronized (limits) {
            try {
                return limits.get(limiter);
            } catch (NullPointerException npe) {
                return -1;
            }
        }
    }

    /**
     * Removes a limiter from getting bandwidth pushed. (This class only holds
     * weak references to BandwidthLimiters, so even if you don't call this
     * method the limiters get removed as soon as the garbage collector clears
     * them)
     * 
     * @param limiter
     *            the limiter to remove
     */
    public void removeLimiter(BandwidthLimiter limiter) {
        synchronized (limits) {
            limits.remove(limiter);
        }
    }
}
