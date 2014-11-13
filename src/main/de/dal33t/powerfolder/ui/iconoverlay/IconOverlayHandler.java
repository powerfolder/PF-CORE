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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.liferay.nativity.control.win.WindowsNativityUtil;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.LockingEvent;
import de.dal33t.powerfolder.event.LockingListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Decide which Overlay to add to which Icon on Windows Explorer.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IconOverlayHandler extends PFComponent implements
    FileIconControlCallback
{
    private MyIconOverlayListener updateListener;

    public IconOverlayHandler(Controller controller) {
        super(controller);
        updateListener = new MyIconOverlayListener();
    }

    @Override
    public int getIconForFile(String pathName) {
        try {
            // First check, if the path is associated with any folder ...
            FolderRepository fr = getController().getFolderRepository();

            Path basepath = fr.getFoldersBasedir();
            Path path = Paths.get(pathName);

            if (basepath.equals(path.getParent()) && Files.isRegularFile(path))
            {
                if (Constants.GETTING_STARTED_GUIDE_FILENAME.equals(path
                    .getFileName().toString())
                    || PathUtils.DESKTOP_INI_FILENAME.equals(path.getFileName()
                        .toString()))
                {
                    return IconOverlayIndex.NO_OVERLAY.getIndex();
                }
                return IconOverlayIndex.WARNING_OVERLAY.getIndex();
            }

            Folder folder = fr.findContainingFolder(pathName);
            if (folder == null) {
                return IconOverlayIndex.NO_OVERLAY.getIndex();
            }

            // ... then see, if it is part of a meta-folder.
            if (pathName.contains(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                return IconOverlayIndex.NO_OVERLAY.getIndex();
            }

            // We know, it is a file in a Folder, so create a lookup instance
            // ...
            FileInfo lookup = FileInfoFactory.lookupInstance(folder, path);
            SyncStatus status = SyncStatus.of(getController(), lookup);

            // Pick the apropriate icon overlay
            switch (status) {
                case SYNC_OK :
                    return IconOverlayIndex.OK_OVERLAY.getIndex();
                case SYNCING :
                    return IconOverlayIndex.SYNCING_OVERLAY.getIndex();
                case IGNORED :
                    return IconOverlayIndex.IGNORED_OVERLAY.getIndex();
                case LOCKED :
                    return IconOverlayIndex.LOCKED_OVERLAY.getIndex();
                case WARNING :
                    return IconOverlayIndex.WARNING_OVERLAY.getIndex();
                case NONE :
                default :
                    return IconOverlayIndex.NO_OVERLAY.getIndex();
            }
        } catch (RuntimeException re) {
            logSevere("An error occured while determening the icon overlay for file '"
                + pathName + "'. " + re);
            re.printStackTrace();
            return IconOverlayIndex.NO_OVERLAY.getIndex();
        }
    }

    public void start() {
        getController().getFolderRepository().getLocking()
            .addListener(updateListener);
        getController().getTransferManager().addListener(updateListener);
    }

    public void stop() {
        getController().getFolderRepository().getLocking()
            .removeListener(updateListener);
        getController().getTransferManager().removeListener(updateListener);
    }

    private class MyIconOverlayListener implements LockingListener,
        TransferManagerListener
    {

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

        private void update(FileInfo fInfo) {
            if (OSUtil.isWindowsSystem()) {
                final Path file = fInfo.getDiskFile(getController()
                    .getFolderRepository());
                Folder folder = fInfo.getFolder(getController()
                    .getFolderRepository());
                final Path folderBaseDir = folder != null ? folder
                    .getLocalBase() : null;

                UIUtil.invokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        if (Files.exists(file)) {
                            WindowsNativityUtil.updateExplorer(file.toString());
                        }
                        if (folderBaseDir != null
                            && Files.exists(folderBaseDir))
                        {
                            WindowsNativityUtil.updateExplorer(folderBaseDir
                                .toString());
                        }
                    }
                });
            }
        }
    }
}
