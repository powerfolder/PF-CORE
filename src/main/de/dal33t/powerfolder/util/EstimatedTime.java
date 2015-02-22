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
package de.dal33t.powerfolder.util;


/**
 * Container for time estimation information.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 */
public class EstimatedTime {
    private long deltaTimeMillis;
    private boolean active;

    public EstimatedTime(long deltaTimeMillis, boolean active) {
        this.deltaTimeMillis = deltaTimeMillis;
        this.active = active;
    }

    /**
     * Returns if the time estimation was calculated "in progress"
     * @return
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the estimated time in milliseconds
     * @return
     */
    public long getDeltaTimeMillis() {
        return deltaTimeMillis;
    }

	@Override
	public String toString() {
		if (isActive()) {
			if (deltaTimeMillis < 0) {
				return Translation.get("estimation.unknown");
			} else {
				return Format.formatDeltaTime(deltaTimeMillis);
			}
		}
		return "";
	}
}
