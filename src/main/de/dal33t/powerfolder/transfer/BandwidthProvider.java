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

import java.util.Map;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * A BandwidthProvider can be used to periodically assign BandwidthLimiters a
 * given amount of bandwidth. It uses a one Thread solution to perform this.
 * $Id$
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.5 $
 */
public class BandwidthProvider extends Loggable {

    // ms between bandwidth "pushs"
    public static final int PERIOD = 1000;

    private final Map<BandwidthLimiter, Long> limits = new WeakHashMap<BandwidthLimiter, Long>();
    private ScheduledExecutorService scheduledES;
    private ScheduledFuture<?> task;
    private final BandwidthStatsListener statListenerSupport = ListenerSupportFactory
        .createListenerSupport(BandwidthStatsListener.class);

    public BandwidthProvider() {
        scheduledES = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        task = scheduledES.scheduleWithFixedDelay(() -> {
            synchronized (limits) {
                for (Map.Entry<BandwidthLimiter, Long> me : limits.entrySet()) {
                    BandwidthLimiter limiter = me.getKey();
                    if (limiter == null) {
                        continue;
                    }

                    // Set new limit and distribute the stat from the
                    // previous period.
                    Long value = me.getValue();
                    BandwidthStat stat = limiter.setAvailable(value > 0
                        ? PERIOD * value / 1000
                        : BandwidthLimiter.UNLIMITED);
                    statListenerSupport.handleBandwidthStat(stat);
                }
            }
        } , 0, PERIOD, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel(true);
        }
        scheduledES.shutdownNow();
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
            logFiner("Bandwidth limiter " + limiter + " initalized, max CPS: " + bps);
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

    public void addBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }

    public void removeBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }
}
