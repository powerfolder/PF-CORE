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
package de.dal33t.powerfolder.net;

import java.awt.EventQueue;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
@SuppressWarnings("serial")
public class ConnectionException extends Exception {
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
     * Adds the member, where the problem occurred
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
                && handler.getIdentity().isValid()) {
            target = handler.getIdentity().getMemberInfo();
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
                if (target instanceof Member) {
                    Member m = (Member) target;
                    if (m.getLastProblem() != null) {
                        msg += "\nmessage:\n" + m.getLastProblem().message;
                    }
                }
            }
            final String message = msg;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    DialogFactory.genericDialog(
                            controller,
                            Translation.getTranslation("dialog.connection_problem"),
                            message,
                            controller.isVerbose(), ConnectionException.this);
                }
            });

        }
    }
}