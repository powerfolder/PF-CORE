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
 * $Id: ActionModel.java 5419 2008-09-29 12:18:20Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import javax.swing.*;

/**
 * Holder of all simple singleton actions.
 */
public class ActionModel extends PFComponent {

    public ActionModel(Controller controller) {
        super(controller);
    }

    private FolderWizardAction folderWizardAction;
    private NewFolderAction newFolderAction;
    private FindComputersAction findComputersAction;

    public NewFolderAction getNewFolderAction() {
        if (newFolderAction == null) {
            newFolderAction = new NewFolderAction(getController());
            newFolderAction.allowWith(FolderCreatePermission.INSTANCE);
            newFolderAction.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());

            getController().getOSClient().addListener(
                    new MyServerClientListener(newFolderAction));

            if (!ConfigurationEntry.SHOW_CREATE_FOLDER
                    .getValueBoolean(getController()))
            {
                newFolderAction.setEnabled(false);
            }
        }
        return newFolderAction;
    }

    public FolderWizardAction getFolderWizardAction() {
        if (folderWizardAction == null) {
            folderWizardAction = new FolderWizardAction(getController());
            folderWizardAction.allowWith(FolderCreatePermission.INSTANCE);
        }
        return folderWizardAction;
    }

    public FindComputersAction getFindComputersAction() {
        if (findComputersAction == null) {
            findComputersAction = new FindComputersAction(getController());
        }
        return findComputersAction;
    }

    private class MyServerClientListener implements ServerClientListener {
        private Action label;

        MyServerClientListener(Action label) {
            this.label = label;
        }

        public void accountUpdated(ServerClientEvent event) {
            label.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());
        }

        public void login(ServerClientEvent event) {
            label.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            label.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());
        }

        public void serverConnected(ServerClientEvent event) {
            label.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());
        }

        public void serverDisconnected(ServerClientEvent event) {
            label.setEnabled(getController().getOSClient()
                    .isAllowedToCreateFolders());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
