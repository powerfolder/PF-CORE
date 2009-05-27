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
package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileUtilsTest extends TestCase {

    public void testZipFile() throws IOException {
        byte[] b = new byte[1024 * 1024 * 3];
        for (int i = 0; i < 1024 * 100; i++) {
            b[(int) (Math.random() * b.length)] = (byte) (Math.random() * 256);
        }
        File t = TestHelper.createTestFile(TestHelper.getTestDir(),
            "file.plain", b);
        File zip = new File(TestHelper.getTestDir(), "file.zip");
        FileUtils.zipFile(t, zip);
        assertTrue(zip.exists());
        assertTrue(zip.length() < t.length());

        t = TestHelper.createTestFile(TestHelper.getTestDir(), "file2.txt",
            "Test contents in here, nothing much about it!".getBytes("UTF-8"));
        zip = new File(TestHelper.getTestDir(), "file2.zip");
        FileUtils.zipFile(t, zip);
        assertTrue(zip.exists());
    }

    public void testFileInDirectory() {

        boolean okay = true;
        File testDir = TestHelper.getTestDir();

        // Test null file.
        try {
            FileUtils.isFileInDirectory(null, testDir);
            okay = false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertTrue("Process a null file", okay);

        // Test null directory.
        try {
            FileUtils.isFileInDirectory(new File("x"), null);
            okay = false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertTrue("Process a null directory", okay);

        // Test directory for file
        try {
            FileUtils.isFileInDirectory(testDir, testDir);
            okay = false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertTrue("Process a directory for file", okay);

        // Test file for directory
        try {
            FileUtils.isFileInDirectory(new File("X"), new File("Y"));
            okay = false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertTrue("Process a file for directory", okay);

        // Test file not in directory
        try {
            File dir = new File(testDir, "P");
            dir.mkdir();
            assertTrue(dir.isDirectory());
            okay = FileUtils.isFileInDirectory(new File(testDir, "X"), dir);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertFalse("Process a file not in directory", okay);

        // Test file in directory
        try {
            File dir = new File(testDir, "Q");
            dir.mkdir();
            assertTrue(dir.isDirectory());
            okay = FileUtils.isFileInDirectory(new File(dir, "X"), testDir);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertTrue("Process a file in directory", okay);
    }

    public void testGetValidEmptyDirectory() throws IOException {
        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);

        File actual = FileUtils.createEmptyDirectory(baseDir, "test");
        assertTrue(actual.exists());
        assertTrue(actual.isDirectory());
        assertEquals(0, actual.list().length);
        assertEquals("test", actual.getName());

        File actual2 = FileUtils.createEmptyDirectory(baseDir, "test");
        assertTrue(actual2.exists());
        assertTrue(actual2.isDirectory());
        assertEquals(0, actual2.list().length);
        assertEquals("test (2)", actual2.getName());

        File actual3 = FileUtils.createEmptyDirectory(baseDir,
            "hümmers / rüttenscheiß: Wichtige Doxx|");
        assertTrue(actual3.exists());
        assertTrue(actual3.isDirectory());
        assertEquals(0, actual3.list().length);
        assertEquals("hümmers  rüttenscheiß Wichtige Doxx", actual3.getName());
    }

    /**
     * Move of ...
     *
     * build/test/a
     * build/test/dir/b
     * build/test/dir/c
     * build/test/dir/sub/d
     *
     * ... to ...
     *
     * build/move/
     *
     * @throws IOException
     */
    public void testFileMove() throws IOException {

        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);
        FileUtils.recursiveDelete(new File("build/move"));

        // Setup base dir with dirs and files.
        assertTrue(baseDir.mkdir());
        File dir = new File(baseDir, "dir");
        assertTrue(dir.mkdir());
        File sub = new File(dir, "sub");
        assertTrue(sub.mkdir());
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Move it.
        File moveDir = new File("build/move");
        FileUtils.recursiveMove(baseDir, moveDir);

        // Check move.
        assertTrue(moveDir.exists());
        System.out.println(moveDir.listFiles().length);
        assertTrue(moveDir.listFiles().length == 2); // dir and a
        boolean foundDir = false;
        boolean foundSub = false;
        for (File dirFile : moveDir.listFiles()) {
            if (dirFile.isDirectory()) {
                foundDir = true;
                assertTrue(dirFile.listFiles().length == 3); // b, c, and sub
                for (File subFile : dirFile.listFiles()) {
                    if (subFile.isDirectory()) {
                        foundSub = true;
                        assertTrue(subFile.listFiles().length == 1); // d
                    }
                }
            }
        }
        assertTrue(foundDir);
        assertTrue(foundSub);

        // Check the original is gone.
        assertTrue(!baseDir.exists());
    }

    /**
     * Copy of ...
     *
     * build/test/a
     * build/test/dir/b
     * build/test/dir/c
     * build/test/dir/sub/d
     *
     * ... to ...
     *
     * build/copy/
     *
     * @throws IOException
     */
    public void testFileCopy() throws IOException {

        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);
        FileUtils.recursiveDelete(new File("build/copy"));

        // Setup base dir with dirs and files.
        assertTrue(baseDir.mkdir());
        File dir = new File(baseDir, "dir");
        assertTrue(dir.mkdir());
        File sub = new File(dir, "sub");
        assertTrue(sub.mkdir());
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Copy it.
        File copyDir = new File("build/copy");
        FileUtils.recursiveCopy(baseDir, copyDir);

        // Check copy.
        assertTrue(copyDir.exists());
        assertTrue(copyDir.listFiles().length == 2); // a and dir
        boolean foundDir = false;
        boolean foundSub = false;
        for (File dirFile : copyDir.listFiles()) {
            if (dirFile.isDirectory()) {
                foundDir = true;
                assertTrue(dirFile.listFiles().length == 3); // b, c and sub
                for (File subFile : dirFile.listFiles()) {
                    if (subFile.isDirectory()) {
                        foundSub = true;
                        assertTrue(subFile.listFiles().length == 1); // d
                    }
                }
            }
        }
        assertTrue(foundDir);
        assertTrue(foundSub);

        // Check the original is still there.
        assertTrue(baseDir.exists());
        foundDir = false;
        foundSub = false;
        for (File dirFile : baseDir.listFiles()) { // a and dir
            if (dirFile.isDirectory()) {
                foundDir = true;
                assertTrue(dirFile.listFiles().length == 3); // b, c, and sub
                for (File subFile : dirFile.listFiles()) {
                    if (subFile.isDirectory()) {
                        foundSub = true;
                        assertTrue(subFile.listFiles().length == 1); // d
                    }
                }
            }
        }
        assertTrue(foundDir);
        assertTrue(foundSub);
    }

}
