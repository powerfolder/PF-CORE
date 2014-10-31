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
* $Id: CoalescedBandwidthStat.java 7042 2011-01-31 01:17:24Z harry $
*/
package de.dal33t.powerfolder.transfer;

import java.util.Date;

/**
 * Holds coalesced stats for a time unit of bandwidth.
 * Includes the date-hour of the event, the BandwidthLimiter source info,
 * the total bandwidth made available during the period, and
 * the residual bandwith left throughout the period.
 *
 * Used bandwidth, average bandwidth and peak bandwidth are calculated,
 * and a count of th coalesced stats is available.
 */
public class CoalescedBandwidthStat implements Comparable<CoalescedBandwidthStat> {

    private final Date date;
    private final BandwidthLimiterInfo info;
    private final long initialBandwidth;
    private final long residualBandwidth;
    private final long peakBandwidth;
    private final long count;

    public CoalescedBandwidthStat(Date date, BandwidthLimiterInfo info, long initialBandwidth,
                         long residualBandwidth, long peakBandwidth, long count) {
        this.date = date;
        this.info = info;
        this.initialBandwidth = initialBandwidth;
        this.residualBandwidth = residualBandwidth;
        this.peakBandwidth = peakBandwidth;
        this.count = count;
    }

    public Date getDate() {
        return date;
    }

    public BandwidthLimiterInfo getInfo() {
        return info;
    }

    public long getInitialBandwidth() {
        return initialBandwidth;
    }

    public long getResidualBandwidth() {
        return residualBandwidth;
    }

    public long getPeakBandwidth() {
        return peakBandwidth;
    }

    public long getCount() {
        return count;
    }

    public long getUsedBandwidth() {
        return initialBandwidth - residualBandwidth;
    }

    public double getPercentageUsedBandwidth() {
        return initialBandwidth == 0 ? 0.0 :
                100.0 * getUsedBandwidth() / initialBandwidth;
    }

    public double getAverageUsedBandwidth() {
        return count == 0 ? 0.0 : 1.0 * getUsedBandwidth() / count;
    }

    public String toString() {
        return "BandwidthStat{" +
                "date=" + date +
                ", info=" + info +
                ", initialBandwidth=" + initialBandwidth +
                ", residualBandwidth=" + residualBandwidth +
                ", peakBandwidth=" + peakBandwidth +
                ", count=" + count +
                '}';
    }

    public int compareTo(CoalescedBandwidthStat o) {
        if  (date.compareTo(o.date) == 0) {
            return info.compareTo(o.info);
        } else {
            return date.compareTo(o.date);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CoalescedBandwidthStat that = (CoalescedBandwidthStat) obj;

        if (!date.equals(that.date)) {
            return false;
        }

        if (info != that.info) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + info.hashCode();
        return result;
    }
}