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
 * $Id: NewFolderAction.java 5419 2008-09-29 12:18:20Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.WhatToDoPanel;
import de.dal33t.powerfolder.util.Translation;

public class FolderWizardAction extends BaseAction {

    public FolderWizardAction(Controller controller) {
        super("exp.action_folder_wizard", controller);
    }

    public void actionPerformed(ActionEvent e) {
        getController().getUIController().getApplicationModel()
            .getServerClientModel().checkAndSetupAccount();
        if (getController().isBackupOnly()) {
            PFWizard wizard = new PFWizard(getController(),
                Translation.get("wizard.pfwizard.folder_title"));
            wizard.open(WhatToDoPanel.doBackupOption(getController(),
                wizard.getWizardContext()));
        } else {
            PFWizard.openWhatToDoWizard(getController());
        }
    }
}