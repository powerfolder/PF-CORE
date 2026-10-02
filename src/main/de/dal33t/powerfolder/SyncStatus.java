/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.Reject;

/**
 * Identify the icon overlay by index and name.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public enum SyncStatus {
    SYNC_OK, SYNCING, IGNORED, WARNING, LOCKED, NONE;

    /**
     * @param controller
     * @param fInfo
     *            The file to get the {@link SyncStatus} for.
     * @return the sync status of the file
     */
    public static SyncStatus of(Controller controller, FileInfo fInfo) {
        return SyncStatus.of(controller, fInfo, null);
    }

    /**
     * @param controller
     * @param fInfo
     *            The file to get the {@link SyncStatus} for.
     * @param folder
     *            If the {@code Folder} is already known, just pass it here.
     *            Otherwise use {@link #of(Controller, FileInfo)}.
     * @return the sync status of the file
     */
    public static SyncStatus of(Controller controller, FileInfo fInfo, Folder folder) {
        Reject.ifNull(controller, "Controller");
        Reject.ifNull(fInfo, "FileInfo");

        if (!controller.isStarted() || controller.isShuttingDown()) {
            return NONE;
        }
        if (folder == null) {
            folder = fInfo.getFolder(controller.getFolderRepository());
            if (folder == null) {
                return NONE;
            }
        }
        if (fInfo.equals(folder.getBaseDirectoryInfo())) {
            double sync = folder.getStatistic().getHarmonizedSyncPercentage();
            if (folder.isTransferring()) {
                return SYNCING;
            } else if (folder.isDeviceDisconnected()) {
                return WARNING;
            } else if (folder.getCompletelyConnectedMembersCount() == 0 || sync < 0) {
                return NONE;
            } else if (sync > 0 && sync < 100.0d) {
                return SYNCING;
            } else {
                return SYNC_OK;
            }
        }
        if (fInfo.isDiretory()) {
            if (fInfo.isLocked(controller)) {
                return LOCKED;
            }
            // PFC-3643: subfolders show a sync overlay too (they used to always
            // return NONE): the spinner while anything inside them is transferring,
            // otherwise the synced tick. A tick on an otherwise-idle directory means
            // "nothing is syncing under it" - with the default full sync that is the
            // on-device/synced state.
            if (folder.getCompletelyConnectedMembersCount() == 0) {
                return NONE;
            }
            return isTransferringUnder(controller, fInfo) ? SYNCING : SYNC_OK;
        }
        if (fInfo.isLookupInstance()) {
            fInfo = folder.getDAO().find(fInfo, null);
        }
        if (fInfo == null) {
            return NONE;
        }
        if (folder.getCompletelyConnectedMembersCount() == 0) {
            return NONE;
        }
        if (folder.getDiskItemFilter().isExcluded(fInfo)) {
            return IGNORED;
        }
        TransferManager tm = controller.getTransferManager();
        if (tm.isDownloading(fInfo) || tm.isUploading(fInfo)) {
            return SYNCING;
        }
        if (fInfo.isLocked(controller)) {
            return LOCKED;
        }
        if (fInfo.isNewerAvailable(controller.getFolderRepository())) {
            return SYNCING;
        }
        if (tm.getSourcesFor(fInfo).isEmpty()) {
            return SYNCING;
        }
        return SYNC_OK;
    }

    /**
     * PFC-3643: is any active transfer (download or upload) for a file located
     * inside the given directory? Active transfers are few, so this is a cheap
     * bounded check (no per-callback directory scan).
     */
    private static boolean isTransferringUnder(Controller controller, FileInfo dirInfo) {
        String prefix = dirInfo.getRelativeName();
        if (prefix == null) {
            return false;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        TransferManager tm = controller.getTransferManager();
        for (DownloadManager dm : tm.getActiveDownloads()) {
            if (isUnder(dm.getFileInfo(), dirInfo, prefix)) {
                return true;
            }
        }
        for (Upload ul : tm.getActiveUploads()) {
            if (isUnder(ul.getFile(), dirInfo, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnder(FileInfo transferFile, FileInfo dirInfo,
        String dirPrefix)
    {
        if (transferFile == null
            || !transferFile.getFolderInfo().equals(dirInfo.getFolderInfo()))
        {
            return false;
        }
        String rn = transferFile.getRelativeName();
        return rn != null && rn.startsWith(dirPrefix);
    }
}
