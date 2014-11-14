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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.FolderScanner;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FolderScannerTest extends ControllerTestCase {

    public void setUp() throws Exception {
        super.setUp();
        // use project profiel so no unwanted scanning
        setupTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
    }

    public void testScanFilesMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testScanFiles();
            tearDown();
            setUp();
        }
    }

    public void testScanFiles() throws Exception {
        // getController().setPaused(true);
        final FolderScanner folderScanner = getController()
            .getFolderRepository().getFolderScanner();
        // getController().setPaused(false);

        Path file1 = TestHelper.createRandomFile(getFolder().getLocalBase());
        assertTrue(Files.exists(file1));
        Path file2 = TestHelper
            .createRandomFile(getFolder().getLocalBase().resolve(
                "deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore"));
        assertTrue(Files.exists(file2));
        Path file3 = TestHelper.createRandomFile(getFolder().getLocalBase());
        Path file4 = TestHelper.createRandomFile(getFolder().getLocalBase());

        ScanResult result = scanFolderWaitIfBusy(folderScanner);
        getController().setPaused(true);

        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        Collection<FileInfo> newFiles = result.getNewFiles();
        // new Scan should find 4 + 20 dirs
        assertEquals(result.toString(), 4 + 20, newFiles.size());
        getFolder().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolder());

        // old Scan should find 4 + 20 dirs
        assertEquals(4 + 20, getFolder().getKnownItemCount());

        // delete a file
        Files.delete(file1);

        getController().setPaused(false);
        result = scanFolderWaitIfBusy(folderScanner);
        assertTrue(result.getResultState().toString(),
            ScanResult.ResultState.SCANNED == result.getResultState());

        // one deleted file should be found in new Scanning
        assertEquals("Deleted files: " + result.getDeletedFiles().size(), 1,
            result.getDeletedFiles().size());

        scanFolder(getFolder());

        // one deleted file should be found in old Scanning
        assertEquals("Counted deleted: "
            + countDeleted(getFolder().getKnownFiles()), 1,
            countDeleted(getFolder().getKnownFiles()));

        // change a file
        TestHelper.waitMilliSeconds(3000);
        TestHelper.changeFile(file2);
        result = scanFolderWaitIfBusy(folderScanner);
        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        assertEquals("Changed files wrong: " + result.getChangedFiles().size(),
            1, result.getChangedFiles().size());

        // rename a file
        Files.move(file3, file3.getParent().resolve("newname.txt"));

        // move a file
        Path newFileLocation = file4.getParent().resolve(
            "sub/newname.txt");
        Files.createDirectories(newFileLocation.getParent());
        Files.move(file4, newFileLocation);

        result = scanFolderWaitIfBusy(folderScanner);
        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        // Find a file rename and movement!
        if (Feature.CORRECT_MOVEMENT_DETECTION.isEnabled()) {
            assertEquals(2, result.getMovedFiles().size());
        }
    }

    private ScanResult scanFolderWaitIfBusy(final FolderScanner folderScanner) {
        ScanResult result;
        boolean scannerBusy;
        do {
            result = folderScanner.scanFolder(getFolder());
            scannerBusy = ScanResult.ResultState.BUSY.equals(result
                .getResultState());
            if (scannerBusy) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } while (scannerBusy);
        return result;
    }

    private int countDeleted(Collection<FileInfo> fileInfos) {
        int deletedCount = 0;
        for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.isDeleted()) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

}
