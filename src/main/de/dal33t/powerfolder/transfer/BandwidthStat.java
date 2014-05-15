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
* $Id: BandwidthStat.java 7042 2011-01-27 01:17:24Z harry $
*/
package de.dal33t.powerfolder.transfer;

import java.util.Date;

/**
 * Holds raw stats data for a time unit of bandwidth.
 * Includes the date of the event, the BandwidthLimiter source info,
 * the bandwidth made available at the start of the time unit, and
 * the residual bandwith left at the end of the time unit.
 */
public class BandwidthStat implements Comparable<BandwidthStat> {

    private final Date date;
    private final BandwidthLimiterInfo info;
    private final long initialBandwidth;
    private final long residualBandwidth;

    public BandwidthStat(Date date, BandwidthLimiterInfo info, long initialBandwidth,
                         long residualBandwidth) {
        this.date = date;
        this.info = info;
        this.initialBandwidth = initialBandwidth;
        this.residualBandwidth = residualBandwidth;
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

    public String toString() {
        return "BandwidthStat{" +
                "date=" + date +
                ", info=" + info +
                ", initialBandwidth=" + initialBandwidth +
                ", residualBandwidth=" + residualBandwidth +
                '}';
    }

    public int compareTo(BandwidthStat o) {
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

        BandwidthStat that = (BandwidthStat) obj;

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
