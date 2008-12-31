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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.downloads.DownloadsTableModel;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;

public class TransferManagerModel extends PFUIComponent {
    private TransferManager transferManager;
    private DownloadsTableModel downloadsTableModel;
    private UploadsTableModel uploadsTableModel;
    private ValueModel downloadsAutoCleanupModel;
    private ValueModel uploadsAutoCleanupModel;

    /** Value model with integer number of all displayed downloads. */
    private final ValueModel allDownloadsCountVM = new ValueHolder();

    /** Value model with integer number of all displayed uploads. */
    private final ValueModel allUploadsCountVM = new ValueHolder();

    /** Value model with integer number of active displayed uploads. */
    private final ValueModel activeUploadsCountVM = new ValueHolder();

    /** Value model with integer number of active displayed downloads. */
    private final ValueModel activeDownloadsCountVM = new ValueHolder();

    /** Value model with integer number of completed displayed uploads. */
    private final ValueModel completedUploadsCountVM = new ValueHolder();

    /** Value model with integer number of completed displayed uploads. */
    private final ValueModel completedDownloadsCountVM = new ValueHolder();

    public TransferManagerModel(TransferManager transferManager) {
        super(transferManager.getController());
        this.transferManager = transferManager;
        downloadsAutoCleanupModel = new ValueHolder();
        downloadsAutoCleanupModel
            .setValue(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                .getValueBoolean(getController()));
        downloadsTableModel = new DownloadsTableModel(this);
        uploadsAutoCleanupModel = new ValueHolder();
        uploadsAutoCleanupModel
            .setValue(ConfigurationEntry.UPLOADS_AUTO_CLEANUP
                .getValueBoolean(getController()));
        uploadsTableModel = new UploadsTableModel(this, true);
    }

    /**
     * Initializes the listeners into the <code>TransferManager</code>.
     */
    public void initialize() {
        // Listen on transfer manager
        transferManager.addListener(new MyTransferManagerListener());
        uploadsTableModel.initialize();
        downloadsTableModel.initialize();
        updateDownloadsTreeNode();
        updateUploadsTreeNode();
    }

    // Exposing ***************************************************************

    public TransferManager getTransferManager() {
        return getController().getTransferManager();
    }

    public DownloadsTableModel getDownloadsTableModel() {
        return downloadsTableModel;
    }

    public ValueModel getDownloadsAutoCleanupModel() {
        return downloadsAutoCleanupModel;
    }

    public ValueModel getUploadsAutoCleanupModel() {
        return uploadsAutoCleanupModel;
    }

    public UploadsTableModel getUploadsTableModel() {
        return uploadsTableModel;
    }

    /**
     * Count the visible completed downloads for a folder.
     * 
     * @param folder
     * @return
     */
    public int countCompletedDownloads(Folder folder) {
        int downloadCount = downloadsTableModel.getRowCount();
        int completedDownloadCount = 0;
        FolderRepository folderRepository = getController()
            .getFolderRepository();
        for (int i = 0; i < downloadCount; i++) {
            Download dl = downloadsTableModel.getDownloadAtRow(i);
            Folder f = dl.getFile().getFolder(folderRepository);
            if (f == null) {
                continue;
            }
            if (dl.isCompleted() && f.equals(folder)) {
                completedDownloadCount++;
            }
        }
        return completedDownloadCount;
    }

    /**
     * Count visible completed downloads.
     * 
     * @return
     */
    public int countCompletedDownloads() {
        int downloadCount = downloadsTableModel.getRowCount();
        int completedDownloadCount = 0;
        for (int i = 0; i < downloadCount; i++) {
            Download dl = downloadsTableModel.getDownloadAtRow(i);
            if (dl.isCompleted()) {
                completedDownloadCount++;
            }
        }
        return completedDownloadCount;
    }

    /**
     * Count visible active downloads.
     * 
     * @return
     */
    public int countActiveDownloads() {
        int downloadCount = downloadsTableModel.getRowCount();
        int activeDownloadCount = 0;
        for (int i = 0; i < downloadCount; i++) {
            Download dl = downloadsTableModel.getDownloadAtRow(i);
            if (dl.isPending() || dl.isQueued()) {
                activeDownloadCount++;
            }
        }
        return activeDownloadCount;
    }

    /**
     * Count total visible downloads.
     * 
     * @return
     */
    public int countTotalDownloads() {
        return downloadsTableModel.getRowCount();
    }

    /**
     * Returns a value model with integer number of active displayed uploads.
     *
     * @return
     */
    public ValueModel getActiveUploadsCountVM() {
        return activeUploadsCountVM;
    }

    /**
     * Returns a value model with integer number of active displayed downloads.
     *
     * @return
     */
    public ValueModel getActiveDownloadsCountVM() {
        return activeDownloadsCountVM;
    }

    /**
     * Returns a value model with integer number of all displayed downloads.
     *
     * @return
     */
    public ValueModel getAllDownloadsCountVM() {
        return allDownloadsCountVM;
    }

    /**
     * Returns a value model with integer number of all displayed uploads.
     *
     * @return
     */
    public ValueModel getAllUploadsCountVM() {
        return allUploadsCountVM;
    }

    /**
     * Returns a value model with integer number of completed displayed uploads.
     *
     * @return
     */
    public ValueModel getCompletedUploadsCountVM() {
        return completedUploadsCountVM;
    }

    /**
     * Returns a value model with integer number of completed displayed downloads.
     *
     * @return
     */
    public ValueModel getCompletedDownloadsCountVM() {
        return completedDownloadsCountVM;
    }

    private void updateDownloadsTreeNode() {

        // Recalculate totals for downloads.
        int downloadCount = downloadsTableModel.getRowCount();
        int allDownloadsCount = 0;
        int activeDownloadCount = 0;
        int completedDownloadsCount = 0;
        for (int i = 0; i < downloadCount; i++) {
            Download dl = downloadsTableModel.getDownloadAtRow(i);
            if (dl.isStarted() && !dl.isCompleted() && !dl.isBroken())
            {
                activeDownloadCount++;
            } else if (dl.isCompleted()) {
                completedDownloadsCount++;
            }
            allDownloadsCount++;
        }

        allDownloadsCountVM.setValue(allDownloadsCount);
        activeDownloadsCountVM.setValue(activeDownloadCount);
        completedDownloadsCountVM.setValue(completedDownloadsCount);
    }

    private void updateUploadsTreeNode() {

        // Recalculate total and active uploads.
        int uploadCount = uploadsTableModel.getRowCount();
        int activeUploadCount = 0;
        int allUploadCount = 0;
        int completedUploadCount = 0;
        for (int i = 0; i < uploadCount; i++) {
            Upload ul = uploadsTableModel.getUploadAtRow(i);
            if (ul.isStarted() && !ul.isCompleted() && !ul.isBroken()
                && !ul.isAborted())
            {
                activeUploadCount++;
            } else if (ul.isCompleted()) {
                completedUploadCount++;
            }
            allUploadCount++;
        }

        allUploadsCountVM.setValue(allUploadCount);
        activeUploadsCountVM.setValue(activeUploadCount);
        completedUploadsCountVM.setValue(completedUploadCount);
    }

    // Inner classes **********************************************************

    /**
     * Listens on transfermanager and fires change events on tree
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener implements TransferManagerListener {

         public void downloadRequested(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        private void updateFolderTreeNode(TransferManagerEvent event) {
            Folder folder = event.getFile().getFolder(
                getController().getFolderRepository());
            if (folder == null) {
                return;
            }
            if (folder.isTransferring()) {
                getUIController().getBlinkManager().addChatBlinking(folder,
                    Icons.FOLDER);
            } else {
                getUIController().getBlinkManager().removeBlinking(folder);
            }
        }
    }
}
