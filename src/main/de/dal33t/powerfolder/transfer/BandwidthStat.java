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
* $Id: BandwidthStat.java 7042 2009-02-27 01:17:24Z harry $
*/
package de.dal33t.powerfolder.transfer;

/**
 * Holds stats for a time unit of bandwidth.
 * Includes the BandwidthLimiter source info,
 * the bandwidth made available at the start of the time unit, and
 * the residual bandwith left at the end of the time unit.
 */
public class BandwidthStat {

    private final BandwidthLimiterInfo info;
    private final long initialBandwidth;
    private final long residualBandwidth;

    public BandwidthStat(BandwidthLimiterInfo info, long initialBandwidth,
                         long residualBandwidth) {
        this.info = info;
        this.initialBandwidth = initialBandwidth;
        this.residualBandwidth = residualBandwidth;
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

    @Override
    public String toString() {
        return "BandwidthStat{" +
                "info=" + info +
                ", initialBandwidth=" + initialBandwidth +
                ", residualBandwidth=" + residualBandwidth +
                '}';
    }
}
