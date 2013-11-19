/*
* Copyright 2004 - 2011 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
* $Id: BandwidthStatsRecorder.java 7042 2011-01-24 01:17:24Z harry $
*/
package de.dal33t.powerfolder.transfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Reject;

/**
 * Class to allow the transfer manager to record bandwidth stats.
 */
public class BandwidthStatsRecorder extends PFComponent implements BandwidthStatsListener {

    /**
     * Map of stats by info-hour.
     */
    private final Map<StatKey, StatValue> coalescedStats =
            new HashMap<StatKey, StatValue>();

    /**
     * Constructor.
     *
     * @param controller
     */
    public BandwidthStatsRecorder(Controller controller) {
        super(controller);
        loadStats();
    }

    /**
     * Load stats from file.
     */
    @SuppressWarnings("unchecked")
    private void loadStats() {
        String filename = getController().getConfigName() + ".stats";
        Path file = Controller.getMiscFilesLocation().resolve(filename);
        if (Files.exists(file)) {
            logFiner("Loading stats");
            try (ObjectInputStream inputStream = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file))))
            {
                Map<StatKey, StatValue> stats = (Map<StatKey, StatValue>) inputStream
                    .readObject();
                coalescedStats.putAll(stats);
                logFine("Loaded " + stats.size() + " stats.");
            } catch (IOException e) {
                logSevere("IOException", e);
            } catch (ClassNotFoundException e) {
                logSevere("ClassNotFoundException", e);
            } catch (ClassCastException e) {
                logSevere("ClassCastException", e);
            }
        } else {
            logFine("No stats found - probably first start of PF.");
        }
    }

    public void handleBandwidthStat(BandwidthStat stat) {

        StatKey key = new StatKey(stat.getInfo(), stat.getDate());

        // Synchronize on the map so that we do not get concurrent updates to
        // stats.
        synchronized (coalescedStats) {
            StatValue value = coalescedStats.get(key);

            // Create a new entry if required.
            if (value == null) {
                value = new StatValue();
                coalescedStats.put(key, value);
            }

            // Update the stat data.
            value.update(stat.getInitialBandwidth(),
                    stat.getResidualBandwidth());
        }
    }

    /**
     * Prune stats older than date.
     */
    public void pruneStats(Date date) {

        int prunedCount = 0;
        synchronized (coalescedStats) {
            for (Iterator<StatKey> iterator =
                    coalescedStats.keySet().iterator();
                 iterator.hasNext();) {
                StatKey statKey = iterator.next();
                if (statKey.date.before(date)) {
                    iterator.remove();
                    prunedCount++;
                }
            }
        }
        logFiner("Pruned " + prunedCount + " stats.");
    }

    /**
     * Returns the coalesced stats.
     *
     * @return
     */
    public Set<CoalescedBandwidthStat> getBandwidthStats() {
        synchronized (coalescedStats) {
            Set<CoalescedBandwidthStat> stats = new TreeSet<CoalescedBandwidthStat>();
            for (Map.Entry<StatKey, StatValue> entry :
                    coalescedStats.entrySet()) {
                CoalescedBandwidthStat stat = new CoalescedBandwidthStat(entry.getKey().getDate(),
                        entry.getKey().getInfo(),
                        entry.getValue().getInitial(),
                        entry.getValue().getResidual(),
                        entry.getValue().getPeak(),
                        entry.getValue().getCount());
                stats.add(stat);
            }
            return stats;
        }
    }

    public boolean fireInEventDispatchThread() {
        return false;
    }

    public void persistStats() {
        synchronized (coalescedStats) {
            String filename = getController().getConfigName() + ".stats";
            Path file = Controller.getMiscFilesLocation().resolve(filename);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file))))
            {
                logInfo("There are " + coalescedStats.size() + " stats to persist.");
                outputStream.writeUnshared(coalescedStats);
            } catch (FileNotFoundException e) {
                logSevere("FileNotFoundException", e);
            } catch (IOException e) {
                // PFC-2416
                logWarning("IOException", e);
            }
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    /**
     * Inner class to act as a stat-date key limiter by hour.
     * Note that the date constructor arg is truncated to the nearest hour.
     */
    private static class StatKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private final BandwidthLimiterInfo info;
        private final Date date;

        private StatKey(BandwidthLimiterInfo info, Date date) {
            Reject.ifNull(info, "Info is null");
            Reject.ifNull(date, "Date is null");

            this.info = info;
            this.date = DateUtil.truncateToHour(date);
        }

        public BandwidthLimiterInfo getInfo() {
            return info;
        }

        public Date getDate() {
            return date;
        }

        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            StatKey that = (StatKey) obj;

            if (!date.equals(that.date)) {
                return false;
            }

            if (info != that.info) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result = info.hashCode();
            result = 31 * result + date.hashCode();
            return result;
        }

        public String toString() {
            return "StatKey{" +
                    "info=" + info +
                    ", date=" + date +
                    '}';
        }
    }

    /**
     * Inner class to hold cumulative stat details.
     */
    private static class StatValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private long initial;
        private long residual;
        private long peak;
        private long count;

        public long getInitial() {
            return initial;
        }

        public long getResidual() {
            return residual;
        }

        public long getPeak() {
            return peak;
        }

        public long getCount() {
            return count;
        }

        public void update(long initialValue, long residualValue) {
            // Check > 0 to fix for the initial UNLIMITED (-1) stat values.
            if (initialValue >= 0) {
                initial += initialValue;
            }
            if (residualValue >= 0) {
                residual += residualValue;
            }
            if (initialValue >= 0 && residualValue >= 0) {
                peak = Math.max(peak, initialValue - residualValue);
            }
            count++;
        }

        public String toString() {
            return "StatValue{" +
                    "initial=" + initial +
                    ", residual=" + residual +
                    ", peak=" + peak +
                    ", count=" + count +
                    '}';
        }
    }
}
