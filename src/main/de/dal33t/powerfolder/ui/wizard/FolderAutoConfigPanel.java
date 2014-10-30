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
package de.dal33t.powerfolder.ui.wizard;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

/**
 * A panel that actually configures an auto-created folder. Just to give
 * some visualization during the process.
 * Automatically switches to the next panel when succeeded otherwise prints
 * error.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderAutoConfigPanel extends SwingWorkerPanel {

    public FolderAutoConfigPanel(Controller controller) {
        super(controller, null, Translation
                .getTranslation("wizard.create_folder.title"), Translation
                .getTranslation("wizard.create_folder.working"), null);
        setTask(new MyFolderCreateWorker());
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.create_folder.title");
    }

    @Override
    public WizardPanel next() {
        boolean sendInvitations = Boolean.TRUE.equals(
                getWizardContext().getAttribute(
                        SEND_INVIATION_AFTER_ATTRIBUTE));
        if (sendInvitations) {
            return new SendInvitationsPanel(getController());
        } else {
            return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        }
    }

    private class MyFolderCreateWorker implements Runnable {

        public void run() {
            FolderInfo folderInfo =
                    (FolderInfo) getWizardContext().getAttribute(
                            FOLDERINFO_ATTRIBUTE);

            boolean useCloudStorage =
                    (Boolean) getWizardContext().getAttribute(
                            USE_CLOUD_STORAGE);

            SyncProfile syncProfile = (SyncProfile) getWizardContext()
                .getAttribute(SYNC_PROFILE_ATTRIBUTE);

            Folder folder = getController().getFolderRepository().getFolder(
                folderInfo);
            folder.setSyncProfile(syncProfile);
            ServerClient client = getController().getOSClient();
            if (client.isConnected() && client.isLoggedIn()) {
                boolean joined = client.joinedByCloud(folder);
                if (!joined && useCloudStorage) {
                    client.getFolderService().createFolder(folderInfo, null);
                } else if (joined && !useCloudStorage) {
                    client.getFolderService().removeFolder(folderInfo, true,
                            false);
                }
            }
        }
    }
}