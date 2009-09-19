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
package de.dal33t.powerfolder.test.disk;

import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Test cases for the Directory class.
 */
public class DirectoryTest extends ControllerTestCase {

    /**
     * Test that constuctors have enough information.
     */
    public void testConstructor() {
        setupTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        Folder folder = getFolder();
        new Directory(folder, null, "test");

        try {
            new Directory(null, null, "test");
        } catch (NullPointerException e) {
            assertTrue("Failed to detect null folder", true);
        }

        try {
            new Directory(folder, null, null);
        } catch (NullPointerException e) {
            assertTrue("Failed to detect null name", true);
        }
    }

    /**
     * Check that we can get directories by relative name,
     * and that a directory only gets created when there is a file in it.
     *
     * Also does a remove and checks the empty directory goes.
     */
    public void testGetSubDirectory() {
        setupTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        File base = getFolder().getLocalBase();
        // ./a/b/
        File a = new File(base, "a");
        a.mkdir();
        TestHelper.createRandomFile(a);

        File b = new File(a, "b");
        b.mkdirs();
        TestHelper.createRandomFile(b);

        File c = new File(a, "c");
        c.mkdirs();
        // No files in c

        scanFolder(getFolder());
        Directory d = getFolder().getDirectory();
        assertEquals("Wrong number of files", 1, d.getSubdirectories().size());
        assertNotNull(d.getSubDirectory("a"));
        assertNotNull(d.getSubDirectory("a/b"));
        assertNull(d.getSubDirectory("a/c"));
        assertNull(d.getSubDirectory("no"));

        // Remove 'a/x.txt'
        FileInfo fileInfo = getFolder().getDirectory().getSubdirectories()
                .iterator().next().getFileInfosRecursive().get(0);
        d.removeFileInfo(fileInfo);

        // a should be gone because it is empty.
        assertNull(d.getSubDirectory("a"));
    }

    public void testGetFileInfos() {
        setupTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        File base = getFolder().getLocalBase();
        // ./a/b/
        File a = new File(base, "a");
        a.mkdir();
        TestHelper.createRandomFile(a);

        File b = new File(a, "b");
        b.mkdirs();
        TestHelper.createRandomFile(b);

        File c = new File(a, "c");
        c.mkdirs();
        // No files in c

        scanFolder(getFolder());
        Directory d = getFolder().getDirectory();

        // None in root
        assertEquals("Wrong number of directories", 0, d.getFileInfos().size());
        // Two in subdirectories
        assertEquals("Wrong number of recursive directories", 2,
                d.getFileInfosRecursive().size());
    }

    /**
     * Test that filenameOnly and relativeNAme are correct.
     */
    public void testNames() {
        setupTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        File base = getFolder().getLocalBase();
        // ./a/b/
        File a = new File(base, "a");
        a.mkdir();
        TestHelper.createRandomFile(a);

        File b = new File(a, "B");
        b.mkdirs();
        TestHelper.createRandomFile(b);

        scanFolder(getFolder());

        Directory d = getFolder().getDirectory();
        Directory da = d.getSubDirectory("a");
        Directory dB = da.getSubDirectory("B");

        assertEquals("Bad filenameOnly", "", d.getFilenameOnly());
        assertEquals("Bad relativeName", "", d.getRelativeName());
        assertEquals("Bad filenameOnly a", "a", da.getFilenameOnly());
        assertEquals("Bad relativeName a", "a", da.getRelativeName());
        assertEquals("Bad filenameOnly b", "B", dB.getFilenameOnly());
        assertEquals("Bad relativeName b", "a/B", dB.getRelativeName());

        // Check a file.
        FileInfo fileInfo = dB.getFileInfosRecursive().get(0);
        assertTrue("Bad file name",
                fileInfo.getRelativeName().startsWith("a/B/"));

        assertEquals("Bad toString", "B", dB.toString());
        assertEquals("Bad lowerCaseFilenameOnly", "b",
                dB.getLowerCaseFilenameOnly());

    }

}
