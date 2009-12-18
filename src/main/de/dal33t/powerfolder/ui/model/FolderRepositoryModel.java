package de.dal33t.powerfolder.ui.model;

import java.util.Date;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.OverallFolderStatEvent;
import de.dal33t.powerfolder.event.OverallFolderStatListener;

public class FolderRepositoryModel extends PFUIComponent {

    private final MyFolderListener folderListener;

    private final OverallFolderStatListener overallFolderStatListenerSupport;

    /**
     * Date all folders were last synchronized.
     */
    private Date lastSyncDate;

    /**
     * Most future estimated sync date.
     */
    private Date etaSyncDate;

    /**
     * If syncing at the time the dates where calculated.
     */
    private boolean syncingAtDate;

    FolderRepositoryModel(Controller controller) {
        super(controller);
        folderListener = new MyFolderListener();
        overallFolderStatListenerSupport = ListenerSupportFactory
            .createListenerSupport(OverallFolderStatListener.class);

        calculateOverallStats();
        FolderRepository repo = controller.getController()
            .getFolderRepository();
        for (Folder folder : repo.getFolders()) {
            folder.addFolderListener(folderListener);
        }
        controller.getController().getFolderRepository()
            .addFolderRepositoryListener(new MyFolderRepositoryListener());
    }

    /**
     * @return If was syncing at the time the dates (last sync date / eta) where
     *         calculated.
     */
    public boolean wasSyncingAtDate() {
        return syncingAtDate;
    }

    /**
     * @return Date all folders were last synchronized.
     */
    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    /**
     * @return Most future estimated sync date.
     */
    public Date getEtaSyncDate() {
        return etaSyncDate;
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

    /**
     * When a folder updates its stats, check all folders to get whether any
     * folders are syncing and what the greatest estimated / last sync date is.
     */
    private void calculateOverallStats() {
        boolean syncing = getController().getFolderRepository().isSyncing();

        // Find the folder with the most recent synchronized date / most
        // future estimated date.
        Date calcLastSyncDate = null;
        Date calcEtaDate = null;
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            Date tmpLastSync = folder.getLastSyncDate();
            Date tmpEtaDate = folder.getStatistic().getEstimatedSyncDate();

            if (tmpLastSync != null) {
                if (calcLastSyncDate == null
                    || tmpLastSync.after(calcLastSyncDate))
                {
                    calcLastSyncDate = tmpLastSync;
                }
            }

            if (tmpEtaDate != null) {
                if (calcEtaDate == null || tmpEtaDate.after(calcEtaDate)) {
                    calcEtaDate = tmpEtaDate;
                }
            }
        }

        etaSyncDate = calcEtaDate;
        lastSyncDate = calcLastSyncDate;
        syncingAtDate = syncing;

        overallFolderStatListenerSupport
            .statCalculated(new OverallFolderStatEvent(syncing));
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

        public void scanResultCommited(FolderEvent folderEvent) {
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
