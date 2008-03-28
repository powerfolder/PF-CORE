/* $Id: Identity.java,v 1.6 2005/11/19 22:33:04 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.Calendar;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

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

    // Actually: Will support delta sync on this connection
    private boolean supportingPartTransfers;

    // True if the sender supports sending requests for parts.
    // The value of this variable is meaningless if supportingPartTransfers above is true,
    // in that case it's getter will always return true.
    private boolean supportingPartRequests;

    public Identity() {
        // Serialisation constructor
    }

    public Identity(Controller controller, MemberInfo member, String magicId,
        boolean supportsEncryption, boolean tunneled, ConnectionHandler handler)
    {
        Reject.ifNull(member, "Member is null");
        this.member = member;
        this.magicId = magicId;
        this.supportsEncryption = supportsEncryption;
        this.tunneled = tunneled;
        this.supportingPartTransfers = Util.allowDeltaSync(controller, handler.isOnLAN());
        this.supportingPartRequests = Util.allowPartRequests(controller, handler.isOnLAN());

        // Always true for newer versions #559
        this.acknowledgesHandshakeCompletion = true;
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
     * @return true if requests of single file-chunks is supported.
     */
    public boolean isSupportingPartRequests() {
        return supportingPartRequests || supportingPartTransfers;
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
     * @return the current time of an client when it sent this message.
     */
    public Calendar getTimeGMT() {
        return timeGMT;
    }

    public String toString() {
        return "Identity: " + member;
    }

}