/* $Id: TextMessage.java,v 1.3 2004/10/04 00:41:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * A Simple textmessage to a user. Nothing more
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class TextMessage extends Message {
    private static final long serialVersionUID = 101L;

    public String text;

    public TextMessage(String text) {
        this.text = text;
    }
}