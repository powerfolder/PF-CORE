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
package de.dal33t.powerfolder.ui.actionold;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reconnects to the member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class ReconnectAction extends BaseAction {

    private static final Logger log = Logger.getLogger(ReconnectAction.class.getName());

    public ReconnectAction(Controller controller) {
        super("action_reconnect", controller);
    }

    public void actionPerformed(ActionEvent e) {
        Object item = e.getSource();
        if (item == null) {
            log.severe("Attempt to reconnect to null member");
            return;
        }
        if (!(item instanceof MemberInfo)) {
            log.severe("Attempt to reconnect to class " + item.getClass().getName());
            return;
        }

        // Build new connect dialog
        final ConnectDialog connectDialog = new ConnectDialog(getController());
        MemberInfo memberInfo = (MemberInfo) item;
        final Member member = getController().getNodeManager().getNode(memberInfo);

        Runnable connector = new Runnable() {
            public void run() {

                log.info("Openning dialog");
                // Open connect dialog if ui is open
                connectDialog.open(member.getNick());

                log.info("Shutting down " + member.getNick());

                // Close connection first
                member.shutdown();

                log.info("Shut down " + member.getNick());

                // Now execute the connect
                try {
                    log.info("Reconnecting to " + member.getNick());
                    if (!member.reconnect()) {
                        log.info("Failed reconnect to " + member.getNick());
                        throw new ConnectionException(Translation.getTranslation(
                                "dialog.unable_to_connect_to_member",
                            member.getNick()));
                    }
                } catch (ConnectionException ex) {
                    log.log(Level.SEVERE,
                            "Exception reconnecting to " + member.getNick(), ex);
                    connectDialog.close();
                    log.log(Level.FINER, "ConnectionException", ex);
                    if (!connectDialog.isCanceled() && !member.isConnected()) {
                        // Show if user didn't cancel
                        ex.show(getController());
                    }
                }

                // Close dialog
                connectDialog.close();
                log.info("Reconnector thread finished");
            }
        };

        // Start connect in anonymous thread
        new Thread(connector, "Reconnector to " + member.getNick()).start();
    }
}