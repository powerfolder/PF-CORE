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
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Primary because of #1399
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class DownloadPersistenceTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
    }

    public void xtestStoreCompletedDownloadsMultiple() throws Exception {
        for (int i = 0; i < 5; i++) {
            testStoreCompletedDownloads();
            tearDown();
            setUp();
        }
    }

    @Test
    public void testStoreCompletedDownloads() throws IOException {
        final int nFiles = 10;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(nFiles, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getCompletedDownloadsCollection().size() >= nFiles;
            }

            public String message() {
                return "Completed downloads at lisa: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ". Expected: " + nFiles;
            }
        });

        for (FileInfo f : getFolderAtLisa().getKnownFiles()) {
            assertEquals(0, f.getVersion());
        }

        getContollerLisa().shutdown();

        for (DownloadManager dlManager : getContollerLisa()
            .getTransferManager().getCompletedDownloadsCollection())
        {
            assertTrue(dlManager.getTempFile() == null);
            assertTrue(
                dlManager.isCompleted(),"Got state on completed download: "
                + dlManager.getState().getState().toString());
        }

        startControllerLisa();
        connectBartAndLisa();

        TestHelper.waitMilliSeconds(2500);

        for (FileInfo f : getFolderAtLisa().getKnownFiles()) {
            assertEquals(0, f.getVersion());
        }

        for (FileInfo f : getFolderAtBart().getKnownFiles()) {
            assertEquals(0, f.getVersion());
        }

        assertEquals( nFiles, getContollerLisa()
            .getTransferManager().getCompletedDownloadsCollection().size(),"Invalid number of completed downloads: "
            + getContollerLisa().getTransferManager()
                .getCompletedDownloadsCollection());

        for (DownloadManager dlManager : getContollerLisa()
            .getTransferManager().getCompletedDownloadsCollection())
        {
            assertFalse( Files.exists(dlManager.getTempFile()),"Tempfile existing for completed download: "
                + dlManager.getTempFile());
            try {
                Files.createFile(dlManager.getTempFile());
            } catch (IOException ioe) {
                fail("Unable to access temp file: " + dlManager.getTempFile().toString());
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getFolderAtBart().getLocalBase())) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    TestHelper.changeFile(p);
                }
            }
        }
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(nFiles * 5, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getCompletedDownloadsCollection().size() == nFiles * 2;
            }

            public String message() {
                return "Completed dls at lisa: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size();
            }
        });

        scanFolder(getFolderAtBart());
        for (FileInfo fInfo : getFolderAtBart().getKnownFiles()) {
            assertFileMatch(
                fInfo.getDiskFile(getContollerBart().getFolderRepository()),
                fInfo, getContollerBart());
            assertEquals(1, fInfo.getVersion());
            assertTrue(fInfo.getSize() > 0);
        }
        scanFolder(getFolderAtLisa());
        for (FileInfo fInfo : getFolderAtLisa().getKnownFiles()) {
            assertFileMatch(
                fInfo.getDiskFile(getContollerLisa().getFolderRepository()),
                fInfo, getContollerLisa());
            assertEquals(1, fInfo.getVersion());
            assertTrue(fInfo.getSize() > 0);
        }
    }

}
