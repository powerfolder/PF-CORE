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
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests if both instance join the same folder by folder id
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderJoinTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    @Test
    public void testJoinSecretFolder() {
        // Join on testfolder
        FolderInfo testFolder = FolderInfoFactory.newTopFolderForTest("testFolder");
        //  testFolder = new FolderInfo("testFolder", IdGenerator.makeFolderId());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    @Test
    public void testJoinMetaFolder() {
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        assertEquals( 1, bartRepo.getFoldersCount(),"Bart count");
        assertEquals( 2, bartRepo.getFoldersCount(true),"Bart count with meta");
        assertEquals( 1, bartRepo.getFolders().size(),"Bart folders");
        assertEquals( 2, bartRepo.getFolders(true)
            .size(),"Bart folders with meta");
        assertTrue( bartRepo.getFolders().contains(
            getFolderAtBart()),"Bart contains");
        assertTrue( bartRepo.getFolders(true)
            .contains(getFolderAtBart()),"Bart contains with meta");

        Folder bartMeta = bartRepo.getMetaFolder(getFolderAtBart()
            .getInfo());
        assertTrue( bartRepo.hasJoinedFolder(bartMeta.getInfo()),"Bart joined");
        assertTrue( bartMeta.getInfo().isMetaFolder(),"Bart is meta");
        assertEquals( SyncProfile.META_FOLDER_SYNC, bartMeta
            .getSyncProfile(),"Bart profile");
        assertEquals( getFolderAtBart(), bartRepo
            .getContentFolder(bartMeta.getInfo()),"Bart parent");
        assertNotNull( bartRepo.getFolder(bartMeta
            .getInfo()),"Bart info not null");
        assertEquals( 2, bartMeta.getMembersCount(),"Bart members");

        // Same tests for lisa
        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        assertEquals( 1, lisaRepo.getFoldersCount(),"Lisa count");
        assertEquals( 2, lisaRepo.getFoldersCount(true),"Lisa count with meta");
        assertEquals( 1, lisaRepo.getFolders().size(),"Lisa folders");
        assertEquals( 2, lisaRepo.getFolders(true)
            .size(),"Lisa folders with meta");
        assertTrue( lisaRepo.getFolders().contains(
            getFolderAtLisa()),"Lisa contains");
        assertTrue( lisaRepo.getFolders(true)
            .contains(getFolderAtLisa()),"Lisa contains with meta");

        Folder lisaMeta = lisaRepo.getMetaFolder(getFolderAtLisa()
            .getInfo());
        assertTrue( lisaRepo.hasJoinedFolder(lisaMeta.getInfo()),"Lisa joined");
        assertTrue( lisaMeta.getInfo().isMetaFolder(),"Lisa is meta");
        assertEquals( SyncProfile.META_FOLDER_SYNC, lisaMeta
            .getSyncProfile(),"Lisa profile");
        assertEquals( getFolderAtLisa(), lisaRepo
            .getContentFolder(lisaMeta.getInfo()),"Lisa parent");
        assertNotNull( lisaRepo.getFolder(lisaMeta
            .getInfo()),"Lisa info not null");
        assertEquals( 2, lisaMeta.getMembersCount(),"Lisa members");
    }

    @Test
    public void testJoinMultipleFolders() {
        getContollerBart().setPaused(true);
        getContollerLisa().setPaused(true);
        int nFolders = 100;
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
            assertEquals( 2, f.getMembersCount(),"Not all members joined on " + f + ". Got: "
                + f.getMembersAsCollection());
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
            assertEquals( 2, folder
                .getMembersCount(),"No two members on barts folder: " + folder);
        }
    }

    private FolderInfo createRandomFolder(String nameSuffix) {
        String folderName = "testFolder-" + nameSuffix;
        // return new FolderInfo(folderName, folderName + IdGenerator.makeFolderId());
        return FolderInfoFactory.newTopFolderForTest(folderName);
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     *
     * @throws FolderException
     * @throws IOException
     */
    @Test
    public void testStartAutoDownload() throws FolderException, IOException {
        FolderInfo testFolder = FolderInfoFactory.newTopFolderForTest("testFolder");

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
    @Test
    public void testStartAutoDownloadInPausedMode() throws FolderException,
        IOException
    {
        FolderInfo testFolder = FolderInfoFactory.newTopFolderForTest("testFolder");
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

    @Test
    public void testReceiveFileListOnReconnect() {
        FolderInfo testFolder = FolderInfoFactory.newTopFolderForTest("testFolder");
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
