package de.dal33t.powerfolder.ui.model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.OverallFolderStatListener;

import javax.swing.*;

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
    private final List<FolderInfo> interestedFolders =
            new ArrayList<FolderInfo>();

    FolderRepositoryModel(Controller controller) {
        super(controller);
        folderListener = new MyFolderListener();
        overallFolderStatListenerSupport = ListenerSupportFactory
            .createListenerSupport(OverallFolderStatListener.class);

        calculateOverallStats();
        FolderRepository repo = controller.getController()
            .getFolderRepository();
        for (Folder folder : repo.getFolders(true)) {
            folder.addFolderListener(folderListener);
        }
        controller.getController().getFolderRepository()
            .addFolderRepositoryListener(new MyFolderRepositoryListener());
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
        int count = 0;
        double localOverallSyncPercentage = 0;
        for (Folder folder :
                getController().getFolderRepository().getFolders(true)) {

            if (folder.isSyncing()) {
                localSyncing = true;
            }
            Date tmpLastSync = folder.getLastSyncDate();
            Date tmpEstimatedDate = folder.getStatistic().getEstimatedSyncDate();

            if (tmpLastSync != null) {
                if (localLastSyncDate == null || tmpLastSync.after(localLastSyncDate)) {
                    localLastSyncDate = tmpLastSync;
                }
            }

            if (tmpEstimatedDate != null) {
                if (localEstimatedSyncDate == null ||
                        tmpEstimatedDate.after(localEstimatedSyncDate)) {
                    localEstimatedSyncDate = tmpEstimatedDate;
                }
            }
            double syncPercentage =
                    folder.getStatistic().getHarmonizedSyncPercentage();
            if (syncPercentage > 0) {
                localOverallSyncPercentage += syncPercentage;
                count++;
            }
        }

        if (count == 0) {
            localOverallSyncPercentage = 0;
        } else {
            localOverallSyncPercentage /= count;
        }

        // Upate with the lastest values.
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

    private class MyFolderListener implements FolderListener {

        public void statisticsCalculated(FolderEvent folderEvent) {
            calculateOverallStats();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(final FolderEvent folderEvent) {
            FolderInfo folderInfo = folderEvent.getFolder().getInfo();
            synchronized (interestedFolders) {
                if (interestedFolders.contains(
                        folderInfo)) {
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

        public void fileChanged(FolderEvent folderEvent) {
        }

        public void filesDeleted(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

}
