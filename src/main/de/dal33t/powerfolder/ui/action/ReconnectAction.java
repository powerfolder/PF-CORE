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
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reconnects to the member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class ReconnectAction extends SelectionBaseAction {

    private static final Logger log = Logger.getLogger(ReconnectAction.class.getName());

    public ReconnectAction(Controller controller, SelectionModel selectionModel) {
        super("reconnect", controller, selectionModel);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof Member) {
            // Enable if not myself
            setEnabled(!((Member) selection).isMySelf());
        }

    }

    public void actionPerformed(ActionEvent e) {
        Object item = getUIController().getControlQuarter().getSelectedItem();
        if (!(item instanceof Member)) {
            return;
        }

        // Build new connect dialog
        final ConnectDialog connectDialog = new ConnectDialog(getController());
        final Member member = (Member) item;

        Runnable connector = new Runnable() {
            public void run() {
                // Open connect dialog if ui is open
                connectDialog.open(member.getNick());

                // Close connection first
                member.shutdown();

                // Now execute the connect
                try {
                    if (!member.reconnect()) {
                        throw new ConnectionException(Translation.getTranslation("dialog.unable_to_connect_to_member", 
                            member.getNick()));
                    }
                } catch (ConnectionException ex) {
                    connectDialog.close();
                    log.log(Level.FINER, "ConnectionException", ex);
                    if (!connectDialog.isCanceled() && !member.isConnected()) {
                        // Show if user didn't canceled
                        ex.show(getController());
                    }
                }

                // Close dialog
                connectDialog.close();
                log.finer("Re-connector thread finished");
            }
        };

        // Start connect in anonymous thread
        new Thread(connector, "Reconnector to " + member.getNick()).start();
    }
}