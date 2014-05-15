/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: SecurityManager.java 8728 2009-07-26 03:29:38Z tot $
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.event.CoreListener;

/**
 * Changes in security manager.
 *
 * @author sprajc
 */
public interface SecurityManagerListener extends CoreListener {
    /**
     * Fired when the account status on the given node is changed. e.g. logout
     * through disconnect.
     *
     * @param event
     *            the event.
     */
    void nodeAccountStateChanged(SecurityManagerEvent event);
}
