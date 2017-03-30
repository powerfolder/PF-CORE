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
 * $Id$
 */
package de.dal33t.powerfolder;

public enum NetworkingMode {
    /**
     * Connects to and synchronizes with server and other clients on LAN and internet.
     */
    PRIVATEMODE,

    /**
     * Connects to and synchronizes with server and other clients on LAN only.
     *
     * Your firewall will maybe detect an outgoing connection to 224.0.0.1 or
     * ALL-SYSTEMS.MCAST.NET<BR>
     * We use that to detect the other PowerFolder clients in your LAN.
     */
    LANONLYMODE,

    /**
     * Connects to and synchronizes with server only. Disables any peer-to-peer connections.
     */
    SERVERONLYMODE
}