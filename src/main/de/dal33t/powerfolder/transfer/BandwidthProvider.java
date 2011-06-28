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

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.event.ListenerSupportFactory;

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
    public static final int AUTO_DEFAULT = 10240; // Default auto rate to 10kb/s

    private long autoDetectLan = AUTO_DEFAULT;
    private long autoDetectWan = AUTO_DEFAULT;

    private final Map<BandwidthLimiter, Long> limits = new WeakHashMap<BandwidthLimiter, Long>();
    private ScheduledExecutorService scheduledES;
    private ScheduledFuture<?> task;
    private final BandwidthStatsListener statListenerSupport = ListenerSupportFactory
            .createListenerSupport(BandwidthStatsListener.class);
    
    public BandwidthProvider(ScheduledExecutorService scheduledES) {
        Reject.ifNull(scheduledES, "ScheduledExecutorService is null");
        this.scheduledES = scheduledES;
    }

    public void start() {
        task = scheduledES.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                synchronized (limits) {
                    for (Map.Entry<BandwidthLimiter, Long> me :
                            limits.entrySet()) {
                        BandwidthLimiter limiter = me.getKey();
                        if (limiter == null) {
                            continue;
                        }

                        // Set new limit and distribute the stat from the
                        // previous period.
                        Long value = me.getValue();
                        Long actualValue;
                        if (value == 0) { // Unlimited
                            // NOTE Unlimited is different in BandwidthLimiter. 
                            actualValue = BandwidthLimiter.UNLIMITED; // -1
                        } else if (value < 0) { // Autodetect
                            if (limiter.getId().isLan()) {
                                actualValue = PERIOD * autoDetectLan / 1000;
                            } else {
                                actualValue = PERIOD * autoDetectWan / 1000;
                            }
                        } else {
                            actualValue = PERIOD * value / 1000;
                        }
                        BandwidthStat stat = limiter.setAvailable(actualValue);
                        statListenerSupport.handleBandwidthStat(stat);
                    }
                }
            }
        }, 0, PERIOD, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * Sets the bps for the given limiter.
     * 
     * @param limiter
     * @param bps
     * the number of bandwidth per second to apply.
     * Zero will set the limiter to unlimited bandwidth.
     * Negative will set the limiter to auto detect.
     */
    public void setLimitBPS(BandwidthLimiter limiter, long bps) {
        synchronized (limits) {
            limits.put(limiter, bps);
        }
        logFiner("Bandwidth limiter initalized, max CPS: " + bps);
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

    public void setAutoDetectLan(long autoDetectLan) {
        this.autoDetectLan = autoDetectLan;
    }

    public void setAutoDetectWan(long autoDetectWan) {
        this.autoDetectWan = autoDetectWan;
    }

    public void addBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }

    public void removeBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }
}
