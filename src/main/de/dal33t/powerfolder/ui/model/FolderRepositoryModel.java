package de.dal33t.powerfolder.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.OverallFolderStatListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.PFUIComponent;

public class FolderRepositoryModel extends PFUIComponent {

    private final MyFolderListener folderListener;
    private final OverallFolderStatListener overallFolderStatListenerSupport;

    private boolean syncing = false;
    private Date lastSyncDate = null;
    private Date estimatedSyncDate = null;
    private double overallSyncPercentage = 0;

    /**
     * List of folders where the user has requested a scan. Used to advise the
     * UI when the next scan arrives so that the user can be notified.
     */
    private final List<FolderInfo> interestedFolders = new ArrayList<FolderInfo>();

    FolderRepositoryModel(Controller controller) {
        super(controller);
        folderListener = new MyFolderListener();
        overallFolderStatListenerSupport = ListenerSupportFactory
            .createListenerSupport(OverallFolderStatListener.class);

        calculateOverallStats();
        FolderRepository repo = controller.getFolderRepository();
        for (Folder folder : repo.getFolders(true)) {
            folder.addFolderListener(folderListener);
        }
        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        controller.getTransferManager().addListener(
            new MyTransferManagerListener());

        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    public void addOverallFolderStatListener(OverallFolderStatListener listener)
    {
        ListenerSupportFactory.addListener(overallFolderStatListenerSupport,
            listener);
    }

    public void removeOverallFolderStatListener(
        OverallFolderStatListener listener)
    {
        ListenerSupportFactory.removeListener(overallFolderStatListenerSupport,
            listener);
    }

    public boolean isSyncing() {
        return syncing;
    }

    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    public Date getEstimatedSyncDate() {
        return estimatedSyncDate;
    }

    /**
     * @return -1 for not known yet
     */
    public double getOverallSyncPercentage() {
        return overallSyncPercentage;
    }

    /**
     * When a folder updates its stats, check all folders to get whether any
     * folders are syncing and what the greatest estimated / last sync date is.
     */
    private void calculateOverallStats() {

        // Find the folder with the most recent synchronized date / most
        // future estimated date.
        boolean localSyncing = false;
        Date localLastSyncDate = null;
        Date localEstimatedSyncDate = null;
        double localOverallSyncPercentage = 0;
        long totalSize = 0;
        for (Folder folder : getController().getFolderRepository().getFolders(
            true))
        {
            if (folder.isSyncing()) {
                localSyncing = true;
            }
            Date tmpLastSync = folder.getLastSyncDate();
            Date tmpEstimatedDate = folder.getStatistic()
                .getEstimatedSyncDate();

            if (tmpLastSync != null) {
                if (localLastSyncDate == null
                    || tmpLastSync.after(localLastSyncDate))
                {
                    localLastSyncDate = tmpLastSync;
                }
            }

            if (tmpEstimatedDate != null) {
                if (localEstimatedSyncDate == null
                    || tmpEstimatedDate.after(localEstimatedSyncDate))
                {
                    localEstimatedSyncDate = tmpEstimatedDate;
                }
            }
            double syncPercentage = folder.getStatistic()
                .getHarmonizedSyncPercentage();
            if (syncPercentage > 0) {
                totalSize += folder.getStatistic().getTotalSize();
                localOverallSyncPercentage += syncPercentage
                    * folder.getStatistic().getTotalSize();
            }
        }

        if (totalSize == 0) {
            localOverallSyncPercentage = 0;
        } else {
            localOverallSyncPercentage /= totalSize;
        }
        if (!getController().getOSClient().getServer().isCompletelyConnected())
        {
            localOverallSyncPercentage = -1;
        }

        // Update with the latest values.
        syncing = localSyncing;
        lastSyncDate = localLastSyncDate;
        estimatedSyncDate = localEstimatedSyncDate;
        overallSyncPercentage = localOverallSyncPercentage;

        // Let everyone know.
        overallFolderStatListenerSupport.statCalculated();
    }

    public void addInterestedFolderInfo(FolderInfo info) {
        synchronized (interestedFolders) {
            interestedFolders.add(info);
        }
    }

    public void removeInterestedFolderInfo(FolderInfo info) {
        synchronized (interestedFolders) {
            interestedFolders.remove(info);
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderCreated(FolderRepositoryEvent e) {
            e.getFolder().addFolderListener(folderListener);

        }

        public void folderRemoved(FolderRepositoryEvent e) {
            e.getFolder().removeFolderListener(folderListener);
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            calculateOverallStats();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            calculateOverallStats();
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }

    private class MyFolderListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            calculateOverallStats();
        }

        public void scanResultCommited(final FolderEvent folderEvent) {
            if (folderEvent.getScanResult().isChangeDetected()) {
                calculateOverallStats();
            }
            FolderInfo folderInfo = folderEvent.getFolder().getInfo();
            synchronized (interestedFolders) {
                if (interestedFolders.contains(folderInfo)) {
                    // Give user feedback on this scan result.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getUIController().scanResultCreated(
                                folderEvent.getScanResult());
                        }
                    });
                    interestedFolders.remove(folderInfo);
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {
        private TransferManager tm;

        public MyTransferManagerListener() {
            super();
            this.tm = getController().getTransferManager();
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

        public void downloadRequested(TransferManagerEvent event) {
        }

        public void downloadQueued(TransferManagerEvent event) {
        }

        public void downloadStarted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void downloadAborted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void downloadBroken(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
        }

        public void pendingDownloadEnqueued(TransferManagerEvent event) {
        }

        public void uploadRequested(TransferManagerEvent event) {
        }

        public void uploadStarted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void uploadAborted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void uploadBroken(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            calculateOverallStats();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        @Override
        public void nodeConnecting(NodeManagerEvent e) {
            if (e.getNode().hasJoinedAnyFolder()) {
                calculateOverallStats();
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().hasJoinedAnyFolder()) {
                calculateOverallStats();
            }
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }
}
