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
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.util.Calendar;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message which contains information about me.
 *
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class Identity extends Message {
    private static final long serialVersionUID = 101L;

    private MemberInfo member;

    // A random magic id, valud for the connection
    private String magicId;

    /** Flag which indicates that encryption is supported. */
    private boolean supportsEncryption;

    /**
     * #2366: For server only. Since v3.5.13
     */
    private boolean supportsQuickLogin = true;

    /**
     * flag to indicate a tunneled connection.
     */
    private boolean tunneled;

    /**
     * If to wait for handshake ack from remote side.
     *
     * @see HandshakeCompleted
     */
    private boolean acknowledgesHandshakeCompletion;

    /**
     * Supports Request/Response pattern with serialized arguments. To avoid
     * problems when class model differs between client and server.
     */
    private boolean supportsSerializedRequest = true;

    // uses program version. ATTENTION: NEVER MARK THESE FINAL!!!!!
    private String programVersion = Controller.PROGRAM_VERSION;

    private Calendar timeGMT = Calendar.getInstance();

    // Supports requests for single parts and filepartsrecords.
    // Earlier this was based on a user setting, but that's wrong since we
    // shouldn't deny the
    // remote side to decide how it wants to download.
    // Leftover for semi-old clients
    private boolean supportingPartTransfers = true;

    private Boolean useCompressedStream;

    /**
     * #2072: {@link Externalizable} protocol history:
     * <p>
     * 100: Initial version. Added: {@link RequestPartExt}
     * <p>
     * 101: Added: {@link StopUploadExt}
     * <p>
     * 102: Added: {@link StartUploadExt}
     * <p>
     * 103: Added: {@link RequestDownloadExt}
     * <p>
     * 104: Added: {@link FileChunkExt}
     * <p>
     * 105: Added: {@link FileListExt}
     * <p>
     * 106: Added: {@link FolderListExt}
     * <p>
     * 107: Added: {@link KnownNodesExt}
     * <p>
     * 108: Added: {@link RelayedMessageExt}
     */
    private int protocolVersion = 108;

    private boolean requestFullFolderlist;

    /**
     * If I got interesting pending messages for you. Better keep the
     * connection!
     * <p>
     * TRAC #1124
     */
    private boolean pendingMessages = false;

    private String configurationURL;

    public Identity() {
        // Serialisation constructor
    }

    public Identity(Controller controller, MemberInfo myself, String magicId,
        boolean supportsEncryption, boolean tunneled, ConnectionHandler handler)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(myself, "Member is null");
        this.member = myself;
        this.magicId = magicId;
        this.supportsEncryption = supportsEncryption;
        this.tunneled = tunneled;

        // Always true for newer versions #559
        this.acknowledgesHandshakeCompletion = true;
        // #1124: HACK ALERT. This should only be true, if we have messages for
        // the remote side! Currently true if we have ANY pending messages to be
        // sent. Problem: The remote side cannot be known at the time the
        // identity is created, so we have to use this workaround.
        this.pendingMessages = controller.getTaskManager().hasSendMessageTask();

        boolean useZIPonLAN = ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(controller);
        this.useCompressedStream = !handler.isOnLAN()
            || (handler.isOnLAN() && useZIPonLAN);
        // #2569
        this.requestFullFolderlist = controller.getMySelf().isServer();

        this.configurationURL = controller.getConfig().getProperty("config.url");
    }

    /**
     * @return true if this identity is a valid one
     */
    public boolean isValid() {
        return member != null && member.id != null && member.nick != null;
    }

    /**
     * @return the magic id.
     */
    public String getMagicId() {
        return magicId;
    }

    /**
     * @return the remote member info.
     */
    public MemberInfo getMemberInfo() {
        return member;
    }

    /**
     * @return the program version of the remote side.
     */
    public String getProgramVersion() {
        return programVersion;
    }

    /**
     * @return true if encrypted transfer are supported
     */
    public boolean isSupportsEncryption() {
        return supportsEncryption;
    }

    public boolean isSupportingPartTransfers() {
        return supportingPartTransfers;
    }

    public boolean isSupportsQuickLogin() {
        return supportsQuickLogin;
    }

    /**
     * @return true, if the stream is compressed; false, if the stream is not
     *         compressed; null, if we don't know
     */
    public Boolean isUseCompressedStream() {
        return useCompressedStream;
    }

    /**
     * #2569: Connection improvement: Don't send full folderlist from server to
     * client.
     *
     * @return
     */
    public boolean isRequestFullFolderlist() {
        return requestFullFolderlist;
    }

    /**
     * @return true if this is a tunneled connection.
     */
    public boolean isTunneled() {
        return tunneled;
    }

    /**
     * @return true if the remote side sends a <code>HandshakeCompleted</code>
     *         message after successfull handshake.
     */
    public boolean isAcknowledgesHandshakeCompletion() {
        return acknowledgesHandshakeCompletion;
    }

    public boolean isSupportsSerializedRequest() {
        return supportsSerializedRequest;
    }

    /**
     * @return if this node has interesting messages for you! Keep the
     *         connection.
     */
    public boolean isPendingMessages() {
        return pendingMessages;
    }

    /**
     * @return the current time of an client when it sent this message.
     */
    public Calendar getTimeGMT() {
        return timeGMT;
    }

    /**
     * @return the protocol version for {@link Externalizable}
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getConfigURL() {
        return configurationURL;
    }

    @Override
    public String toString() {
        return "Identity: " + member;
    }
}