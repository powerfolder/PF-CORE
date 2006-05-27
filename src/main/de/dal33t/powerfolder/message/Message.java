/* $Id: Message.java,v 1.3 2004/10/10 04:09:07 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.io.Serializable;

/**
 * General superclass for all messages sent from or to another member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 100L;

    public Message() {
    }
}