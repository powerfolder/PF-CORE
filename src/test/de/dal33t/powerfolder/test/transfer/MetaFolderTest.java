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
 * $Id: MetaFolderTest.java 4282 2008-06-16 03:25:09Z harry $
 */
package de.dal33t.powerfolder.test.transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

/**
 * Test cases for MetaFolder synchronization.
 */
public class MetaFolderTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testSyncSingleFile() {
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        Folder bartMeta = bartRepo.getMetaFolderForParent(getFolderAtLisa()
            .getInfo());
        final Folder lisaMeta = lisaRepo
            .getMetaFolderForParent(getFolderAtLisa().getInfo());
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
    public void testMetaFolderSync() {
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        Folder bartFolder = getFolderAtBart();

        // Check the mata folder was created.
        Path localBase = bartFolder.getLocalBase();
        Path systemSubdir = localBase.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue("bart system subdir does not exist", Files.exists(systemSubdir));
        Path metaFolderDir = systemSubdir.resolve(Constants.METAFOLDER_SUBDIR);
        assertTrue("bart metaFolder dir does not exist", Files.exists(metaFolderDir));
        Path metaFolderSystemSubdir = metaFolderDir.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue("bart metaFolder system subdir does not exist",
            Files.exists(metaFolderSystemSubdir));

        Folder lisaFolder = getFolderAtLisa();

        // Check the meta folder was created.
        localBase = lisaFolder.getLocalBase();
        systemSubdir = localBase.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue("lisa system subdir does not exist", Files.exists(systemSubdir));
        metaFolderDir = systemSubdir.resolve(Constants.METAFOLDER_SUBDIR);
        assertTrue("lisa metaFolder dir does not exist", Files.exists(metaFolderDir));
        metaFolderSystemSubdir = metaFolderDir.resolve(
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        assertTrue("lisa metaFolder system subdir does not exist",
            Files.exists(metaFolderSystemSubdir));

        // Check folders are in repo
        Controller contollerBart = getContollerBart();
        Folder bartMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolderForParent(bartFolder.getInfo());
        assertNotNull("No bart meta folder", bartMetaFolder);

        Folder lisaMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolderForParent(lisaFolder.getInfo());
        assertNotNull("No lisa meta folder", lisaMetaFolder);

        // Check sync between bart and lisa still works.
        int lisaOriginalCount = lisaFolder.getKnownFiles().size();
        TestHelper.createRandomFile(bartFolder.getLocalBase(), "TestFile.txt");
        scanFolder(bartFolder);
        TestHelper.waitMilliSeconds(1000);
        assertEquals("lisa file count wrong: " + lisaFolder.getKnownFiles(),
            lisaOriginalCount + 1, lisaFolder.getKnownFiles().size());
        Controller contollerLisa = getContollerLisa();
        assertTrue("lisa file does not exist", lisaFolder.getKnownFiles()
            .iterator().next().diskFileExists(contollerLisa));

        // Check sync between bart and lisa metafolders works.
        int lisaOriginalMetaCount = lisaMetaFolder.getKnownFiles().size();
        TestHelper.createRandomFile(bartMetaFolder.getLocalBase(),
            "MetaTestFile.txt");
        scanFolder(bartMetaFolder);
        TestHelper.waitMilliSeconds(1000);
        assertEquals(
            "lisa metafolder file count wrong: "
                + lisaMetaFolder.getKnownFiles(), lisaOriginalMetaCount + 1,
            lisaMetaFolder.getKnownFiles().size());
        assertTrue("lisa metafolder file does not exist", lisaMetaFolder
            .getKnownFiles().iterator().next().diskFileExists(contollerLisa));
    }

    /**
     * Test that metaFolders sync parent patterns.
     */
    public void testMetaFolderSyncPatterns() {
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        Folder bartFolder = getFolderAtBart();
        bartFolder.addPattern("test");

        Folder lisaFolder = getFolderAtLisa();
        int initialSize = lisaFolder.getDiskItemFilter().getPatterns().size();

        Controller contollerBart = getContollerBart();
        Folder bartMetaFolder = contollerBart.getFolderRepository()
            .getMetaFolderForParent(bartFolder.getInfo());
        // Wait for Bart's sync patterns to persist.
        TestHelper.waitMilliSeconds(31000);
        scanFolder(bartMetaFolder);
        TestHelper.waitMilliSeconds(1000);

        assertEquals("Wrong number of patterns", initialSize + 1, lisaFolder
            .getDiskItemFilter().getPatterns().size());
    }
}