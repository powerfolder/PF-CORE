/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.iconoverlay;

import com.liferay.nativity.control.win.WindowsNativityUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listen to changes of the transfer state, locking, folders and the folder
 * repository to update the overlay icons.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IconOverlayUpdateListener extends PFComponent implements
    LockingListener, TransferManagerListener, FolderListener,
    FolderRepositoryListener
{
    private final FileIconControl iconControl;
    private final IconOverlayHandler overlayHandler;

    private AtomicBoolean updateRunning = new AtomicBoolean(false);
    private Map<FileInfo, FileInfo> dirtyFiles;

    public IconOverlayUpdateListener(Controller controller,
        FileIconControl iconControl, IconOverlayHandler overlayHandler)
    {
        super(controller);
        this.iconControl = iconControl;
        this.overlayHandler = overlayHandler;
        this.dirtyFiles = Util.createConcurrentHashMap();
    }

    @Override
    public boolean fireInEventDispatchThread() {
        return false;
    }

    @Override
    public void downloadRequested(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void downloadQueued(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void downloadStarted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void downloadAborted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void downloadBroken(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void downloadCompleted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void completedDownloadRemoved(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void pendingDownloadEnqueued(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void uploadRequested(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void uploadStarted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void uploadAborted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void uploadBroken(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void uploadCompleted(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void completedUploadRemoved(TransferManagerEvent event) {
        update(event.getFile());
    }

    @Override
    public void locked(LockingEvent event) {
        update(event.getFileInfo());
    }

    @Override
    public void unlocked(LockingEvent event) {
        update(event.getFileInfo());
    }

    @Override
    public void autoLockForbidden(LockingEvent event) {
        update(event.getFileInfo());
    }


    private void update(FileInfo fInfo) {
        if (fInfo.getFolder(getController().getFolderRepository()) == null) {
            return;
        }

        dirtyFiles.put(fInfo, fInfo);

        if (!updateRunning.compareAndSet(false, true)) {
            if (isFiner()) {
                logFiner("Not spawning, already running update");
            }
            return;
        }

        getController().getIOProvider().startIO(new Runnable() {
            @Override
            public void run() {
                while (!dirtyFiles.isEmpty()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        updateRunning.set(false);
                        return;
                    }
                    if (isFine()) {
                        logFine("Updating OS file exporer for files: " + dirtyFiles.size()+ ": " + dirtyFiles);
                    }
                    for (FileInfo fileInfo : dirtyFiles.keySet()) {
                        dirtyFiles.remove(fileInfo);
                        Path file = fileInfo.getDiskFile(getController().getFolderRepository());
                        if (file == null) {
                            continue;
                        }
                        try {
                            if (Files.exists(file)) {
                                String fileName = file.toString();
                                if (OSUtil.isWindowsSystem()) {
                                    WindowsNativityUtil.updateExplorer(fileName);
                                } else if (OSUtil.isMacOS()) {
                                    iconControl.setFileIcon(fileName, overlayHandler.getIconForFile(fileInfo));
                                }
                            }
                        } catch (RuntimeException re) {
                            logFine(
                                    "Caught exception while updating single file "
                                            + fileInfo.toDetailString() + ". " + re, re);
                        }
                    }
                }
                updateRunning.set(false);
            }
        });
    }

    private void updateFolder(final Folder folder) {
        if (OSUtil.isWindowsSystem()) {
            getController().getIOProvider().startIO(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Do not update folder Documents because it would lead to duplicate entries in explorer sidebar (see PFC-2862)
                        if (!folder.getLocalBase().toString().equals(UserDirectories.getDocumentsReported())) {
                            WindowsNativityUtil.updateExplorer(folder.getLocalBase().toString());
                        }
                    } catch (RuntimeException re) {
                        logFine("Caught exception while updating folder "
                            + folder + ". " + re, re);
                    } catch (UnsatisfiedLinkError e) {
                        logFine("Caught exception while updating folder "
                            + folder + ". " + e, e);
                    }
                }
            });
        }
    }

    @Override
    public void folderRemoved(FolderRepositoryEvent e) {
        e.getFolder().removeFolderListener(this);
    }

    @Override
    public void folderCreated(FolderRepositoryEvent e) {
        e.getFolder().addFolderListener(this);
    }

    @Override
    public void folderMoved(FolderRepositoryEvent e) {
        e.getOldFolder().removeFolderListener(this);
        e.getFolder().addFolderListener(this);
    }

    @Override
    public void maintenanceStarted(FolderRepositoryEvent e) {
    }

    @Override
    public void maintenanceFinished(FolderRepositoryEvent e) {
    }

    @Override
    public void cleanupStarted(FolderRepositoryEvent e) {
    }

    @Override
    public void cleanupFinished(FolderRepositoryEvent e) {
        // ignore
    }

    @Override
    public void statisticsCalculated(FolderEvent folderEvent) {
        updateFolder(folderEvent.getFolder());
    }

    @Override
    public void syncProfileChanged(FolderEvent folderEvent) {
    }

    @Override
    public void archiveSettingsChanged(FolderEvent folderEvent) {
    }

    @Override
    public void remoteContentsChanged(FolderEvent folderEvent) {
    }

    @Override
    public void scanResultCommited(FolderEvent folderEvent) {
        if (folderEvent.getScanResult().isChangeDetected()) {
            updateFolder(folderEvent.getFolder());
        }
    }

    @Override
    public void fileChanged(FolderEvent folderEvent) {
    }

    @Override
    public void filesDeleted(FolderEvent folderEvent) {
    }
}
