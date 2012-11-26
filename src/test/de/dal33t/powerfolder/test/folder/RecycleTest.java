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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class RecycleTest extends ControllerTestCase {

    private FileArchiver archiver;

    public void setUp() throws Exception {
        // Remove directries

        super.setUp();

        setupTestFolder(SyncProfile.HOST_FILES);
        File localbase = getFolder().getLocalBase();
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            assertTrue(testFile.delete());
        }

        assertTrue(testFile.createNewFile());

        FileWriter writer = new FileWriter(testFile);
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        scanFolder(getFolder());
        archiver = getFolder().getFileArchiver();
    }

    public void testRecycleBin() throws IOException {
        FileInfo fileInfo = getFolder().getKnownFiles().iterator().next();
        FileInfo origFile = fileInfo;
        Date lastModified = fileInfo.getModifiedDate();
        File file = getFolder().getDiskFile(fileInfo);
        assertFalse(archiver.hasArchivedFileInfo(fileInfo));

        TestHelper.waitMilliSeconds(2500);
        getFolder().removeFilesLocal(fileInfo);
        fileInfo = getFolder().getKnownFiles().iterator().next();
        assertFalse(file.exists());
        assertEquals(fileInfo.getModifiedBy(), getController().getMySelf()
            .getInfo());
        assertTrue(fileInfo.toDetailString(),
            fileInfo.getModifiedDate().after(lastModified));

        assertTrue(archiver.hasArchivedFileInfo(fileInfo));
        archiver.restore(origFile, file);

        getFolder().scanChangedFile(origFile);
        fileInfo = getFolder().getKnownFiles().iterator().next();

        assertTrue(file.exists());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertTrue(
            fileInfo.toDetailString() + ": was modified " + lastModified,
            lastModified.before(fileInfo.getModifiedDate()));
        assertEquals(fileInfo.toDetailString(), 2, fileInfo.getVersion());
        assertEquals(fileInfo.getModifiedDate(), getFolder().getKnownFiles()
            .iterator().next().getModifiedDate());
        getFolder().removeFilesLocal(fileInfo);
        assertFalse(file.exists());
        archiver.archive(fileInfo, file, false);
        assertFalse(file.exists());
    }

    public void testEmptyRecycleBin() {
        FileInfo testfile = getFolder().getKnownFiles().iterator().next();
        File file = getFolder().getDiskFile(testfile);

        getFolder().removeFilesLocal(testfile);
        assertFalse(file.exists());
        (archiver).setVersionsPerFile(0);
        ((CopyOrMoveFileArchiver) archiver).maintain();
        File recycleBinDir = new File(getFolder().getSystemSubDir(), "archive");
        assertTrue(recycleBinDir.exists());
        // Only size file
        assertTrue(Arrays.asList(recycleBinDir.list()).toString(),
            recycleBinDir.list().length == 1);
    }
}
