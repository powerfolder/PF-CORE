/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
* $Id: OverallFolderStatEvent.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.event;

import java.util.Date;

/**
 * Event with overall stats about the state of folders.
 */
public class OverallFolderStatEvent {

    /** True if all folders are in sync. */
    private boolean allInSync;

    /**
     * Date all folders were last in sync
     * OR date when all folders are expected to be synchronized.
     */
    private Date syncDate;

    public OverallFolderStatEvent(boolean allInSync, Date syncDate) {
        this.allInSync = allInSync;
        this.syncDate = syncDate;
    }

    public boolean isAllInSync() {
        return allInSync;
    }

    public Date getSyncDate() {
        return syncDate;
    }
}
