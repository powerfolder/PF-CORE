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
 *
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case to ensure that a file is re-downloaded if it is removed from the db
 * and then is available at a peer.
 */
public class RedownloadTest extends TwoControllerTestCase {

    private final List<String> rowHistory = new CopyOnWriteArrayList<>();

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        recordRowsOf(getFolderAtBart(), "bart");
        recordRowsOf(getFolderAtLisa(), "lisa");
    }

    private void recordRowsOf(final Folder folder, final String nick) {
        folder.addFolderListener(new FolderAdapter() {
            @Override
            public void fileChanged(FolderEvent event) {
                note(nick, "changed", event.getScannedFileInfos());
            }

            @Override
            public void filesDeleted(FolderEvent event) {
                note(nick, "deleted", event.getDeletedFileInfos());
            }

            @Override
            public boolean fireInEventDispatchThread() {
                return false;
            }
        });
    }

    private void note(String nick, String what, Collection<FileInfo> files) {
        if (files == null) {
            return;
        }
        List<String> details = new ArrayList<>();
        for (FileInfo file : files) {
            details.add(file.toDetailString());
        }
        rowHistory.add(nick + " " + what + " " + details);
    }

    private void waitForUploadsFinished(Controller controller) {
        TransferManager transferManager = controller.getTransferManager();
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Uploads at " + controller.getMySelf().getNick() + ": active "
                    + transferManager.getActiveUploads() + ", queued " + transferManager.getQueuedUploads();
            }

            @Override
            public boolean reached() {
                return transferManager.countActiveUploads() == 0
                    && transferManager.countQueuedUploads() == 0;
            }
        });
    }

    private static FileInfo syncedFileInfo(Folder folder, FileInfo expected) {
        FileInfo local = folder.getFile(expected);
        if (local == null || local.isDeleted()
            || !local.isVersionDateAndSizeIdentical(expected))
        {
            return null;
        }
        Path diskFile = local.getDiskFile(folder.getController()
            .getFolderRepository());
        return local.inSyncWithDisk(diskFile) ? local : null;
    }

    @Test
    public void testRedownload() throws IOException {

        final Folder folderBart = getFolderAtBart();

        final Folder folderLisa = getFolderAtLisa();

        // Set up Bart & Lisa with a file in the folder.
        final Path createdAtBart = TestHelper
            .createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(folderBart);
        final FileInfo scannedAtBart = FileInfoFactory
            .lookupInstance(folderBart, createdAtBart);
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Bart did not scan " + createdAtBart
                    + " in sync with disk. Known files: "
                    + folderBart.getKnownFiles();
            }

            public boolean reached() {
                FileInfo local = folderBart.getFile(scannedAtBart);
                return local != null && !local.isDeleted()
                    && local.inSyncWithDisk(createdAtBart);
            }
        });
        final FileInfo fileAtBart = folderBart.getFile(scannedAtBart);
        scanFolder(folderLisa);
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Lisa did not receive " + fileAtBart.toDetailString()
                    + ". Known files at lisa: " + folderLisa.getKnownFiles();
            }

            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor().triggerFileRequesting();
                return syncedFileInfo(folderLisa, fileAtBart) != null;
            }
        });
        waitForUploadsFinished(getContollerBart());

        // Delete the file at Bart.
        FileInfo fileInfoBart = folderBart.getKnownFiles().iterator().next();
        final Path testFileBart = fileInfoBart.getDiskFile(getContollerBart()
            .getFolderRepository());
        assertTrue( Files.exists(testFileBart),"Bart file should exist");
        try {
            Files.delete(testFileBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        assertFalse( Files.exists(testFileBart),"Bart file should not exist");

        scanFolder(folderBart);
        scanFolder(folderLisa);

        assertEquals( 1, folderBart.getKnownItemCount(),"Bart file count bad");
        assertEquals( 1, folderLisa.getKnownItemCount(),"Lisa file count bad");
        fileInfoBart = folderBart.getKnownFiles().iterator().next();
        assertTrue( fileInfoBart.isDeleted(),"Bart file not deleted");
        FileInfo fileInfoLisa = folderLisa.getKnownFiles().iterator().next();
        assertTrue( !fileInfoLisa.isDeleted(),"Lisa file deleted");

        // Remove Bart's file from the db, so it can be re-downloaded.
        folderBart.removeDeletedFileInfo(fileInfoBart);
        assertSame( 0, folderBart.getKnownItemCount(),"Bart still has old file");

        // Scan folders. Bart should see Lisa's file and download.
        scanFolder(folderBart);
        scanFolder(folderLisa);
        getContollerBart().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folderBart.getInfo());

        // Wait for copy.
        final FileInfo sourceAtLisa = folderLisa.getKnownFiles().iterator()
            .next();
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Bart did not redownload "
                    + sourceAtLisa.toDetailString() + ". Bart file exists: "
                    + Files.exists(testFileBart) + ", known files at bart: "
                    + folderBart.getKnownFiles();
            }

            public boolean reached() {
                getContollerBart().getFolderRepository().getFileRequestor()
                        .triggerFileRequesting(folderBart.getInfo());
                return syncedFileInfo(folderBart, sourceAtLisa) != null;
            }
        });

        assertEquals( 1, folderBart.getKnownItemCount(),"Bart file count bad");
        fileInfoBart = folderBart.getKnownFiles().iterator().next();
        assertFileMatch(testFileBart, fileInfoBart, getContollerBart());
        assertEquals( sourceAtLisa.getVersion(), fileInfoBart.getVersion(),
            "Bart did not take over Lisas version. Bart: "
                + fileInfoBart.toDetailString() + ". Lisa: "
                + sourceAtLisa.toDetailString());
        assertEquals( 0, fileInfoBart.getVersion(),
            "Neither side ever changed the file, so the re-downloaded row must still be"
                + " the first version. Bart: " + fileInfoBart.toDetailString()
                + ". Lisa: " + sourceAtLisa.toDetailString()
                + ". Lisa now: " + folderLisa.getFile(sourceAtLisa)
                + ". Every row change either side published: " + rowHistory);
    }
}
