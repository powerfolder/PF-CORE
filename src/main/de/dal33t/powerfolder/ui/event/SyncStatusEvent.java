/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.event;

/**
 * Basic synchronization status events.
 */
public enum SyncStatusEvent {

    PAUSED("Paused"),
    NOT_STARTED("Not Started"),
    NOT_CONNECTED("Not Connected"),
    LOGGING_IN("Logging in"),
    NOT_LOGGED_IN("Not Logged In"),
    NO_FOLDERS("No Folders"),
    SYNCING("Syncing"),
    SYNC_INCOMPLETE("Sync Incomplete"),
    SYNCHRONIZED("Synchronized"),
    WARNING("Warning Notification"),
    INFORMATION("Information Notification");

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
}
