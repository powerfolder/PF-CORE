/* $Id: ConnectionException.java,v 1.6 2005/11/26 02:27:05 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
@SuppressWarnings("serial")
public class ConnectionException extends Exception
{
    private Object target;

    /**
     * 
     */
    public ConnectionException() {
        super();
    }

    /**
     * @param message
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ConnectionException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Adds the member, where the problem occoured
     * 
     * @param member
     * @return this exception
     */
    public ConnectionException with(Member member) {
        this.target = member;
        return this;
    }

    public ConnectionException with(ConnectionHandler handler) {
        if (handler != null && handler.getMember() != null) {
            target = handler.getMember();
        } else if (handler != null && handler.getIdentity() != null
            && handler.getIdentity().isValid())
        {
            target = handler.getIdentity().member;
        } else {
            target = handler;
        }
        return this;
    }

    /**
     * Shows this error to the user if ui is open
     * 
     * @param controller
     */
    public void show(final Controller controller) {
        if (controller.isUIOpen()) {
            String msg = getMessage();
            if (controller.isVerbose() && getCause() != null) {
                if (target != null) {
                    msg += "\nexception target " + target;
                }
                msg += "\ncaused by\n" + getCause();
            }
            final String message = msg;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(controller.getUIController()
                        .getMainFrame().getUIComponent(), message, Translation
                        .getTranslation("dialog.connection_problem"),
                        JOptionPane.ERROR_MESSAGE);
                }
            });

        }
    }
}