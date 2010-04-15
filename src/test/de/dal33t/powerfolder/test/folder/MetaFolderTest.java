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
 * $Id: DirectoryTest.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Constants;

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
     * Test that constuctors have enough information.
     */
    public void testMetaFolderSync() {
        if (Feature.META_FOLDER.isEnabled()) {
            joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

            Folder bartFolder = getFolderAtBart();

            // Check the mata folder was created.
            File localBase = bartFolder.getLocalBase();
            File systemSubdir = new File(localBase, Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("bart system subdir does not exists", systemSubdir.exists());
            File metaFolderDir = new File(systemSubdir, Constants.METAFOLDER_SUBDIR);
            assertTrue("bart metaFolder dir does not exists", metaFolderDir.exists());
            File metaFolderSystemSubdir = new File(metaFolderDir, Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("bart metaFolder systemsubdir does not exists", metaFolderSystemSubdir.exists());

            Folder lisaFolder = getFolderAtLisa();

            // Check the mata folder was created.
            localBase = lisaFolder.getLocalBase();
            systemSubdir = new File(localBase, Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("lisa system subdir does not exists", systemSubdir.exists());
            metaFolderDir = new File(systemSubdir, Constants.METAFOLDER_SUBDIR);
            assertTrue("lisa metaFolder dir does not exists", metaFolderDir.exists());
            metaFolderSystemSubdir = new File(metaFolderDir, Constants.POWERFOLDER_SYSTEM_SUBDIR);
            assertTrue("lisa metaFolder systemsubdir does not exists", metaFolderSystemSubdir.exists());

        }
    }
}