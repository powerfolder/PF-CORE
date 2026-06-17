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
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class RecycleTest extends ControllerTestCase {

    private FileArchiver archiver;

    @BeforeEach
    public void setUp() throws Exception {
        // Remove directories

        super.setUp();

        setupTestFolder(SyncProfile.HOST_FILES);
        Path localbase = getFolder().getLocalBase();
        Path testFile = localbase.resolve("test.txt");
        if (Files.exists(testFile)) {
            try {
                Files.delete(testFile);
            }
            catch (IOException ioe) {
                fail(ioe.getMessage());
            }
        }

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
        archiver = getFolder().getFileArchiver();
    }

    @Test
    public void testRecycleBin() throws IOException {
        FileInfo fileInfo = getFolder().getKnownFiles().iterator().next();
        FileInfo origFile = fileInfo;
        Date lastModified = fileInfo.getModifiedDate();
        Path file = getFolder().getDiskFile(fileInfo);
        assertFalse(archiver.hasArchivedFileInfo(fileInfo));

        TestHelper.waitMilliSeconds(2500);
        getFolder().removeFilesLocal(fileInfo);
        fileInfo = getFolder().getKnownFiles().iterator().next();
        assertFalse(Files.exists(file));
        assertEquals(fileInfo.getModifiedBy(), getController().getMySelf()
            .getInfo());
        assertTrue(fileInfo.getModifiedDate().after(lastModified),
            fileInfo.toDetailString());

        assertTrue(archiver.hasArchivedFileInfo(fileInfo));
        archiver.restore(origFile, file);

        getFolder().scanChangedFile(origFile);
        fileInfo = getFolder().getKnownFiles().iterator().next();

        assertTrue(Files.exists(file));
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertTrue(
            lastModified.before(fileInfo.getModifiedDate()),
            fileInfo.toDetailString() + ": was modified " + lastModified);
        assertEquals( 2, fileInfo.getVersion(),fileInfo.toDetailString());
        assertEquals(fileInfo.getModifiedDate(), getFolder().getKnownFiles()
            .iterator().next().getModifiedDate());
        getFolder().removeFilesLocal(fileInfo);
        assertFalse(Files.exists(file));
        archiver.archive(fileInfo, file, false);
        assertFalse(Files.exists(file));
    }

    @Test
    public void testEmptyRecycleBin() throws IOException {
        FileInfo testfile = getFolder().getKnownFiles().iterator().next();
        Path file = getFolder().getDiskFile(testfile);

        getFolder().removeFilesLocal(testfile);
        assertFalse(Files.exists(file));
        archiver.setVersionsPerFile(0);
        archiver.maintainAndCleanup(null, getFolder().getDAO(), getFolder().getInfo(), getController().getMySelf().getAccountInfo());
        Path recycleBinDir = getFolder().getSystemSubDir().resolve("archive");
        assertTrue(Files.exists(recycleBinDir));
        // Only size file

        DirectoryStream<Path> stream = Files.newDirectoryStream(recycleBinDir);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int count = 0;

        for (Path p : stream) {
            sb.append(p.toString());
            sb.append(", ");
            count++;
        }

        sb.append("]");

        // Only Sizes file should be left there
        assertTrue( count == 1,sb.toString());
    }
}
