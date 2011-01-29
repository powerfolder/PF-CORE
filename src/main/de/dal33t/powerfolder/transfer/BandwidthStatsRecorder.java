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

import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Reject;

import java.util.*;

/**
 * Class to allow the transfer manager to record bandwidth stats.
 */
public class BandwidthStatsRecorder implements BandwidthStatsListener {

    /**
     * Map of stats by info-hour.
     */
    private final Map<StatKey, StatValue> cumulativeStats =
            new HashMap<StatKey, StatValue>();

    public void handleBandwidthStat(BandwidthStat stat) {

        StatKey key = new StatKey(stat.getInfo(), stat.getDate());

        // Synchronize on the map so that we do not get concurrent updates to
        // stats.
        synchronized (cumulativeStats) {
            StatValue value = cumulativeStats.get(key);
            if (value == null) {
                value = new StatValue();
                cumulativeStats.put(key, value);
            }

            // Update the stat data.
            value.update(stat.getInitialBandwidth(),
                    stat.getResidualBandwidth());
        }
    }

    public void pruneStats(Date date) {
        synchronized (cumulativeStats) {
            for (Iterator<StatKey> iterator =
                    cumulativeStats.keySet().iterator();
                 iterator.hasNext();) {
                StatKey statKey = iterator.next();
                if (statKey.date.before(date)) {
                    iterator.remove();
                }
            }
        }
        String s = "";
    }

    public Set<BandwidthStat> getStats() {
        synchronized (cumulativeStats) {
            Set<BandwidthStat> stats = new TreeSet<BandwidthStat>();
            for (Map.Entry<StatKey, StatValue> entry :
                    cumulativeStats.entrySet()) {
                BandwidthStat stat = new BandwidthStat(entry.getKey().getDate(),
                        entry.getKey().getInfo(),
                        entry.getValue().getInitial(),
                        entry.getValue().getResidual());
                stats.add(stat);
            }
            return stats;
        }
    }

    public boolean fireInEventDispatchThread() {
        return false;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    /**
     * Inner class to act as a stat-date key limiter by hour.
     * Note that the date constructor arg is truncated to the nearest hour.
     */
    private static class StatKey {
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
    }

    /**
     * Inner class to hold cumulative stat details.
     */
    private static class StatValue {

        private long initial;
        private long residual;

        public long getInitial() {
            return initial;
        }

        public long getResidual() {
            return residual;
        }

        public void update(long initialValue, long residualValue) {
            initial += initialValue;
            residual += residualValue;
        }
    }
}
