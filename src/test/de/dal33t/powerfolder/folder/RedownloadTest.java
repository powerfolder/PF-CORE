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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case to ensure that a file is re-downloaded if it is removed from the db
 * and then is available at a peer.
 */
public class RedownloadTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
    }

    @Test
    public void testRedownload() throws IOException {

        final Folder folderBart = getFolderAtBart();

        final Folder folderLisa = getFolderAtLisa();

        // Set up Bart & Lisa with a file in the folder.
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(folderBart);
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Bart known files: " + folderBart.getKnownFiles().size();
            }

            public boolean reached() {
                return folderBart.getKnownFiles().size() == 1;
            }
        });
        scanFolder(folderLisa);
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Lisa known files: " + folderLisa.getKnownFiles().size();
            }

            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor().triggerFileRequesting();
                return folderLisa.getKnownFiles().size() == 1;
            }
        });
        // Wait to complete upload at BART to release file.
        TestHelper.waitMilliSeconds(500);

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
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Bart file exists: " + Files.exists(testFileBart) + ", Bart known files: " + folderBart.getKnownItemCount();
            }

            public boolean reached() {
                getContollerBart().getFolderRepository().getFileRequestor()
                        .triggerFileRequesting(folderBart.getInfo());
                return Files.exists(testFileBart)
                    && folderBart.getKnownItemCount() == 1;
            }
        });

        assertEquals( 1, folderBart.getKnownItemCount(),"Bart file count bad");
        fileInfoBart = folderBart.getKnownFiles().iterator().next();
        assertFileMatch(testFileBart, fileInfoBart, getContollerBart());
        assertEquals( 0, fileInfoBart.getVersion(),"Bart file bad version");
    }
}
