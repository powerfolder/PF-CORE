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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import static org.junit.jupiter.api.Assertions.*;

public class DetectHardwareFailure extends ControllerTestCase {

    @BeforeEach
    public void setUp() throws Exception {

        super.setUp();

        setupTestFolder(SyncProfile.HOST_FILES);

        Path localbase = getFolder().getLocalBase();
        // create 100 random files
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(localbase);
        }
        Path sub = localbase.resolve("sub");
        Files.createDirectories(sub);

        // create 100 random files in sub folder
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(sub);
        }
    }

    @Test
    public void testHardwareFailure() throws IOException {
        scanFolder(getFolder());
        assertEquals(200, getFolder().getKnownFiles().size());
        // now delete the folder :-D
        getFolder().getFolderWatcher().setIngoreAll(true);
        getController().getFolderRepository().setSuspendNewFolderSearch(true);
        deleteWithRetry(getFolder().getLocalBase());

        getFolder().scanLocalFiles();
        assertEquals(200, getFolder().getKnownFiles().size());
        // on hardware failure of deletion of folder of disk we don't want to
        // mark them as deleted. to prevent the los of files to spread over more
        // systems
        for (FileInfo fileInfo : getFolder().getKnownFiles()) {
            assertFalse(fileInfo.isDeleted());
        }
    }

    private static void deleteWithRetry(Path dir) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                PathUtils.recursiveDelete(dir);
                return;
            } catch (java.nio.file.DirectoryNotEmptyException e) {
                last = e;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException(ie);
                }
            }
        }
        throw last;
    }
}
