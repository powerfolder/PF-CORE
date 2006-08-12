/* $Id: Identity.java,v 1.6 2005/11/19 22:33:04 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;

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

    public MemberInfo member;

    // A random magic id, valud for the connection
    public String magicId;

    // uses program version
    public String programVersion = Controller.PROGRAM_VERSION;

    public Identity() {
        // Serialisation constructor
    }

    public Identity(Controller controller, MemberInfo member, String magicId) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        this.member = member;
        this.magicId = magicId;
    }

    /**
     * Answers if this identity is a valid one
     * 
     * @return
     */
    public boolean isValid() {
        return member != null && member.id != null && member.nick != null;
    }

    public String toString() {
        return "Identity: " + member;
    }
}