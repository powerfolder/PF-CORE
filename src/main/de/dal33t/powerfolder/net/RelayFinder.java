/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: RelayedConnectionManager.java 12311 2010-05-11 13:40:50Z tot $
 */
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.Member;

/**
 * To retrieve relays
 *
 * @author sprajc
 */
public interface RelayFinder {

    /**
     * @param nodeManager
     *            the nodemanager to search for the relay
     * @return the relay or null if not found
     */
    Member findRelay(NodeManager nodeManager);

}
