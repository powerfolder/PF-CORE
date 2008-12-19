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

import java.io.File;
import java.util.Collection;

import de.dal33t.powerfolder.disk.Folder;
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
        getController().setSilentMode(true);
        FolderScanner folderScanner = getController().getFolderRepository()
            .getFolderScanner();
        getController().setSilentMode(false);

        File file1 = TestHelper.createRandomFile(getFolder().getLocalBase());
        assertTrue(file1.exists());
        File file2 = TestHelper
            .createRandomFile(new File(
                getFolder().getLocalBase(),
                "deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore"));
        assertTrue(file2.exists());
        File file3 = TestHelper.createRandomFile(getFolder().getLocalBase());
        File file4 = TestHelper.createRandomFile(getFolder().getLocalBase());

        // files to ignore
        File fileTempDownloadFile = new File(getFolder().getLocalBase(),
            "(incomplete) this is a temp download file.whatever");
        fileTempDownloadFile.createNewFile();

        File tempCopieFile = new File(getFolder().getLocalBase(),
            "this is a temp copy file.whatever(copy_temp_powerfolder)");
        tempCopieFile.createNewFile();

        File oldFolderDBFile = new File(getFolder().getLocalBase(),
            Folder.DB_FILENAME);
        oldFolderDBFile.createNewFile();

        File oldFolderDBBakFile = new File(getFolder().getLocalBase(),
            Folder.DB_BACKUP_FILENAME);
        oldFolderDBBakFile.createNewFile();

        ScanResult result = folderScanner.scanFolderWaitIfBusy(getFolder());
        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        Collection<FileInfo> newFiles = result.getNewFiles();
        System.out.println("Scan result: " + result);
        // new Scan should find 4
        assertEquals(result.toString(), 4, newFiles.size());
        getFolder().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolder());

        System.out.print("New files old scanning: ");
        for (FileInfo fileInfo : getFolder().getKnownFiles()) {
            System.out.print(fileInfo + ",");
        }
        System.out.println();
        // old Scan should find 4
        assertEquals(4, getFolder().getKnownFilesCount());

        // delete a file
        file1.delete();

        result = folderScanner.scanFolder(getFolder());
        assertTrue(result.getResultState().toString(),
            ScanResult.ResultState.SCANNED == result.getResultState());

        // one deleted file should be found in new Scanning
        assertEquals(1, result.getDeletedFiles().size());

        scanFolder(getFolder());

        // one deleted file should be found in old Scanning
        assertEquals(1, countDeleted(getFolder().getKnowFilesAsArray()));

        // change a file
        TestHelper.changeFile(file2);
        result = folderScanner.scanFolderWaitIfBusy(getFolder());
        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        assertEquals(1, result.getChangedFiles().size());

        // rename a file
        assertTrue(file3
            .renameTo(new File(file3.getParentFile(), "newname.txt")));

        // move a file
        File newFileLocation = new File(file4.getParentFile(),
            "/sub/newname.txt");
        newFileLocation.getParentFile().mkdirs();
        assertTrue(file4.renameTo(newFileLocation));
        result = folderScanner.scanFolderWaitIfBusy(getFolder());
        assertEquals(ScanResult.ResultState.SCANNED, result.getResultState());

        // Find a file rename and movement!
        assertEquals(2, result.getMovedFiles().size());
    }

    private int countDeleted(FileInfo[] fileInfos) {
        int deletedCount = 0;
        for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.isDeleted()) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

}
