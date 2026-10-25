/*
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

import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.Locking;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Directory overlay rules of {@link SyncStatus#ofDirectoryOverlay} and the unchanged directory result of
 * {@link SyncStatus#of(Controller, FileInfo, Folder)}.
 */
public class SyncStatusTest {

    private Controller controller;
    private Folder folder;
    private TransferManager tm;
    private Locking locking;
    private DiskItemFilter filter;
    private FolderInfo folderInfo;
    private DirectoryInfo dir;

    @BeforeEach
    public void setUp() {
        controller = mock(Controller.class);
        folder = mock(Folder.class);
        tm = mock(TransferManager.class);
        locking = mock(Locking.class);
        filter = mock(DiskItemFilter.class);
        FolderRepository repo = mock(FolderRepository.class);
        FileInfoDAO dao = mock(FileInfoDAO.class);

        folderInfo = FolderInfoFactory.newTopFolder("SyncStatusTest");
        dir = FileInfoFactory.lookupDirectory(folderInfo, "sub");

        when(controller.isStarted()).thenReturn(true);
        when(controller.isShuttingDown()).thenReturn(false);
        when(controller.getTransferManager()).thenReturn(tm);
        when(controller.getFolderRepository()).thenReturn(repo);
        when(repo.getLocking()).thenReturn(locking);
        when(folder.getBaseDirectoryInfo()).thenReturn(FileInfoFactory.lookupDirectory(folderInfo, ""));
        when(folder.getCompletelyConnectedMembersCount()).thenReturn(1);
        when(folder.getDiskItemFilter()).thenReturn(filter);
        when(folder.getDAO()).thenReturn(dao);
        when(dao.find(any(FileInfo.class), isNull())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    public void idleSyncedDirectoryShowsTick() {
        assertEquals(SyncStatus.SYNC_OK, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void excludedDirectoryIsIgnored() {
        when(filter.isExcluded(dir)).thenReturn(true);
        assertEquals(SyncStatus.IGNORED, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void lockedDirectoryIsLocked() {
        when(locking.isLocked(dir)).thenReturn(true);
        assertEquals(SyncStatus.LOCKED, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void activeDownloadUnderDirectoryIsSyncing() {
        DownloadManager dm = mock(DownloadManager.class);
        when(dm.getFileInfo()).thenReturn(FileInfoFactory.lookupInstance(folderInfo, "sub/deep/file.txt"));
        when(tm.getActiveDownloads()).thenReturn(List.of(dm));
        assertEquals(SyncStatus.SYNCING, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void activeUploadUnderDirectoryIsSyncing() {
        Upload ul = mock(Upload.class);
        when(ul.getFile()).thenReturn(FileInfoFactory.lookupInstance(folderInfo, "sub/file.txt"));
        when(tm.getActiveUploads()).thenReturn(List.of(ul));
        assertEquals(SyncStatus.SYNCING, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void transferInSiblingDirectoryKeepsTick() {
        DownloadManager dm = mock(DownloadManager.class);
        when(dm.getFileInfo()).thenReturn(FileInfoFactory.lookupInstance(folderInfo, "subway/file.txt"));
        when(tm.getActiveDownloads()).thenReturn(List.of(dm));
        assertEquals(SyncStatus.SYNC_OK, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void pendingDownloadUnderDirectoryShowsNoTick() {
        Download dl = mock(Download.class);
        when(dl.getFile()).thenReturn(FileInfoFactory.lookupInstance(folderInfo, "sub/file.txt"));
        when(tm.getPendingDownloads()).thenReturn(List.of(dl));
        assertEquals(SyncStatus.NONE, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void disconnectedFolderShowsNothing() {
        when(folder.getCompletelyConnectedMembersCount()).thenReturn(0);
        assertEquals(SyncStatus.NONE, SyncStatus.ofDirectoryOverlay(controller, dir, folder));
    }

    @Test
    public void nonOverlayDirectoryKeepsOldResult() {
        assertEquals(SyncStatus.NONE, SyncStatus.of(controller, dir, folder));
        when(locking.isLocked(dir)).thenReturn(true);
        assertEquals(SyncStatus.LOCKED, SyncStatus.of(controller, dir, folder));
        when(filter.isExcluded(dir)).thenReturn(true);
        assertEquals(SyncStatus.IGNORED, SyncStatus.of(controller, dir, folder));
    }
}
