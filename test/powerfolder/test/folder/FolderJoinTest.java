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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests if both instance join the same folder by folder id
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderJoinTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testJoinSecretFolder() {
        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeFolderId());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    public void testJoinMetaFolder() {
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        assertEquals("Bart count", 1, bartRepo.getFoldersCount());
        assertEquals("Bart count with meta", 2, bartRepo.getFoldersCount(true));
        assertEquals("Bart folders", 1, bartRepo.getFolders().size());
        assertEquals("Bart folders with meta", 2, bartRepo.getFolders(true)
            .size());
        assertTrue("Bart contains", bartRepo.getFolders().contains(
            getFolderAtBart()));
        assertTrue("Bart contains with meta", bartRepo.getFolders(true)
            .contains(getFolderAtBart()));

        Folder bartMeta = bartRepo.getMetaFolderForParent(getFolderAtBart()
            .getInfo());
        assertTrue("Bart joined", bartRepo.hasJoinedFolder(bartMeta.getInfo()));
        assertTrue("Bart is meta", bartMeta.getInfo().isMetaFolder());
        assertEquals("Bart profile", SyncProfile.META_FOLDER_SYNC, bartMeta
            .getSyncProfile());
        assertEquals("Bart parent", getFolderAtBart(), bartRepo
            .getParentFolder(bartMeta.getInfo()));
        assertNotNull("Bart info not null", bartRepo.getFolder(bartMeta
            .getInfo()));
        assertEquals("Bart members", 2, bartMeta.getMembersCount());

        // Same tests for lisa
        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        assertEquals("Lisa count", 1, lisaRepo.getFoldersCount());
        assertEquals("Lisa count with meta", 2, lisaRepo.getFoldersCount(true));
        assertEquals("Lisa folders", 1, lisaRepo.getFolders().size());
        assertEquals("Lisa folders with meta", 2, lisaRepo.getFolders(true)
            .size());
        assertTrue("Lisa contains", lisaRepo.getFolders().contains(
            getFolderAtLisa()));
        assertTrue("Lisa contains with meta", lisaRepo.getFolders(true)
            .contains(getFolderAtLisa()));

        Folder lisaMeta = lisaRepo.getMetaFolderForParent(getFolderAtLisa()
            .getInfo());
        assertTrue("Lisa joined", lisaRepo.hasJoinedFolder(lisaMeta.getInfo()));
        assertTrue("Lisa is meta", lisaMeta.getInfo().isMetaFolder());
        assertEquals("Lisa profile", SyncProfile.META_FOLDER_SYNC, lisaMeta
            .getSyncProfile());
        assertEquals("Lisa parent", getFolderAtLisa(), lisaRepo
            .getParentFolder(lisaMeta.getInfo()));
        assertNotNull("Lisa info not null", lisaRepo.getFolder(lisaMeta
            .getInfo()));
        assertEquals("Lisa members", 2, lisaMeta.getMembersCount());
    }

    public void testJoinMultipleFolders() {
        getContollerBart().setPaused(true);
        getContollerLisa().setPaused(true);
        int nFolders = 500;
        Folder folder1 = null;
        Folder folder2 = null;
        Collection<Folder> folders = new ArrayList<Folder>();
        for (int i = 0; i < nFolders; i++) {
            FolderInfo testFolder;
            if (nFolders < 10) {
                testFolder = createRandomFolder("r-0" + (i + 1));
            } else {
                testFolder = createRandomFolder("r-" + (i + 1));
            }
            Path folderDirBart = TESTFOLDER_BASEDIR_BART
                .resolve(testFolder.getName());
            Path folderDirLisa = TESTFOLDER_BASEDIR_LISA
                .resolve(testFolder.getName());
            System.err.println("Joining folder: " + testFolder);
            // joinFolder(testFolder, folderDirBart, folderDirLisa);

            FolderSettings folderSettings1 = new FolderSettings(folderDirBart,
                SyncProfile.HOST_FILES, 0);
            folder1 = getContollerBart().getFolderRepository().createFolder(
                testFolder, folderSettings1);
            folder1.addDefaultExcludes();

            FolderSettings folderSettings2 = new FolderSettings(folderDirLisa,
                SyncProfile.HOST_FILES, 0);
            folder2 = getContollerLisa().getFolderRepository().createFolder(
                testFolder, folderSettings2);
            folder2.addDefaultExcludes();
            if (folder1.isDeviceDisconnected()
                || folder2.isDeviceDisconnected())
            {
                fail("Unable to join both controller to " + testFolder + ".");
            }
            folders.add(folder1);
            folders.add(folder2);
        }

        final Folder f1 = folder1;
        final Folder f2 = folder2;
        // Give time to complete join
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return f1.getMembersCount() == 2 && f2.getMembersCount() == 2;
            }
        });

        for (Folder f : folders) {
            assertEquals("Not all members joined on " + f + ". Got: "
                + f.getMembersAsCollection(), 2, f.getMembersCount());
        }

        Collection<Folder> bartsFolders = getContollerBart()
            .getFolderRepository().getFolders();
        Collection<Folder> lisasFolders = getContollerLisa()
            .getFolderRepository().getFolders();
        assertEquals(nFolders, getContollerBart().getFolderRepository()
            .getFoldersCount());
        assertEquals(nFolders, getContollerLisa().getFolderRepository()
            .getFoldersCount());
        assertEquals(nFolders, bartsFolders.size());
        assertEquals(nFolders, lisasFolders.size());
        for (Folder folder : lisasFolders) {
            assertEquals(2, folder.getMembersCount());
        }
        for (Folder folder : bartsFolders) {
            assertEquals("No two members on barts folder: " + folder, 2, folder
                .getMembersCount());
        }
    }

    private FolderInfo createRandomFolder(String nameSuffix) {
        String folderName = "testFolder-" + nameSuffix;
        return new FolderInfo(folderName, folderName + IdGenerator.makeFolderId());
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     *
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownload() throws FolderException, IOException {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeFolderId());

        // Prepare folder on "host" Bart.
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);

        FolderSettings folderSettingsBart = new FolderSettings(
            TESTFOLDER_BASEDIR_BART, SyncProfile.HOST_FILES, 0);
        final Folder folderBart = getContollerBart().getFolderRepository()
            .createFolder(testFolder, folderSettingsBart);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getKnownFiles().size() >= 3;
            }
        });

        // Now let lisa join with auto-download
        FolderSettings folderSettingsLisa = new FolderSettings(
            TESTFOLDER_BASEDIR_LISA, SyncProfile.AUTOMATIC_DOWNLOAD, 0);
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, folderSettingsLisa);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().size() >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownItemCount());
        assertEquals(4, PathUtils.getNumberOfSiblings(folderLisa.getLocalBase()));
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     *
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownloadInPausedMode() throws FolderException,
        IOException
    {
        FolderInfo testFolder = new FolderInfo("testFolder",
            IdGenerator.makeFolderId());
        // Prepare folder on "host" Bart.
        FolderSettings folderSettingsBart = new FolderSettings(
            TESTFOLDER_BASEDIR_BART, SyncProfile.HOST_FILES, 0);
        Folder folderBart = getContollerBart().getFolderRepository()
            .createFolder(testFolder, folderSettingsBart);

        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        scanFolder(folderBart);

        // Set lisa in paused mode
        getContollerLisa().setPaused(true);

        // Now let lisa join with auto-download
        FolderSettings folderSettingsLisa = new FolderSettings(
            TESTFOLDER_BASEDIR_LISA, SyncProfile.AUTOMATIC_DOWNLOAD, 0);
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, folderSettingsLisa);

        getContollerLisa().setPaused(false);
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().size() >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownItemCount());
        assertEquals(4, PathUtils.getNumberOfSiblings(folderLisa.getLocalBase()));
    }

    public void testReceiveFileListOnReconnect() {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeFolderId());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);
        disconnectBartAndLisa();

        // Prepare folder on "host" Bart.
        Folder folderLisa = testFolder.getFolder(getContollerLisa());
        Folder folderBart = testFolder.getFolder(getContollerBart());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        scanFolder(folderBart);

        // Bart has 3 files. Lisa is disconnected not expecting anything
        assertEquals(3, folderBart.getKnownFiles().size());
        assertEquals(0, folderLisa.getKnownFiles().size());
        assertEquals(0, folderLisa.getIncomingFiles().size());

        connectBartAndLisa();
        // Lisa should now know the new files of bart
        assertEquals(3, folderLisa.getIncomingFiles().size());
    }
}
