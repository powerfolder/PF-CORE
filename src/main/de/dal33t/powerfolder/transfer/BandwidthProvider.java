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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;

/**
 * A BandwidthProvider can be used to periodically assign BandwidthLimiters a
 * given amount of bandwidth. It uses a one Thread solution to perform this.
 * $Id$
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.5 $
 */
public class BandwidthProvider extends Loggable {

    private long autoDetectUploadRate = -1; // Default to unlimited
    private long autoDetectDownloadRate = 10240; // Default to 10KiB/s

    private final Map<BandwidthLimiter, Long> limits =
            new WeakHashMap<BandwidthLimiter, Long>();
    private ScheduledExecutorService scheduledES;
    private ScheduledFuture<?> task;
    private final BandwidthStatsListener statListenerSupport =
            ListenerSupportFactory.createListenerSupport(
                    BandwidthStatsListener.class);

    public BandwidthProvider(Controller controller) {
        scheduledES = controller.getThreadPool();
        Reject.ifNull(scheduledES, "ScheduledExecutorService is null");
        int autoDetectDownload =
                ConfigurationEntry.AUTO_DETECT_DOWNLOAD.getValueInt(controller);
        if (autoDetectDownload > 0) {
            autoDetectDownloadRate = autoDetectDownload;
        }
        int autoDetectUpload =
                ConfigurationEntry.AUTO_DETECT_UPLOAD.getValueInt(controller);
        if (autoDetectUpload > 0) {
            autoDetectUploadRate = autoDetectUpload;
        }
    }

    // For test case.
    public BandwidthProvider(ScheduledExecutorService scheduledES) {
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
                            // NOTE Unlimited is different value in
                            // BandwidthLimiter 0 <--> -1
                            actualValue = BandwidthLimiter.UNLIMITED; // -1
                        } else if (value < 0) { // Autodetect
                            if (limiter.getId().isLan()) {
                                // Shouldn't be here, autodetect is for WAN only
                                logWarning("Setting autodetect for LAN ??? " +
                                        value);
                                actualValue = BandwidthLimiter.UNLIMITED; // -1
                            } else {
                                if (limiter.getId().isInput()) {
                                    actualValue = autoDetectDownloadRate;
                                } else {
                                    actualValue = autoDetectUploadRate;
                                }
                            }
                        } else {
                            actualValue = value;
                        }
                        BandwidthStat stat = limiter.setAvailable(actualValue);
                        statListenerSupport.handleBandwidthStat(stat);
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
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

    /**
     * Set the autodetect upload rate bytes/second
     *
     * @param autoDetectUploadRate
     */
    public void setAutoDetectUploadRate(long autoDetectUploadRate) {
        this.autoDetectUploadRate = autoDetectUploadRate;
    }

    /**
     * Set the autodetect download rate bytes/second
     *
     * @param autoDetectDownloadRate
     */
    public void setAutoDetectDownloadRate(long autoDetectDownloadRate) {
        this.autoDetectDownloadRate = autoDetectDownloadRate;
    }

    public void addBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }

    public void removeBandwidthStatListener(BandwidthStatsListener listener) {
        ListenerSupportFactory.addListener(statListenerSupport, listener);
    }
}
