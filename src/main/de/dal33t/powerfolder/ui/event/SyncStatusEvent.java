/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: SyncStatusEvent.java 19992 2012-10-24 23:08:39Z sprajc $
 */
package de.dal33t.powerfolder.ui.event;

/**
 * Basic synchronization status events.
 */
public class SyncStatusEvent {

    public static final SyncStatusEvent NOT_STARTED = new SyncStatusEvent("Not Started");
    public static final SyncStatusEvent NO_FOLDERS = new SyncStatusEvent("No Folders");
    public static final SyncStatusEvent PAUSED = new SyncStatusEvent("Paused");
    public static final SyncStatusEvent SYNCING = new SyncStatusEvent("Syncing");
    public static final SyncStatusEvent SYNC_INCOMPLETE = new SyncStatusEvent("Sync Incomplete");
    public static final SyncStatusEvent SYNCHRONIZED = new SyncStatusEvent("Synchronized");

    private final String description;

    /**
     * Static instances only.
     */
    private SyncStatusEvent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return "SyncStatusEvent{" +
                "description='" + description + '\'' +
                '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SyncStatusEvent that = (SyncStatusEvent) obj;

        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return description != null ? description.hashCode() : 0;
    }
}
