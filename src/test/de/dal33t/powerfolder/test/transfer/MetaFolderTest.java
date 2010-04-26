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

import java.io.File;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;

/**
 * Test cases for MetaFolder synchronization.
 */
public class MetaFolderTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    /**
     * Test that metafolders sync.
     */
    public void testMetaFolderSync() {
        if (Feature.META_FOLDER.isEnabled()) {
            joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

            Folder bartFolder = getFolderAtBart();

            // Check the mata folder was created.
            File localBase = bartFolder.getLocalBase();
            File systemSubdir = new File(localBase,
                    Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("bart system subdir does not exist",
                    systemSubdir.exists());
            File metaFolderDir = new File(systemSubdir,
                    Constants.METAFOLDER_SUBDIR);
            assertTrue("bart metaFolder dir does not exist",
                    metaFolderDir.exists());
            File metaFolderSystemSubdir = new File(metaFolderDir,
                    Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("bart metaFolder system subdir does not exist",
                    metaFolderSystemSubdir.exists());

            Folder lisaFolder = getFolderAtLisa();

            // Check the mata folder was created.
            localBase = lisaFolder.getLocalBase();
            systemSubdir = new File(localBase,
                    Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("lisa system subdir does not exist",
                    systemSubdir.exists());
            metaFolderDir = new File(systemSubdir,
                    Constants.METAFOLDER_SUBDIR);
            assertTrue("lisa metaFolder dir does not exist",
                    metaFolderDir.exists());
            metaFolderSystemSubdir = new File(metaFolderDir,
                    Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("lisa metaFolder system subdir does not exist",
                    metaFolderSystemSubdir.exists());

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
            TestHelper.createRandomFile(bartFolder.getLocalBase(),
                    "TestFile.txt");
            scanFolder(bartFolder);
            TestHelper.waitMilliSeconds(1000);
            assertEquals("lisa file count wrong", lisaOriginalCount + 1,
                    lisaFolder.getKnownFiles().size());
            Controller contollerLisa = getContollerLisa();
            assertTrue("lisa file does not exist", lisaFolder.getKnownFiles()
                    .iterator().next().diskFileExists(contollerLisa));

            // Check sync between bart and lisa metafolders works.
            int lisaOriginalMetaCount = lisaMetaFolder.getKnownFiles().size();
            TestHelper.createRandomFile(bartMetaFolder.getLocalBase(),
                    "MetaTestFile.txt");
            scanFolder(bartMetaFolder);
            TestHelper.waitMilliSeconds(1000);
            assertEquals("lisa metafolder file count wrong",
                    lisaOriginalMetaCount + 1,
                    lisaMetaFolder.getKnownFiles().size());
            assertTrue("lisa metafolder file does not exist",
                    lisaMetaFolder.getKnownFiles().iterator().next()
                            .diskFileExists(contollerLisa));
        }
    }
}