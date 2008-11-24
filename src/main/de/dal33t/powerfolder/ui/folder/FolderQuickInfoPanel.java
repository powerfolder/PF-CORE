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
package de.dal33t.powerfolder.ui.folder;

import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
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
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;
import de.dal33t.powerfolder.util.ui.TimeEstimator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Show concentrated information about the whole folder repository
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class FolderQuickInfoPanel extends QuickInfoPanel {

    private static final int DELAY = 500;

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

    private TimerTask updater;

    protected FolderQuickInfoPanel(Controller controller) {
        super(controller);
        myFolderListener = new MyFolderListener();
        updater = null;
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
//        getUIController().getControlQuarter().getSelectionModel()
//            .addSelectionChangeListener(new MySelectionChangeListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    private void scheduleUpdate() {
        if (updater != null) {
            // Already scheduled
            return;
        }
        updater = new TimerTask() {
            @Override
            public void run() {
                UIUtil.invokeLaterInEDT(new Runnable() {
                    public void run() {
                        updateText();
                        updater = null;
                    }
                });
            }
        };
        getController().schedule(updater, DELAY);
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
            boolean isMembersConnected = currentFolder
                .getConnectedMembersCount() > 0;
            StringBuilder text1 = new StringBuilder();
            boolean showDownloads = true;
            if (currentFolder.isDeviceDisconnected()) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.invalid_base_dir"));
            } else if (!isMembersConnected) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.disconnected"));
            } else if (currentFolder.isTransferring()) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.is_synchronizing"));
                FolderStatistic folderStatistic = currentFolder.getStatistic();
                double sync = currentFolder.getStatistic()
                    .getHarmonizedSyncPercentage();

                if (folderStatistic.getDownloadCounter() != null && sync < 100
                    && currentFolder.getSyncProfile().isAutodownload())
                {
                    syncETAEstimator.addValue(folderStatistic
                        .getLocalSyncPercentage());
                    text1.append(", "
                        + Translation.getTranslation(
                            "quickinfo.folder.up_to_date_in",
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
        Icon syncIcon = null;
        // TODO Cache
        if (percentage < 0.0 || (int) percentage > 100) {
            syncIcon = Icons.scaleIcon((ImageIcon) Icons.SYNC_UNKNOWN,
                SCALE_FACTOR);
        } else {
            syncIcon = Icons.scaleIcon(
                (ImageIcon) Icons.SYNC_ICONS[(int) percentage], SCALE_FACTOR);
        }
        syncStatusPicto.setIcon(syncIcon);
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
            
            // scheduleUpdate();
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
            if (dl.getFileInfo().getFolderInfo()
                .equals(currentFolder.getInfo()))
            {
                completedDls++;
            }
        }
        return completedDls;
    }

    // Core listeners *********************************************************
    private class MyFolderListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            scheduleUpdate();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {
        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                scheduleUpdate();
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                scheduleUpdate();
            }
        }

        @Override
        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void downloadQueued(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void downloadStarted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void downloadAborted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void downloadBroken(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void uploadRequested(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void uploadStarted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void uploadAborted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void uploadBroken(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            scheduleUpdate();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}