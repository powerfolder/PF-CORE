/* $Id: FolderQuickInfoPanel.java,v 1.3 2006/04/06 13:48:05 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.folder;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;

/**
 * Show concentrated information about the whole folder repository
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class FolderQuickInfoPanel extends QuickInfoPanel {

    /** Reduce sync perc to be the same size as the folder picto. */
    private static final double SCALE_FACTOR = 0.8;

    private JComponent picto;
    private JLabel headerText;
    private JLabel infoText1;
    private JLabel infoText2;
    private JLabel syncStatusPicto;

    private Folder currentFolder;
    private MyFolderListener myFolderListener;

    private TimeEstimator syncETAEstimator;

    protected FolderQuickInfoPanel(Controller controller) {
        super(controller);
        myFolderListener = new MyFolderListener();
    }

    /**
     * Initalizes the components
     */
    protected void initComponents() {
        syncETAEstimator = new TimeEstimator(Constants.ESTIMATION_WINDOW_MILLIS);
        
        headerText = SimpleComponentFactory.createBiggerTextLabel("");
        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");
        picto = new JLabel(Icons.FOLDER_PICTO);
        syncStatusPicto = new JLabel(Icons.SYNC_UNKNOWN);
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getUIController().getControlQuarter().getSelectionModel()
            .addSelectionChangeListener(new MySelectionChangeListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        if (currentFolder != null) {
            String name = currentFolder.getName();
            if (name.length() > 30) {
                name = name.substring(0, 30) + "...";
            }
            headerText.setText(Translation.getTranslation(
                "quickinfo.folder.status_of_folder", name));

            boolean isMembersConnected = currentFolder.getConnectedMembers().length > 0;

            StringBuilder text1 = new StringBuilder();
            boolean showDownloads = true;
            if (!isMembersConnected) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.disconnected"));
            } else if (currentFolder.isTransferring()) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.is_synchronizing"));
                FolderStatistic folderStatistic = currentFolder.getStatistic();
                double sync = currentFolder.getStatistic().getHarmonizedSyncPercentage();

                if (folderStatistic.getDownloadCounter() != null && sync < 100
                        && currentFolder.getSyncProfile().isAutodownload()) {
                    syncETAEstimator.addValue(folderStatistic.getLocalSyncPercentage());
                    text1.append(", " + Translation
                            .getTranslation("quickinfo.folder.up_to_date_in",
                                    new EstimatedTime(syncETAEstimator
                                            .estimatedMillis(100.0), true).toString()));
                    showDownloads = false;
                }
            } else if (currentFolder.isPreviewOnly()) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.preview"));
            } else {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.is_in_sync"));
            }

            if (showDownloads) {
                int nCompletedDls = countCompletedDownloads();
                if (nCompletedDls > 0) {
                    // This is a hack(tm)
                    text1.append(", "
                        + Translation.getTranslation(
                            "quickinfo.folder.downloads_recently_completed",
                            nCompletedDls));
                }
            }

            infoText1.setText(text1.toString());

            FolderStatistic folderStatistic = currentFolder.getStatistic();
            String text2 = Translation.getTranslation(
                "quickinfo.folder.number_of_files_and_size", String
                    .valueOf(folderStatistic.getTotalFilesCount()), Format
                    .formatBytes(folderStatistic.getTotalSize()));

            infoText2.setText(text2);
            setSyncPercentage(currentFolder.getStatistic()
                .getHarmonizedSyncPercentage());
        }
    }

    /**
     * Set the synchronization percentage image on the right of the panel.
     * 
     * @param percentage
     */
    private void setSyncPercentage(double percentage) {
        if (percentage < 0.0 || (int) percentage > 100) {
            syncStatusPicto.setIcon(Icons.scaleIcon(
                (ImageIcon) Icons.SYNC_UNKNOWN, SCALE_FACTOR));
        } else {
            syncStatusPicto.setIcon(Icons.scaleIcon(
                (ImageIcon) Icons.SYNC_ICONS[(int) percentage], SCALE_FACTOR));
        }
        syncStatusPicto.setVisible(true);
        syncStatusPicto.setToolTipText(SyncProfileUtil
            .renderSyncPercentage(percentage));
    }

    // Overridden stuff *******************************************************

    protected JComponent getPicto() {
        return picto;
    }

    protected JComponent getHeaderText() {
        return headerText;
    }

    protected JComponent getInfoText1() {
        return infoText1;
    }

    protected JComponent getInfoText2() {
        return infoText2;
    }

    protected JComponent getRightComponent() {
        return syncStatusPicto;
    }

    private void setFolder(Folder folder) {
        if (currentFolder != null) {
            currentFolder.removeFolderListener(myFolderListener);
        }
        currentFolder = folder;
        if (currentFolder != null) {
            currentFolder.addFolderListener(myFolderListener);
            updateText();
        }
    }

    // UI listeners
    private class MySelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            SelectionModel selectionModel = (SelectionModel) event.getSource();
            Object selection = selectionModel.getSelection();

            if (selection instanceof Folder) {
                setFolder((Folder) selection);
            } else if (selection instanceof Directory) {
                setFolder(((Directory) selection).getRootFolder());
            }
        }
    }

    // Helper code ************************************************************

    private int countCompletedDownloads() {
        int completedDls = 0;
        for (DownloadManager dl : getController().getTransferManager()
            .getCompletedDownloadsCollection())
        {
            if (dl.getFileInfo().getFolderInfo().equals(currentFolder.getInfo())) {
                completedDls++;
            }
        }
        return completedDls;
    }

    // Core listeners *********************************************************
    private class MyFolderListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            updateText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {
        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                updateText();
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                updateText();
            }
        }

        @Override
        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateText();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateText();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}