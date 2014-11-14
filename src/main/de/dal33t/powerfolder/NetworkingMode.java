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

/**
 * One of 3 networking modes.
 * <UL>
 * <LI>LANONLYMODE : Connect only to PowerFolder clients in the Local Area
 * Network.<BR>
 * The only connection out will be to the update check site.<BR>
 * Your firewall will maybe detect an outgoing connection to 224.0.0.1 or
 * ALL-SYSTEMS.MCAST.NET<BR>
 * We use that to detect the other PowerFolder clients in your LAN.</LI>
 * <LI>PRIVATEMODE : Disables public folder sharing. Restricts connectivity to
 * interesting users only.<BR>
 * Actually only connects to friends, users on LAN and people, who are on joined
 * folders.<BR>
 * Further PowerFolder connects to some other users so so the finding of your
 * friends in the network is posible.</LI>
 * <LI>PUBLICMODE : NO LONGER AVAILABLE<BR>
 * Private folders will always require an Invitation, regardless of the
 * networking mode.</LI>
 * </UL>
  *
 * The names are used as configuration entry values, so should not be changed.
 */
public enum NetworkingMode {
    /**
     * Disables public folder sharing. Restricts connectivity to interesting
     * users only.<BR>
     * Actually only connects to friends, users on LAN and people, who are on
     * joined folders.
     */
    PRIVATEMODE,

    /**
     * Restricts connectivity to the server(s) only. This actually disables any
     * peer-to-peer traffic and keeps the connection to the set server(s) only.
     */
    SERVERONLYMODE,

    /**
     * Connect only to PowerFolder clients in the Local Area Network.<BR>
     * The only connection out will be to the update check site.<BR>
     * Your firewall will maybe detect an outgoing connection to 224.0.0.1 or
     * ALL-SYSTEMS.MCAST.NET<BR>
     * We use that to detect the other PowerFolder clients in your LAN.
     */
    LANONLYMODE
}