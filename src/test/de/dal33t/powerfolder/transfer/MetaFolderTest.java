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
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for MetaFolder synchronization.
 */
public class MetaFolderTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    @Test
    public void testSyncSingleFile() {
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        Folder bartMeta = bartRepo.getMetaFolder(getFolderAtLisa()
            .getInfo());
        final Folder lisaMeta = lisaRepo
            .getMetaFolder(getFolderAtLisa().getInfo());
        assertTrue(bartMeta.hasReadPermission(getContollerLisa().getMySelf()));
        assertEquals(2, bartMeta.getMembersCount());
        assertEquals(2, lisaMeta.getMembersCount());

        final int nCount = lisaMeta.getKnownItemCount();
        final Path bartFile = TestHelper.createRandomFile(bartMeta
            .getLocalBase());
        scanFolder(bartMeta);
        final FileInfo fInfo = FileInfoFactory.lookupInstance(bartMeta,
            bartFile);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisaMeta.getKnownItemCount() == nCount + 1
                    && getContollerBart().getTransferManager()
                        .getCompletedUploadsCollection().size() == 0
                    && lisaMeta.hasFile(fInfo);
            }

            public String message() {
                return "Lisa did not download meta data file: " + bartFile;
            }
        });

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                try {
                    Files.delete(bartFile);
                    return true;
                } catch (IOException ioe) {
                    return false;
                }
            }

            public String message() {
                return "Unable to delete file: " + bartFile;
            }
        });

        scanFolder(bartMeta);

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return fInfo.getLocalFileInfo(
                    getContollerLisa().getFolderRepository()).isDeleted();
            }

            public String message() {
                return "Lisa did not delete meta data file: "
                    + fInfo.getLocalFileInfo(getContollerLisa()
                        .getFolderRepository());
            }
        });

    }

    /**
     * Test that metafolders sync.
     */
    @Test
    public void testMetaFolderSync() {
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        Folder bartFolder = getFolderAtBart();

        // Check the mata folder was created.
        Path localBase = bartFolder.getLocalBase();
        Path systemSubdir = localBase.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue( Files.exists(systemSubdir),"bart system subdir does not exist");
        Path metaFolderDir = systemSubdir.resolve(Constants.METAFOLDER_SUBDIR);
        assertTrue( Files.exists(metaFolderDir),"bart metaFolder dir does not exist");
        Path metaFolderSystemSubdir = metaFolderDir.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue(
            Files.exists(metaFolderSystemSubdir),"bart metaFolder system subdir does not exist");

        Folder lisaFolder = getFolderAtLisa();

        // Check the meta folder was created.
        localBase = lisaFolder.getLocalBase();
        systemSubdir = localBase.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue( Files.exists(systemSubdir),"lisa system subdir does not exist");
        metaFolderDir = systemSubdir.resolve(Constants.METAFOLDER_SUBDIR);
        assertTrue( Files.exists(metaFolderDir),"lisa metaFolder dir does not exist");
        metaFolderSystemSubdir = metaFolderDir.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue(
            Files.exists(metaFolderSystemSubdir),"lisa metaFolder system subdir does not exist");

        // Check folders are in repo
        Controller contollerBart = getContollerBart();
        Folder bartMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolder(bartFolder.getInfo());
        assertNotNull( bartMetaFolder,"No bart meta folder");

        Folder lisaMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolder(lisaFolder.getInfo());
        assertNotNull( lisaMetaFolder,"No lisa meta folder");

        // Check sync between bart and lisa still works.
        int lisaOriginalCount = lisaFolder.getKnownFiles().size();
        TestHelper.createRandomFile(bartFolder.getLocalBase(), "TestFile.txt");
        scanFolder(bartFolder);
        TestHelper.waitMilliSeconds(1000);
        assertEquals(
            lisaOriginalCount + 1, lisaFolder.getKnownFiles().size(),"lisa file count wrong: " + lisaFolder.getKnownFiles());
        Controller contollerLisa = getContollerLisa();
        assertTrue( lisaFolder.getKnownFiles()
            .iterator().next().diskFileExists(contollerLisa),"lisa file does not exist");

        // Check sync between bart and lisa metafolders works.
        int lisaOriginalMetaCount = lisaMetaFolder.getKnownFiles().size();
        TestHelper.createRandomFile(bartMetaFolder.getLocalBase(),
            "MetaTestFile.txt");
        scanFolder(bartMetaFolder);
        TestHelper.waitForCondition(10, () -> lisaOriginalMetaCount + 1 == lisaMetaFolder.getKnownFiles().size());
        assertEquals( lisaOriginalMetaCount + 1,
            lisaMetaFolder.getKnownFiles().size(),
            "lisa metafolder file count wrong: "
                + lisaMetaFolder.getKnownFiles());
        TestHelper.waitForCondition(10, () -> lisaMetaFolder
                .getKnownFiles().iterator().next().diskFileExists(contollerLisa));
        assertTrue( lisaMetaFolder
                .getKnownFiles().iterator().next().diskFileExists(contollerLisa),"lisa metafolder file does not exist");
    }

    /**
     * Test that metaFolders sync parent patterns.
     */
    @Test
    public void testMetaFolderSyncPatterns() {
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        Folder lisaFolder = getFolderAtLisa();
        int initialSize = lisaFolder.getDiskItemFilter().getPatterns().size();

        Folder bartFolder = getFolderAtBart();
        bartFolder.addPattern("test");

        Controller contollerBart = getContollerBart();
        Folder bartMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolder(bartFolder.getInfo());
        // Wait for Bart's sync patterns to persist.
        TestHelper.waitMilliSeconds(2000);
        scanFolder(bartMetaFolder);
        TestHelper.waitForCondition(60, () -> initialSize + 1 == lisaFolder
                .getDiskItemFilter().getPatterns().size());

        assertEquals( initialSize + 1, lisaFolder
            .getDiskItemFilter().getPatterns().size(),"Wrong number of patterns");
    }
}
