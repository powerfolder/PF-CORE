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

/**
 * Class to allow the transfer manager to record bandwidth stats.
 */
public class BandwidthStatsRecorder implements BandwidthStatsListener {

    public void handleBandwidthStat(BandwidthStat stat) {
        // @todo
    }

    public boolean fireInEventDispatchThread() {
        return false;
    }
}
