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
 * $Id: OnlineStorageLogInAction.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

import java.awt.event.ActionEvent;

/**
 * Action to log in to online storage.
 */
public class OnlineStorageLogInAction extends BaseAction {

    public OnlineStorageLogInAction(Controller controller) {
        super("action_online_storage_log_in", controller);
    }

    public void actionPerformed(ActionEvent e) {
        PFWizard.openLoginWebServiceWizard(getController(),
                getController().getOSClient());
    }
}
