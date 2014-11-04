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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * Test that when a folder is confgured not to use the recycle bin, no folders
 * are moved to the recycle bin.
 */
public class NoRecycleTest extends ControllerTestCase {

    @Override
    public void setUp() throws Exception {
        // Remove directries

        super.setUp();

        setupTestFolder(SyncProfile.HOST_FILES);
        Path localbase = getFolder().getLocalBase();
        Path testFile = localbase.resolve("test.txt");
        Files.deleteIfExists(testFile);

        try {
            Files.createFile(testFile);
        }
        catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        BufferedWriter writer = Files.newBufferedWriter(testFile, Charset.forName("UTF-8"));
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        scanFolder(getFolder());
        getFolder().setArchiveVersions(0);
    }

    public void testRecycleBin() {
        FileInfo testfile = getFolder().getKnownFiles().iterator().next();
        Path file = getFolder().getDiskFile(testfile);

        getFolder().removeFilesLocal(testfile);

        // Expecting the local copy to be removed.
        assertFalse(Files.exists(file));

        // Not expecting the bin to have the removed copy.
        assertEquals(0, getFolder().getFileArchiver().getArchivedFilesInfos(
            testfile).size());
    }
}
