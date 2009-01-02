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
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;

/**
 * Prepares core data as (swing) ui models. e.g. <code>TreeModel</code>
 * <p>
 * 
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderRepositoryModel extends PFUIComponent {


    public FolderRepositoryModel(Controller controller) {
        super(controller);

        // // Register listener
        // getController().getFolderRepository().addFolderRepositoryListener(
        // new MyFolderRepositoryListener());

    }

    // Inizalization **********************************************************

    public void initialize() {

    }

    // Exposing ***************************************************************

    // Helper methods *********************************************************

    /**
     * Synchronizes a folder.
     */
    public void syncFolder(FolderInfo folderInfo) {
        Folder folder = getController().getFolderRepository().getFolder(folderInfo);

        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderPanel(getController(), folder).open();
        } else {

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            // Recommend scan on this
            folder.recommendScanOnNextMaintenance();

            // Now trigger the scan
            getController().getFolderRepository().triggerMaintenance();

            // Trigger file requesting.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folderInfo);
        }
    }

    // Internal code **********************************************************

//    /**
//     * Listens for changes in the folder repository. Changes the prepared model
//     * and fires the appropiate events on the swing models.
//     */
//    private class MyFolderRepositoryListener implements
//        FolderRepositoryListener {
//
//        public void folderRemoved(FolderRepositoryEvent e) {
//        }
//
//        public void folderCreated(FolderRepositoryEvent e) {
//        }
//
//        public void maintenanceStarted(FolderRepositoryEvent e) {
//            updateBlinker(e);
//        }
//
//        public void maintenanceFinished(FolderRepositoryEvent e) {
//            updateBlinker(e);
//        }
//
//        public boolean fireInEventDispatchThread() {
//            return true;
//        }
//
//        private void updateBlinker(FolderRepositoryEvent event) {
//            Folder folder = event.getFolder();
//            if (folder == null) {
//                return;
//            }
//            boolean maintenance = folder.equals(getController()
//                .getFolderRepository().getCurrentlyMaintainingFolder());
//            if (folder.isTransferring() || folder.isScanning() || maintenance) {
//                getUIController().getBlinkManager().addChatBlinking(folder,
//                    Icons.FOLDER);
//            } else {
//                getUIController().getBlinkManager().removeBlinking(folder);
//            }
//        }
    //}
}
