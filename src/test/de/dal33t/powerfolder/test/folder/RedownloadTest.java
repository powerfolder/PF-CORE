/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: RedownloadTest.java 4282 2009-12-12 13:25:09Z harry $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Test case to ensure that a file is re-downloaded if it is removed from the db
 * and then is available at a peer.
 */
public class RedownloadTest extends TwoControllerTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
    }

    public void testRedownload() throws IOException {

        final Folder folderBart = getFolderAtBart();

        final Folder folderLisa = getFolderAtLisa();

        // Set up Bart & Lisa with a file in the folder.
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(folderBart);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getKnownFiles().size() == 1;
            }
        });
        scanFolder(folderLisa);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().size() == 1;
            }
        });
        // Wait to complete upload at BART to release file.
        TestHelper.waitMilliSeconds(500);

        // Delete the file at Bart.
        FileInfo fileInfoBart = folderBart.getKnownFiles().iterator().next();
        final Path testFileBart = fileInfoBart.getDiskFile(getContollerBart()
            .getFolderRepository());
        assertTrue("Bart file should exist", Files.exists(testFileBart));
        try {
            Files.delete(testFileBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        assertFalse("Bart file should not exist", Files.exists(testFileBart));

        scanFolder(folderBart);
        scanFolder(folderLisa);

        assertEquals("Bart file count bad", 1, folderBart.getKnownItemCount());
        assertEquals("Lisa file count bad", 1, folderLisa.getKnownItemCount());
        fileInfoBart = folderBart.getKnownFiles().iterator().next();
        assertTrue("Bart file not deleted", fileInfoBart.isDeleted());
        FileInfo fileInfoLisa = folderLisa.getKnownFiles().iterator().next();
        assertTrue("Lisa file deleted", !fileInfoLisa.isDeleted());

        // Remove Bart's file from the db, so it can be re-downloaded.
        folderBart.removeDeletedFileInfo(fileInfoBart);
        assertSame("Bart still has old file", 0, folderBart.getKnownItemCount());

        // Scan folders. Bart should see Lisa's file and download.
        scanFolder(folderBart);
        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(folderBart.getInfo());
        scanFolder(folderLisa);

        // Wait for copy.
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return Files.exists(testFileBart)
                    && folderBart.getKnownItemCount() == 1;
            }
        });

        assertEquals("Bart file count bad", 1, folderBart.getKnownItemCount());
        fileInfoBart = folderBart.getKnownFiles().iterator().next();
        assertFileMatch(testFileBart, fileInfoBart, getContollerBart());
        assertEquals("Bart file bad version", 0, fileInfoBart.getVersion());
    }
}
