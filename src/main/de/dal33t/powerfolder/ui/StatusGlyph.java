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
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.event.SyncStatusEvent;

/**
 * PFC-3643: the one symbol a sync status shows, in the main window and in the system tray alike.
 */
public enum StatusGlyph {
    SYNCED, SYNCING, PAUSED, WARNING, INFORMATION, SETUP, WAITING;

    /**
     * @param event  the sync status
     * @param client the server client; before the first login, connecting / logging in / login failed show the
     *               setup symbol (the click opens the login)
     * @return the symbol of {@code event}
     */
    public static StatusGlyph of(SyncStatusEvent event, ServerClient client) {
        boolean loginExecuted = client != null && client.isLoginExecuted();
        switch (event) {
            case PAUSED :
                return PAUSED;
            case NOT_STARTED :
            case NO_FOLDERS :
                return SETUP;
            case SYNCING :
            case SYNC_INCOMPLETE :
                return SYNCING;
            case SYNCHRONIZED :
                return SYNCED;
            case WARNING :
                return WARNING;
            case INFORMATION :
                return INFORMATION;
            case NOT_LOGGED_IN :
                return loginExecuted ? WARNING : SETUP;
            case NOT_CONNECTED :
            case LOGGING_IN :
            default :
                return loginExecuted ? WAITING : SETUP;
        }
    }
}
