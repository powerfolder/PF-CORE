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
 * $Id: SyncButtonComponent.java 5500 2008-10-25 04:23:44Z harry $
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.ui.actionold.SyncAllFoldersAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JLabel;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to display and handle Sync All button.
 */
public class SyncButtonComponent extends PFUIComponent {

    private JLabel syncAllLabel;
    private final AtomicBoolean mouseOver;
    private final AtomicBoolean anyFolderSyncing;
    private final AtomicBoolean mousePressed;

    /**
     * Constructor
     * 
     * @param controller
     */
    public SyncButtonComponent(Controller controller) {
        super(controller);
        mouseOver = new AtomicBoolean();
        anyFolderSyncing = new AtomicBoolean();
        mousePressed = new AtomicBoolean();
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        controller.getThreadPool().submit(new MyRunnable());
    }

    /**
     * @return the sync 'button'.
     */
    public Component getUIComponent() {
        if (syncAllLabel == null) {
            initComponents();
        }
        return syncAllLabel;
    }

    private void initComponents() {
        syncAllLabel = new JLabel(Icons
            .getIconById("icons/sync/normal/sync_00.png"));
        syncAllLabel.setToolTipText(Translation
            .getTranslation("scan_all_folders.description"));
        syncAllLabel.addMouseListener(new MyMouseAdapter());
    }

    private void updateSyncLabel() {
        anyFolderSyncing.set(getController().getFolderRepository()
            .isAnyFolderTransferring()
            || getController().getFolderRepository()
                .getCurrentlyMaintainingFolder() != null);
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateSyncLabel();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void completedDownloadRemoved(TransferManagerEvent event) {
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadRequested(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

    /**
     * Class to handle mouse overs and clicks.
     */
    private class MyMouseAdapter extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            mousePressed.set(true);
            logFine("Triggering SyncAllFoldersAction perfomSync");
            SyncAllFoldersAction.perfomSync(getController());
        }

        public void mouseReleased(MouseEvent e) {
            mousePressed.set(false);
        }

        public void mouseEntered(MouseEvent e) {
            mouseOver.set(true);
        }

        public void mouseExited(MouseEvent e) {
            mouseOver.set(false);
        }
    }

    private class MyRunnable implements Runnable {
        public void run() {
            int index = 0;
            while (!getController().getThreadPool().isShutdown()) {
                try {
                    Thread.sleep(40);
                    if (anyFolderSyncing.get()) {
                        index++;
                        if (index > 17) {
                            index = 0;
                        }
                    } else {
                        index = 0;
                    }

                    String numberString;
                    if (index > 9) {
                        numberString = String.valueOf(index);
                    } else {
                        numberString = '0' + String.valueOf(index);
                    }

                    String directoryString;
                    if (mouseOver.get()) {
                        if (mousePressed.get()) {
                            directoryString = "push";
                        } else {
                            directoryString = "hover";
                        }
                    } else {
                        directoryString = "normal";
                    }

                    String iconString = "icons/sync/" + directoryString
                        + "/sync_" + numberString + ".png";
                    final Icon icon = Icons.getIcon(iconString);
                    // Move into EDT, otherwise it violates the one thread EDT
                    // rule.
                    UIUtil.invokeAndWaitInEDT(new Runnable() {
                        public void run() {
                            syncAllLabel.setIcon(icon);
                        }
                    });

                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
            logFine("Thread terminated");
        }
    }
}
