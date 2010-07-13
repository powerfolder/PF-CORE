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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.IdGenerator;
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
            .makeId());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    public void testJoinMetaFolder() {
        if (Feature.META_FOLDER.isDisabled()) {
            return;
        }
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        assertEquals(1, bartRepo.getFoldersCount());
        assertEquals(2, bartRepo.getFoldersCount(true));
        assertEquals(1, bartRepo.getFolders().size());
        assertEquals(2, bartRepo.getFolders(true).size());
        assertTrue(bartRepo.getFolders().contains(getFolderAtBart()));
        assertTrue(bartRepo.getFolders(true).contains(getFolderAtBart()));

        Folder bartMeta = bartRepo.getMetaFolderForParent(getFolderAtBart()
            .getInfo());
        assertTrue(bartRepo.hasJoinedFolder(bartMeta.getInfo()));
        assertTrue(bartMeta.getInfo().isMetaFolder());
        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, bartMeta
            .getSyncProfile());
        assertEquals(getFolderAtBart(), bartRepo.getParentFolder(bartMeta
            .getInfo()));
        assertNotNull(bartRepo.getFolder(bartMeta.getInfo()));
        assertEquals(2, bartMeta.getMembersCount());

        // Same tests for lisa
        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        assertEquals(1, lisaRepo.getFoldersCount());
        assertEquals(2, lisaRepo.getFoldersCount(true));
        assertEquals(1, lisaRepo.getFolders().size());
        assertEquals(2, lisaRepo.getFolders(true).size());
        assertTrue(lisaRepo.getFolders().contains(getFolderAtLisa()));
        assertTrue(lisaRepo.getFolders(true).contains(getFolderAtLisa()));

        Folder lisaMeta = lisaRepo.getMetaFolderForParent(getFolderAtLisa()
            .getInfo());
        assertTrue(lisaRepo.hasJoinedFolder(lisaMeta.getInfo()));
        assertTrue(lisaMeta.getInfo().isMetaFolder());
        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, lisaMeta
            .getSyncProfile());
        assertEquals(getFolderAtLisa(), lisaRepo.getParentFolder(lisaMeta
            .getInfo()));
        assertNotNull(lisaRepo.getFolder(lisaMeta.getInfo()));
        assertEquals(2, lisaMeta.getMembersCount());
    }

    public void testJoinMultipleFolders() {
        getContollerBart().setSilentMode(true);
        getContollerLisa().setSilentMode(true);
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
            File folderDirBart = new File(TESTFOLDER_BASEDIR_BART,
                testFolder.name);
            File folderDirLisa = new File(TESTFOLDER_BASEDIR_LISA,
                testFolder.name);
            System.err.println("Joining folder: " + testFolder);
            // joinFolder(testFolder, folderDirBart, folderDirLisa);

            FolderSettings folderSettings1 = new FolderSettings(folderDirBart,
                SyncProfile.HOST_FILES, false, ArchiveMode.NO_BACKUP, 0);
            folder1 = getContollerBart().getFolderRepository().createFolder(
                testFolder, folderSettings1);
            folder1.addDefaultExcludes();

            FolderSettings folderSettings2 = new FolderSettings(folderDirLisa,
                SyncProfile.HOST_FILES, false, ArchiveMode.NO_BACKUP, 0);
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
        return new FolderInfo(folderName, folderName + IdGenerator.makeId());
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     * 
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownload() throws FolderException {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId());

        // Prepare folder on "host" Bart.
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);

        FolderSettings folderSettingsBart = new FolderSettings(
            TESTFOLDER_BASEDIR_BART, SyncProfile.HOST_FILES, false,
            ArchiveMode.NO_BACKUP, 0);
        final Folder folderBart = getContollerBart().getFolderRepository()
            .createFolder(testFolder, folderSettingsBart);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getKnownFiles().size() >= 3;
            }
        });

        // Now let lisa join with auto-download
        FolderSettings folderSettingsLisa = new FolderSettings(
            TESTFOLDER_BASEDIR_LISA, SyncProfile.AUTOMATIC_DOWNLOAD, false,
            ArchiveMode.NO_BACKUP, 0);
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, folderSettingsLisa);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().size() >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownItemCount());
        assertEquals(4, folderLisa.getLocalBase().list().length);
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     * 
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownloadInSilentMode() throws FolderException {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId());
        // Prepare folder on "host" Bart.
        FolderSettings folderSettingsBart = new FolderSettings(
            TESTFOLDER_BASEDIR_BART, SyncProfile.HOST_FILES, false,
            ArchiveMode.NO_BACKUP, 0);
        Folder folderBart = getContollerBart().getFolderRepository()
            .createFolder(testFolder, folderSettingsBart);

        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        scanFolder(folderBart);

        // Set lisa in silent mode
        getContollerLisa().setSilentMode(true);

        // Now let lisa join with auto-download
        FolderSettings folderSettingsLisa = new FolderSettings(
            TESTFOLDER_BASEDIR_LISA, SyncProfile.AUTOMATIC_DOWNLOAD, false,
            ArchiveMode.NO_BACKUP, 0);
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, folderSettingsLisa);

        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().size() >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownItemCount());
        assertEquals(4, folderLisa.getLocalBase().list().length);
    }

    public void testReceiveFileListOnReconnect() {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId());
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
