/* $Id: AsynchronMessage.java,v 1.1 2004/10/10 02:35:05 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.message.Message;

/**
 * A Message send command which is executed asynchron.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class AsynchronMessage {
    private Message message;
    private String errorMessage;

    /**
     * Initalizes with a message and an error message which will be displayed or
     * loggen on error
     */
    public AsynchronMessage(Message message, String errorMessage) {
        super();
        this.message = message;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Message getMessage() {
        return message;
    }
}