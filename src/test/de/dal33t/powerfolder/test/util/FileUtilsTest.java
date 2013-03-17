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
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileUtilsTest extends TestCase {

    public void testURLEncoding() {
        String filename = "PowerFolder.exe";
        String url = "https://www.my-server.com:8822";
        filename = "PowerFolder" + FileUtils.encodeURLinFilename(url) + ".exe";
        String actual = FileUtils.decodeURLFromFilename(filename);
        assertEquals(url, actual);

        url = "https://my.powerfolder.com/cloud004";
        filename = "PowerFolder_Latest_Installer_s_https___my.powerfolder.com_cloud004_(1).exe";
        actual = FileUtils.decodeURLFromFilename(filename);
        assertEquals(url, actual);
    }

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
        assertFalse(FileUtils.isFileInDirectory(testDir, testDir));

        // Test file for directory
        assertFalse(FileUtils.isFileInDirectory(new File("X"), new File("Y")));

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

        File actual3 = FileUtils
            .createEmptyDirectory(
                baseDir,
                FileUtils
                    .removeInvalidFilenameChars("hümmers / rüttenscheiß: Wichtige Doxx|"));
        assertTrue(actual3.exists());
        assertTrue(actual3.isDirectory());
        assertEquals(0, actual3.list().length);
        assertEquals("hümmers  rüttenscheiß Wichtige Doxx", actual3.getName());
    }

    /**
     * Move of ... build/test/a build/test/dir/b build/test/dir/c
     * build/test/dir/sub/d ... to ... build/move/
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
     * Copy of ... build/test/a build/test/dir/b build/test/dir/c
     * build/test/dir/sub/d ... to ... build/copy/
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

    /**
     * Copy build/test/a to build/test/a Should not be permitted.
     * 
     * @throws IOException
     */
    public void testRecursiveCopy() throws IOException {
        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);

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

        // Copy
        File copyDir = new File("build/test/sub");
        boolean success = true;
        try {
            FileUtils.recursiveCopy(baseDir, copyDir);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        assertFalse(success);

        // Copy
        copyDir = new File("build/test/sub/subsub");
        copyDir.mkdirs();

        success = true;
        try {
            FileUtils.recursiveCopy(baseDir, copyDir);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        assertFalse(success);

    }

    /**
     * Move build/test to build/test Should not be permitted.
     * 
     * @throws IOException
     */
    public void testRecursiveMove() throws IOException {
        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);

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

        // Move
        File copyDir = new File("build/test/sub");
        boolean success = true;
        try {
            FileUtils.recursiveMove(baseDir, copyDir);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        assertFalse(success);

        // Move
        copyDir = new File("build/test/sub/subsub");
        copyDir.mkdirs();

        success = true;
        try {
            FileUtils.recursiveMove(baseDir, copyDir);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        assertFalse(success);

    }

    public void testRecursiveMirror() throws IOException {
        File baseDir = new File("build/test");
        FileUtils.recursiveDelete(baseDir);

        // Setup base dir with dirs and files.
        assertTrue(baseDir.mkdirs());
        File source = new File(baseDir, "source");
        assertTrue(source.mkdir());
        File sub = new File(source, "sub");
        assertTrue(sub.mkdir());
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(source, "b");
        TestHelper.createRandomFile(source, "c");
        TestHelper.createRandomFile(sub, "d");

        // Move
        File copyDir = new File("build/test/sub");
        boolean done;
        try {
            FileUtils.recursiveMirror(baseDir, copyDir);
            done = true;
        } catch (IOException e) {
            System.err.println(e.toString());
            done = false;
        }
        assertFalse(done);

        // Move
        copyDir = new File("build/test/sub/subsub");
        copyDir.mkdirs();
        try {
            FileUtils.recursiveMirror(baseDir, copyDir);
            done = true;
        } catch (IOException e) {
            System.err.println(e.toString());
            done = false;
        }
        assertFalse(done);

        // Now actual copy
        File target = new File(baseDir, "target");
        long souceSum = buildCheckSum(source, 0);
        long targetSum = buildCheckSum(target, 0);
        assertFalse(souceSum == targetSum);
        FileUtils.recursiveMirror(source, target);
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertEquals(souceSum, targetSum);
        // Should be OK!

        int nFiles = 1000;
        int nDirs = 0; // Count them
        Set<File> testFiles = new HashSet<File>();
        // Create a initial folder structure
        File currentSubDir = source;
        nDirs++;
        for (int i = 0; i < nFiles; i++) {
            if (Math.random() > 0.95) {
                // Change subdir
                boolean madeDir = false;
                do {
                    int depth = (int) (Math.random() * 10);
                    String fileName = "";
                    for (int j = 0; j < depth; j++) {
                        fileName += TestHelper.createRandomFilename() + "/";
                    }
                    fileName += TestHelper.createRandomFilename();
                    currentSubDir = new File(source, fileName);
                    madeDir = currentSubDir.mkdirs();
                    if (madeDir) {
                        nDirs++;
                    }
                } while (!madeDir);
                System.err.println("New subdir: "
                    + currentSubDir.getAbsolutePath());
            }

            if (!currentSubDir.equals(source)) {
                if (Math.random() > 0.9) {
                    // Go one directory up
                    // System.err.println("Moving up from "
                    // + currentSubDir.getAbsoluteFile());
                    currentSubDir = currentSubDir.getParentFile();
                } else if (Math.random() > 0.95) {
                    // Go one directory up
                    File subDirCanidate = new File(currentSubDir,
                        TestHelper.createRandomFilename());
                    // System.err.println("Moving down to "
                    // + currentSubDir.getAbsoluteFile());
                    if (!subDirCanidate.isFile()) {
                        currentSubDir = subDirCanidate;
                        currentSubDir.mkdirs();
                        nDirs++;
                    }
                }
            }

            File file = TestHelper.createRandomFile(currentSubDir);
            testFiles.add(file);
        }

        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertFalse(souceSum == targetSum);
        FileUtils.recursiveMirror(source, target);
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertEquals(souceSum, targetSum);

        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(target);
        }
        // Again
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertFalse(souceSum == targetSum);
        FileUtils.recursiveMirror(source, target);
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertEquals(souceSum, targetSum);

        final File tempDir = new File(source, "temp");
        assertTrue(tempDir.mkdir());
        final File existingDestDir = new File(target, "shouldRemain");
        assertTrue(existingDestDir.mkdir());
        FileUtils.recursiveMirror(source, target, new FileFilter() {
            public boolean accept(File pathname) {
                return !pathname.equals(tempDir)
                    && !existingDestDir.equals(pathname);
            }
        });
        assertFalse(new File(target, tempDir.getName()).exists());
        assertTrue(existingDestDir.exists());
    }

    private long buildCheckSum(File file, long baseSum) throws IOException {
        baseSum += file.length() + file.getCanonicalPath().length();
        if (file.isDirectory()) {
            for (File subDir : file.listFiles()) {
                baseSum += buildCheckSum(subDir, baseSum);
            }
        }
        return baseSum;
    }

    public void testIsSubdirectory() {
        File parent = new File("parent/");
        parent.mkdir();

        File file = new File("parent/sub/");
        file.mkdir();
        assertTrue(FileUtils.isSubdirectory(parent, file));

        file = new File("parent/sub/subsub");
        file.mkdir();
        assertTrue(FileUtils.isSubdirectory(parent, file));

        file = new File("sub/subsub");
        file.mkdirs();
        assertFalse(FileUtils.isSubdirectory(parent, file));

    }

    public void testBuildFileFromRelativeName() {

        File base = new File("build");

        try {
            FileUtils.buildFileFromRelativeName(null, "");
            assertTrue("Built a file with no base", true);
        } catch (Exception e) {
            // Ignore
        }

        try {
            FileUtils.buildFileFromRelativeName(new File("test.txt"), "");
            assertTrue("Built a file with base file", true);
        } catch (Exception e) {
            // Ignore
        }

        try {
            FileUtils.buildFileFromRelativeName(base, null);
            assertTrue("Built a file with no relative", true);
        } catch (Exception e) {
            // Ignore
        }

        File f = FileUtils.buildFileFromRelativeName(base, "bob");
        assertEquals("Bad file name", "bob", f.getName());
        assertEquals("Bad file name", "build", f.getParent());

        f = FileUtils.buildFileFromRelativeName(base, "bob/jim");
        assertEquals("Bad file name", "jim", f.getName());
        assertTrue("Bad file name", f.getParent().endsWith("bob"));
    }

    /**
     * Test the FileUtils hasFiles method.
     */
    public void testHasFiles() {

        // Test null directory
        try {
            FileUtils.hasFiles(null);
            fail("Should not work on a null");
        } catch (NullPointerException e) {
            // All good.
        }

        // Test empty dir
        File base = new File("build/test/x");
        try {
            FileUtils.recursiveDelete(base);
        } catch (IOException e) {
        }
        assertTrue("Failed to create test dir", base.mkdirs());
        assertFalse("Failed because test dir has files",
            FileUtils.hasFiles(base));
        assertFalse("Failed because test dir has contents",
            FileUtils.hasContents(base));

        // Test with a file
        File randomFile1 = TestHelper.createRandomFile(base, "b");
        assertTrue("Failed because test file not detected",
            FileUtils.hasFiles(base));
        assertTrue("Failed because test file not detected",
            FileUtils.hasContents(base));

        // Test bad directory (a file)
        try {
            FileUtils.hasFiles(randomFile1);
            fail("Should not work on a file");
        } catch (IllegalArgumentException e) {
            // All good.
        }

        // Test bad directory (a file)
        try {
            FileUtils.hasContents(randomFile1);
            fail("Should not work on a file");
        } catch (IllegalArgumentException e) {
            // All good.
        }

        // Test again with file removed
        randomFile1.delete();
        assertFalse("Failed because test file not deleted",
            FileUtils.hasFiles(base));
        assertFalse("Failed because test file not deleted",
            FileUtils.hasContents(base));

        // Test with subdirestory
        File subDir = new File(base, "sub");
        assertTrue("Failed to create test sub dir", subDir.mkdirs());
        assertFalse("Failed because test sub dir has files",
            FileUtils.hasFiles(base));
        assertTrue("Failed because test sub dir has files",
            FileUtils.hasContents(base));

        // Test with a file in subDirectory
        File randomFile2 = TestHelper.createRandomFile(subDir, "c");
        assertTrue("Failed because test file not detected in subDir",
            FileUtils.hasFiles(base));
        assertTrue("Failed because test file not detected in subDir",
            FileUtils.hasContents(base));

        // Test again with file removed from subDirectory
        randomFile2.delete();
        assertFalse("Failed because test file not deleted in subDir",
            FileUtils.hasFiles(base));
        assertTrue("Failed because test file not deleted in subDir",
            FileUtils.hasContents(base));

        // Test with a file in the .PowerFolder dir. Don't care about files
        // here.
        assertTrue(subDir.delete());
        File dotPowerFolderDir = new File(base, ".PowerFolder");
        assertTrue("Failed to create test .PowerFolder dir",
            dotPowerFolderDir.mkdirs());
        File randomFile3 = TestHelper.createRandomFile(dotPowerFolderDir, "c");
        assertFalse(
            "Failed because test file not detected in .PowerFolder dir",
            FileUtils.hasFiles(base));
        assertFalse(
            "Failed because test file not detected in .PowerFolder dir",
            FileUtils.hasContents(base));
        randomFile3.delete();
        assertFalse("Failed because test file not deleted in .PowerFolder dir",
            FileUtils.hasFiles(base));
        assertFalse("Failed because test file not deleted in .PowerFolder dir",
            FileUtils.hasContents(base));

        // Bye
        try {
            FileUtils.recursiveDelete(base);
        } catch (IOException e) {
        }

    }
}
