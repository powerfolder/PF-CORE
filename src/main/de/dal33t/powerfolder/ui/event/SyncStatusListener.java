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
 * $Id: SyncStatusListener.java 19992 2012-10-24 23:08:39Z sprajc $
 */
package de.dal33t.powerfolder.ui.event;

import de.dal33t.powerfolder.event.CoreListener;

/**
 * Listener to basic synchronisation status event changes.
 */
public interface SyncStatusListener extends CoreListener {
    void syncStatusChanged(SyncStatusEvent event);
}
