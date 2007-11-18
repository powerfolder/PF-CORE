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
 * <LI>PRIVATEMODE : Disables public folder sharing. Restricts connectivity
 * to interesting users only.<BR>
 * Actually only connects to friends, users on LAN and people, who are on
 * joined folders.<BR>
 * Further PowerFolder connects to some other users so so the finding of
 * your friends in the network is posible.</LI>
 * <LI>PUBLICMODE : NOT LONGER AVAILABLE<BR>
 * Private folders will always require an Invitation, regardless of the
 * networking mode.</LI>
 * </UL>
 */
public enum NetworkingMode {
    /**
     * Disables public folder sharing. Restricts connectivity to interesting
     * users only.<BR>
     * Actually only connects to friends, users on LAN and people, who are
     * on joined folders.
     */
    PRIVATEMODE,
    /**
     * Connect only to PowerFolder clients in the Local Area Network.<BR>
     * The only connection out will be to the update check site.<BR>
     * Your firewall will maybe detect an outgoing connection to 224.0.0.1
     * or ALL-SYSTEMS.MCAST.NET<BR>
     * We use that to detect the other PowerFolder clients in your LAN.
     */
    LANONLYMODE
}