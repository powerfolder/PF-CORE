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

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Translation;

/**
 * Asks for ip and tries to connect
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class ConnectAction extends BaseAction {

    public ConnectAction(Controller controller) {
        super("connect", controller);
    }

    public void actionPerformed(ActionEvent e) {
        Object input = JOptionPane.showInputDialog(getUIController()
            .getMainFrame().getUIComponent(),
            Translation.getTranslation("connect.dialog.text"),
            Translation.getTranslation("connect.dialog.title"),
            JOptionPane.QUESTION_MESSAGE, null, null, getController()
                .getPreferences().get("input.lastconnect", ""));
        if (StringUtils.isBlank((String) input)) {
            return;
        }

        // Build new connect dialog
        final ConnectDialog connectDialog = new ConnectDialog(getController());
        final String conStr = input.toString();
        getController().getPreferences().put("input.lastconnect", conStr);
        
        Runnable connector = new Runnable() {
            public void run() {
                // Open connect dialog if ui is open
                connectDialog.open(conStr);

                // Now execute the connect
                try {
                    getController().connect(conStr);
                } catch (ConnectionException ex) {
                    connectDialog.close();
                    Loggable.logFinerStatic(ConnectAction.class, ex);
                    if (!connectDialog.isCanceled()) {
                        // Show if user didn't canceled
                        ex.show(getController());
                    }
                }

                // Close dialog
                connectDialog.close();
                Loggable.logFinerStatic(ConnectAction.class,
                        "Connector thread finished");
            }
        };

        // Start connect in anonymous thread
        new Thread(connector, "Connector to " + conStr).start();
    }
}