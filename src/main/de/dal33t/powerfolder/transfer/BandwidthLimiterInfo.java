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
* $Id: BandwidthLimiterInfo.java 7042 2011-01-27 01:17:24Z harry $
*/
package de.dal33t.powerfolder.transfer;

/**
 * Encapsulates information about the various BandwidthLimiters so that
 * bandwidth stat listeners can get details about the source of the stat.
 * Four instances are available for LAN/WAN output/input.
 */
public enum BandwidthLimiterInfo {

    LAN_OUTPUT("LAN Output", true, false),
    LAN_INPUT("LAN Input", true, true),
    WAN_OUTPUT("WAN Output", false, false),
    WAN_INPUT("WAN Input", false, true);

    private String name;
    private boolean lan;
    private boolean input;

    BandwidthLimiterInfo(String name, boolean lan, boolean input) {
        this.name = name;
        this.lan = lan;
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public boolean isLan() {
        return lan;
    }

    public boolean isInput() {
        return input;
    }

    public String toString() {
        return "BandwidthLimiterInfo{" +
                "name='" + name + '\'' +
                ", lan=" + lan +
                ", input=" + input +
                '}';
    }
}
