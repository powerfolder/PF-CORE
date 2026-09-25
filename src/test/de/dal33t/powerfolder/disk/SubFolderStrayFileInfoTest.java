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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.nio.file.Path;

/**
 * PFC-3641: A FileInfo of an interrupted subfolder that got into the top folder's database while the barrier
 * was missing is erased by the next scan of the top folder - the root directory's too (PFC-3575).
 */
public class SubFolderStrayFileInfoTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    public void testAScanOfTheTopFolderErasesAStrayFileInfo() {
        Folder topFolder = getFolderAtBart();
        Path reports = topFolder.getPhysicalDir().resolve("projects/reports");
        TestHelper.createRandomFile(reports, "Report.txt");
        scanFolder(topFolder);
        Folder subFolder = topFolder.share((DirectoryInfo) topFolder.getFileInfo("projects/reports"));
        subFolder.setInheritsPermissions(false);
        assertFalse("Sanity: the subfolder is interrupted", subFolder.getInfo().inheritsPermissions());

        // What a scan without the barrier recorded: a file of the subfolder as a FileInfo of the top folder.
        Path strayFile = TestHelper.createRandomFile(reports, "Stray.txt");
        FileInfo stray = FileInfoFactory.newFile(topFolder, strayFile, null,
            getContollerBart().getMySelf().getInfo(), getContollerBart().getMySelf().getAccountInfo(), null, false,
            null);
        topFolder.getDAO().store(null, stray);
        FileInfo root = FileInfoFactory.lookupInstance(topFolder.getInfo(), "projects/reports", true);
        topFolder.getDAO().store(null, FileInfoFactory.newFile(topFolder, reports, null,
            getContollerBart().getMySelf().getInfo(), getContollerBart().getMySelf().getAccountInfo(), null, true,
            null));
        assertNotNull("Sanity: the stray FileInfo is in the top database", topFolder.getDAO().find(stray, null));
        assertNotNull("Sanity: ... and so is a stray FileInfo of the subfolder's root", topFolder.getDAO().find(root, null));

        scanFolder(topFolder);

        assertNull("The stray FileInfo is erased from the top database", topFolder.getDAO().find(stray, null));
        assertNull("... and so is the root's - it has no FileInfo in the top folder (PFC-3575)",
            topFolder.getDAO().find(root, null));
        assertTrue("Nothing is deleted on disk", strayFile.toFile().exists());
        assertNotNull("... and the subfolder still has its file", subFolder.getFileInfo("Report.txt"));
    }
}
