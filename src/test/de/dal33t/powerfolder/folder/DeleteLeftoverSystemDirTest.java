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
package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PFC-3646: A share dissolved after its directory was moved leaves its .PowerFolder behind at the new place. Nobody
 * owns it any more, but the deletion skipped it like the database of a running folder - the directory never went,
 * and every delete answered "not deleted".
 */
public class DeleteLeftoverSystemDirTest extends ControllerTestCase {

    private Path base;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupTestFolder(SyncProfile.HOST_FILES);
        base = getFolder().getPhysicalDir();
        TestHelper.createRandomFile(base.resolve("Inspektionsreisen"), "Plan.txt");
        Files.createDirectories(base.resolve("Inspektionsreisen/01 Vortraege"));
        TestHelper.createRandomFile(base.resolve("Inspektionsreisen/01 Vortraege/" + Constants.POWERFOLDER_SYSTEM_SUBDIR),
            "PowerFolder.db");
        scanFolder(getFolder());
    }

    @Test
    public void testDirectoryWithALeftoverSystemDirIsDeleted() {
        getFolder().removeFilesLocal((AccountInfo) null, directory("Inspektionsreisen"));

        assertFalse(Files.exists(base.resolve("Inspektionsreisen")), "Gone on disk, the leftover .PowerFolder with it");
        assertTrue(getFolder().getFileInfo("Inspektionsreisen").isDeleted());
        assertTrue(getFolder().getFileInfo("Inspektionsreisen/01 Vortraege").isDeleted());
    }

    @Test
    public void testOwnSystemDirStays() {
        getFolder().removeFilesLocal((AccountInfo) null, directory("Inspektionsreisen"));

        assertTrue(Files.isDirectory(base.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR)), "The folder's own stays");
    }

    private DirectoryInfo directory(String relativeName) {
        FileInfo fInfo = getFolder().getFileInfo(relativeName);
        assertNotNull(fInfo, "No entry for " + relativeName);
        return (DirectoryInfo) fInfo;
    }
}
