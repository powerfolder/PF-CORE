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
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.Reject;

import java.util.Collection;

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
        if (fInfo.isDiretory() && !fInfo.isLocked(controller)) {
            return NONE;
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
     * Sync status for the file-manager icon overlay. A subdirectory reports a state of its own: {@link #IGNORED} when
     * excluded, {@link #LOCKED}, {@link #SYNCING} while a file under it transfers, {@link #NONE} while a file under it
     * waits for a source, otherwise {@link #SYNC_OK}. Files and the base directory get
     * {@link #of(Controller, FileInfo, Folder)}.
     *
     * @param controller
     * @param fInfo
     *            The file or directory to get the {@link SyncStatus} for.
     * @param folder
     *            The {@code Folder} of the file, or {@code null} to look it up.
     * @return the sync status for the icon overlay
     */
    public static SyncStatus ofDirectoryOverlay(Controller controller, FileInfo fInfo, Folder folder) {
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
        if (!fInfo.isDiretory() || fInfo.equals(folder.getBaseDirectoryInfo())) {
            return of(controller, fInfo, folder);
        }
        if (folder.getCompletelyConnectedMembersCount() == 0) {
            return NONE;
        }
        if (folder.getDiskItemFilter().isExcluded(fInfo)) {
            return IGNORED;
        }
        if (fInfo.isLocked(controller)) {
            return LOCKED;
        }
        String prefix = fInfo.getRelativeName();
        if (prefix == null) {
            return NONE;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        TransferManager tm = controller.getTransferManager();
        if (isTransferringUnder(tm, fInfo, prefix)) {
            return SYNCING;
        }
        if (isPendingUnder(tm, fInfo, prefix)) {
            return NONE;
        }
        return SYNC_OK;
    }

    /**
     * @return true if an active download or upload is located inside the directory; returns at once while nothing
     *         transfers at all
     */
    private static boolean isTransferringUnder(TransferManager tm, FileInfo dirInfo, String prefix) {
        Collection<DownloadManager> downloads = tm.getActiveDownloads();
        Collection<Upload> uploads = tm.getActiveUploads();
        if (downloads.isEmpty() && uploads.isEmpty()) {
            return false;
        }
        for (DownloadManager dm : downloads) {
            if (isUnder(dm.getFileInfo(), dirInfo, prefix)) {
                return true;
            }
        }
        for (Upload ul : uploads) {
            if (isUnder(ul.getFile(), dirInfo, prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a download inside the directory is queued and waits for a source
     */
    private static boolean isPendingUnder(TransferManager tm, FileInfo dirInfo, String prefix) {
        Collection<Download> pending = tm.getPendingDownloads();
        if (pending.isEmpty()) {
            return false;
        }
        for (Download dl : pending) {
            if (isUnder(dl.getFile(), dirInfo, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnder(FileInfo transferFile, FileInfo dirInfo, String dirPrefix) {
        if (transferFile == null || !transferFile.getFolderInfo().equals(dirInfo.getFolderInfo())) {
            return false;
        }
        String rn = transferFile.getRelativeName();
        return rn != null && rn.startsWith(dirPrefix);
    }
}
