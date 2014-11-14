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

import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.information.downloads.DownloadManagersTableModel;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;

public class TransferManagerModel extends PFUIComponent {
    private TransferManager transferManager;
    private DownloadManagersTableModel downloadManagersTableModel;
    private UploadsTableModel uploadsTableModel;

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

    private DelayedUpdater downloadsValueModelUpdater;
    private DelayedUpdater uploadsValueModelUpdater;

    public TransferManagerModel(TransferManager aTransferManager) {
        super(aTransferManager.getController());
        transferManager = aTransferManager;
        downloadsValueModelUpdater = new DelayedUpdater(getController());
        uploadsValueModelUpdater = new DelayedUpdater(getController());
        downloadManagersTableModel = new DownloadManagersTableModel(this);
        uploadsTableModel = new UploadsTableModel(this);

    }

    /**
     * Initializes the listeners into the <code>TransferManager</code>.
     */
    public void initialize() {
        // Listen on transfer manager
        transferManager.addListener(new MyTransferManagerListener());
        try {
            // Ensure that models are not modified out of EDT.
            UIUtil.invokeAndWaitInEDT(new Runnable() {
                public void run() {
                    uploadsTableModel.initialize();
                    downloadManagersTableModel.initialize();
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        updateDownloadsValueModels0();
        updateUploadsValueModels0();
    }

    // Exposing ***************************************************************

    public TransferManager getTransferManager() {
        return getController().getTransferManager();
    }

    public DownloadManagersTableModel getDownloadsTableModel() {
        return downloadManagersTableModel;
    }

    public UploadsTableModel getUploadsTableModel() {
        return uploadsTableModel;
    }

    /**
     * @param folder
     * @return the visible completed downloads for a folder.
     */
    public int countCompletedDownloads(Folder folder) {
        int downloadCount = downloadManagersTableModel.getRowCount();
        int completedDownloadCount = 0;
        FolderRepository folderRepository = getController()
            .getFolderRepository();
        for (int i = 0; i < downloadCount; i++) {
            DownloadManager dlm = downloadManagersTableModel
                .getDownloadManagerAtRow(i);
            Folder f = dlm.getFileInfo().getFolder(folderRepository);
            if (f == null) {
                continue;
            }
            if (dlm.isCompleted() && f.equals(folder)) {
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
        int downloadCount = downloadManagersTableModel.getRowCount();
        int completedDownloadCount = 0;
        for (int i = 0; i < downloadCount; i++) {
            DownloadManager dlm = downloadManagersTableModel
                .getDownloadManagerAtRow(i);
            if (dlm.isCompleted()) {
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
        int downloadCount = downloadManagersTableModel.getRowCount();
        int activeDownloadCount = 0;
        for (int i = 0; i < downloadCount; i++) {
            DownloadManager dlm = downloadManagersTableModel
                .getDownloadManagerAtRow(i);
            if (dlm.isStarted()) {
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
        return downloadManagersTableModel.getRowCount();
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
     * @return a value model with integer number of completed displayed
     *         downloads.
     */
    public ValueModel getCompletedDownloadsCountVM() {
        return completedDownloadsCountVM;
    }

    private void updateDownloadsValueModels() {
        downloadsValueModelUpdater.schedule(new Runnable() {
            public void run() {
                updateDownloadsValueModels0();
            }
        });
    }

    private void updateDownloadsValueModels0() {

        // Recalculate totals for downloads.
      //  int downloadCount = downloadManagersTableModel.getRowCount();
        int completedDownloadsCount = getTransferManager().countCompletedDownloads();
        int activeDownloadCount = getTransferManager().countActiveDownloads();
        int allDownloadsCount = completedDownloadsCount + activeDownloadCount;


//        for (int i = 0; i < downloadCount; i++) {
//            DownloadManager dlm = downloadManagersTableModel
//                .getDownloadManagerAtRow(i);
//            if (dlm == null) {
//                continue;
//            }
//            if (dlm.isStarted() && !dlm.isCompleted()) {
//                activeDownloadCount++;
//            } else if (dlm.isCompleted()) {
//                completedDownloadsCount++;
//            }
//            allDownloadsCount++;
//        }

        allDownloadsCountVM.setValue(allDownloadsCount);
        activeDownloadsCountVM.setValue(activeDownloadCount);
        completedDownloadsCountVM.setValue(completedDownloadsCount);
    }

    private void updateUploadsValueModels() {
        uploadsValueModelUpdater.schedule(new Runnable() {
            public void run() {
                updateUploadsValueModels0();
            }
        });
    }

    private void updateUploadsValueModels0() {
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
            updateDownloadsValueModels();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateDownloadsValueModels();
        }

        public void pendingDownloadEnqueued(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateUploadsValueModels();
        }

        public boolean fireInEventDispatchThread() {
            // Not required to be executed in EDT, because DelayedUpdater is
            // used.
            return false;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateUploadsValueModels();
        }
    }
}
