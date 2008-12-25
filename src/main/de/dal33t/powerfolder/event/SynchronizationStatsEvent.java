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
* $Id: SynchronizationStatsEvent.java 6135 2008-12-24 08:04:17Z harry $
*/
package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.Date;

/**
 * This even type is distributed from the FolderRepository when a change in
 * over all synchronization is detected. If any folders are synchronizing,
 * the FolderRepository calculates the time/date of the longest synchronization
 * estimate. When all folders are up-to-date, the FolderRepository distributes
 * an event with the time/date that all folders were synchronized.
 */
public class SynchronizationStatsEvent extends EventObject {

    private final Date synchronizationDate;
    private final boolean synchronizing;

    public SynchronizationStatsEvent(Object source, Date synchronizationDate,
                                     boolean synchronizing) {
        super(source);
        this.synchronizationDate = synchronizationDate;
        this.synchronizing = synchronizing;
    }

    public Date getSynchronizationDate() {
        return synchronizationDate;
    }

    public boolean isSynchronizing() {
        return synchronizing;
    }
}
