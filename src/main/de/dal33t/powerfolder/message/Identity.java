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

import java.util.Calendar;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message which contains information about me.
 * <p>
 * TODO Make a better handshake class.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
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
     * flag to indicate a tunneled connection.
     */
    private boolean tunneled;

    /**
     * If to wait for handshake ack from remote side.
     * 
     * @see HandshakeCompleted
     */
    private boolean acknowledgesHandshakeCompletion;

    // uses program version
    private String programVersion = Controller.PROGRAM_VERSION;

    private Calendar timeGMT = Calendar.getInstance();

    // Supports requests for single parts and filepartsrecords.
    // Earlier this was based on a user setting, but that's wrong since we
    // shouldn't deny the
    // remote side to decide how it wants to download.
    private boolean supportingPartTransfers = true;

    /**
     * If I got interesting pending messages for you. Better keep the
     * connection!
     * <p>
     * TRAC #1124
     */
    private boolean pendingMessages = false;

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
        // the remote side! Currently true if we hava ANY pending messages to be
        // sent. Problem: The remote side cannot be known at the time the
        // identity is created, so we have to use this workaround.
        this.pendingMessages = controller.getTaskManager()
            .hasSendMessageTask();
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

    /**
     * @return true if partial transfers of data are supported
     */
    public boolean isSupportingPartTransfers() {
        return supportingPartTransfers;
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

    public String toString() {
        return "Identity: " + member;
    }

}