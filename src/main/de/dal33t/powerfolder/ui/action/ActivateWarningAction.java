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
* $Id: ActivateWarningAction.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.WarningEvent;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.model.ReceivedInvitationsModel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.TimerTask;

/**
 * This action activates the runnable in the warning.
 */
public class ActivateWarningAction extends BaseAction {

    public ActivateWarningAction(Controller controller) {
        super("action_activate_warning", controller);
    }

    public void actionPerformed(ActionEvent e) {
        WarningEvent warning = getController().getUIController()
                .getApplicationModel().getWarningsModel().getNextWarning();
        if (warning != null) {
            SwingUtilities.invokeLater(warning.getRunnable());
        }
    }
}