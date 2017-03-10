package de.dal33t.powerfolder.test.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.dal33t.powerfolder.disk.EncryptedFileSystemUtils;
import junit.framework.TestCase;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.TestHelper;

public class PathUtilsTest extends TestCase {

    private Path baseDir = Paths.get("build/test").toAbsolutePath();

    public void testURLEncoding() {
        String filename = "PowerFolder.exe";
        String url = "https://www.my-server.com:8822";
        filename = "PowerFolder" + PathUtils.encodeURLinFilename(url) + ".exe";
        String actual = PathUtils.decodeURLFromFilename(filename);
        assertEquals(url, actual);

        url = "https://access.powerfolder.com/cloud004";
        filename = "PowerFolder_Latest_Installer_s_https___access.powerfolder.com_cloud004_(1).exe";
        actual = PathUtils.decodeURLFromFilename(filename);
        assertEquals(url, actual);
    }

    public void testZipFile() throws IOException {
        byte[] b = new byte[1024 * 1024 * 3];
        for (int i = 0; i < 1024 * 100; i++) {
            b[(int) (Math.random() * b.length)] = (byte) (Math.random() * 256);
        }
        Path t = TestHelper.createTestFile(TestHelper.getTestDir(),
            "file.plain", b);
        Path zip = TestHelper.getTestDir().resolve("file.zip");
        PathUtils.zipFile(t, zip);
        assertTrue(Files.exists(zip));
        assertTrue(Files.size(zip) < Files.size(t));

        t = TestHelper.createTestFile(TestHelper.getTestDir(), "file2.txt",
            "Test contents in here, nothing much about it!".getBytes("UTF-8"));
        zip = TestHelper.getTestDir().resolve("file2.zip");
        PathUtils.zipFile(t, zip);
        assertTrue(Files.exists(zip));
    }

    public void testFileInDirectory() {

        boolean okay;
        Path testDir = TestHelper.getTestDir();

        // Test null file.
        try {
            PathUtils.isFileInDirectory(null, testDir);
            okay = false;
        } catch (IllegalArgumentException e) {
            okay = true;
        }
        assertTrue("Process a null file", okay);

        // Test null directory.
        try {
            PathUtils.isFileInDirectory(Paths.get("x"), null);
            okay = false;
        } catch (IllegalArgumentException e) {
            okay = true;
        }
        assertTrue("Process a null directory", okay);

        // Test directory for file
        assertFalse(PathUtils.isFileInDirectory(testDir, testDir));

        // Test file for directory
        assertFalse(PathUtils.isFileInDirectory(Paths.get("X"), Paths.get("Y")));

        // Test file not in directory
        try {
            Path dir = testDir.resolve("P");
            Files.createDirectories(dir);
            assertTrue(Files.isDirectory(dir));
            okay = PathUtils.isFileInDirectory(testDir.resolve("X"), dir);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        assertFalse("Process a file not in directory", okay);

        // Test file in directory
        try {
            Path dir = testDir.resolve("Q");
            Files.createDirectories(dir);
            assertTrue(Files.isDirectory(dir));
            okay = PathUtils.isFileInDirectory(dir.resolve("X"), testDir);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        assertTrue("Process a file in directory", okay);
    }

    public void testIsSameName() {
        assertTrue(PathUtils.isSameName("abc123", "abc123"));
        assertTrue(PathUtils.isSameName("+abc123", "+abc123"));
        assertTrue(PathUtils.isSameName("abc123", "abc123 ()"));
        assertTrue(PathUtils.isSameName("abc123←", "abc123← ()"));
        assertTrue(PathUtils.isSameName("abc123 ()", "abc123"));
        assertTrue(PathUtils.isSameName("abc123", "abc123 (2098324)"));
        assertTrue(PathUtils.isSameName("abc123 (lksjdflknsef)", "abc123"));
        assertTrue(PathUtils.isSameName("abc123", "abc123 ()"));
        assertTrue(PathUtils.isSameName("abc123 ()", "abc123"));
        assertTrue(PathUtils.isSameName("abc123 (123)", "abc123 (123)"));
        assertFalse(PathUtils.isSameName("abc123", "abc123 ("));
        assertFalse(PathUtils.isSameName("foo", "bar"));
        assertFalse(PathUtils.isSameName("foo (123)", "bar (123)"));
        assertFalse(PathUtils.isSameName("foo", "bar (123)"));
        assertFalse(PathUtils.isSameName("foo (123)", "bar"));
    }

    public void testGetValidEmptyDirectory() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);

        Path actual = PathUtils.createEmptyDirectory(baseDir.resolve("test"));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isDirectory(actual));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual));
        assertEquals("test", actual.getFileName().toString());

        Path actual2 = PathUtils.createEmptyDirectory(baseDir.resolve("test"));
        assertTrue(Files.exists(actual2));
        assertTrue(Files.isDirectory(actual2));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual2));
        assertEquals("test (2)", actual2.getFileName().toString());

        Path actual3 = PathUtils
            .createEmptyDirectory(
                baseDir.resolve(PathUtils.removeInvalidFilenameChars("hümmers / rüttenscheiß: Wichtige Doxx|.....")));
        assertTrue(Files.exists(actual3));
        assertTrue(Files.isDirectory(actual3));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual3));
        assertEquals("hümmers  rüttenscheiß Wichtige Doxx", actual3
            .getFileName().toString());

        assertEquals("", PathUtils.removeInvalidFilenameChars("....."));
    }

    public void testGetValidEmptyDirectoryWithRawName() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);

        Path actual = PathUtils.createEmptyDirectory(baseDir, "test");
        assertTrue(Files.exists(actual));
        assertTrue(Files.isDirectory(actual));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual));
        assertEquals("test", actual.getFileName().toString());

        Path actual2 = PathUtils.createEmptyDirectory(baseDir, "test");
        assertTrue(Files.exists(actual2));
        assertTrue(Files.isDirectory(actual2));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual2));
        assertEquals("test (2)", actual2.getFileName().toString());

        Path actual3 = PathUtils
            .createEmptyDirectory(
                baseDir,
                PathUtils
                    .removeInvalidFilenameChars("hümmers / rüttenscheiß: Wichtige Doxx|....."));
        assertTrue(Files.exists(actual3));
        assertTrue(Files.isDirectory(actual3));
        assertEquals(0, PathUtils.getNumberOfSiblings(actual3));
        assertEquals("hümmers  rüttenscheiß Wichtige Doxx", actual3
            .getFileName().toString());

        assertEquals("", PathUtils.removeInvalidFilenameChars("....."));
    }

    /**
     * Move of ... build/test/a build/test/dir/b build/test/dir/c
     * build/test/dir/sub/d ... to ... build/move/
     * 
     * @throws IOException
     */
    public void testFileMove() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);
        PathUtils.recursiveDelete(Paths.get("build/move").toAbsolutePath());

        // Setup base dir with dirs and files.
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        Path dir = baseDir.resolve("dir");
        try {
            Files.createDirectories(dir);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        Path sub = dir.resolve("sub");
        try {
            Files.createDirectories(sub);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Move it.
        Path moveDir = Paths.get("build/move").toAbsolutePath();
        Files.move(baseDir, moveDir);
        // PathUtils.recursiveMove(baseDir, moveDir);

        // Check move.
        assertTrue(Files.exists(moveDir));
        int count = 0;
        try (DirectoryStream<Path> moveStream = Files
            .newDirectoryStream(moveDir)) {
            boolean foundDir = false;
            boolean foundSub = false;
            for (Path dirFile : moveStream) {
                count++;
                if (Files.isDirectory(dirFile)) {
                    foundDir = true;
                    try (DirectoryStream<Path> dirStream = Files
                        .newDirectoryStream(dirFile)) {
                        int dirCount = 0;
                        for (Path subFile : dirStream) {
                            dirCount++;
                            if (Files.isDirectory(subFile)) {
                                foundSub = true;
                                try (DirectoryStream<Path> subStream = Files
                                    .newDirectoryStream(subFile)) {
                                    Iterator<Path> it = subStream.iterator();
                                    it.next(); // d
                                    assertTrue(!it.hasNext()); // d
                                }
                            }
                        }
                        assertTrue(dirCount == 3); // b, c, and sub
                    }
                }
            }
            System.out.println(count);
            assertTrue(count == 2); // dir and a
            assertTrue(foundDir);
            assertTrue(foundSub);
        }

        // Check the original is gone.
        assertTrue(Files.notExists(baseDir));
    }

    /**
     * Copy of ... build/test/a build/test/dir/b build/test/dir/c
     * build/test/dir/sub/d ... to ... build/copy/
     * 
     * @throws IOException
     */
    public void testFileCopy() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);
        PathUtils.recursiveDelete(Paths.get("build/copy").toAbsolutePath());

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path dir = baseDir.resolve("dir");
        Files.createDirectories(dir);
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Copy it.
        Path copyDir = Paths.get("build/copy").toAbsolutePath();
        PathUtils.recursiveCopy(baseDir, copyDir);

        // Check copy.
        assertTrue(Files.exists(copyDir));
        assertTrue(PathUtils.getNumberOfSiblings(copyDir) == 2); // a and dir
        boolean foundDir = false;
        boolean foundSub = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(copyDir)) {
            for (Path dirFile : stream) {
                if (Files.isDirectory(dirFile)) {
                    foundDir = true;
                    assertTrue(PathUtils.getNumberOfSiblings(dirFile) == 3); // b,
                                                                             // c
                                                                             // and
                                                                             // sub

                    try (DirectoryStream<Path> subStream = Files
                        .newDirectoryStream(dirFile)) {
                        for (Path subFile : subStream) {
                            if (Files.isDirectory(subFile)) {
                                foundSub = true;
                                assertTrue(PathUtils
                                    .getNumberOfSiblings(subFile) == 1); // d
                            }
                        }
                    }
                }
            }

            assertTrue(foundDir);
            assertTrue(foundSub);
        }

        // Check the original is still there.
        assertTrue(Files.exists(baseDir));
        foundDir = false;
        foundSub = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path dirFile : stream) { // a and dir
                if (Files.isDirectory(dirFile)) {
                    foundDir = true;
                    assertTrue(PathUtils.getNumberOfSiblings(dirFile) == 3); // b,
                                                                             // c,
                                                                             // and
                                                                             // sub

                    try (DirectoryStream<Path> subStream = Files
                        .newDirectoryStream(dirFile)) {
                        for (Path subFile : subStream) {
                            if (Files.isDirectory(subFile)) {
                                foundSub = true;
                                assertTrue(PathUtils
                                    .getNumberOfSiblings(subFile) == 1); // d
                            }
                        }
                    }
                }
            }

            assertTrue(foundDir);
            assertTrue(foundSub);
        }
    }

    /**
     * Copy build/test/a to build/test/a Should not be permitted.
     * 
     * @throws IOException
     */
    public void testRecursiveCopy() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path dir = baseDir.resolve("dir");
        Files.createDirectories(dir);
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Copy
        Path copyDir = Paths.get("build/test/sub").toAbsolutePath();
        boolean success = true;
        try {
            PathUtils.recursiveCopy(baseDir, copyDir);
        } catch (IOException e) {
            success = false;
        }

        assertFalse(success);

        // Copy
        copyDir = Paths.get("build/test/sub/subsub").toAbsolutePath();
        Files.createDirectories(copyDir);

        success = true;
        try {
            PathUtils.recursiveCopy(baseDir, copyDir);
        } catch (IOException e) {
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
        Path baseDir = Paths.get("build/test");
        PathUtils.recursiveDelete(baseDir);

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path dir = baseDir.resolve("dir");
        Files.createDirectories(dir);
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Move
        Path copyDir = Paths.get("build/test/sub");
        boolean success = true;
        try {
            PathUtils.recursiveMove(baseDir, copyDir);
        } catch (IOException e) {
            success = false;
        }

        assertFalse(success);

        // Move
        copyDir = Paths.get("build/test/sub/subsub");
        Files.createDirectories(copyDir);

        success = true;
        try {
            PathUtils.recursiveMove(baseDir, copyDir);
        } catch (IOException e) {
            success = false;
        }

        assertFalse(success);
    }

    public void testRecursiveMirror() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDelete(baseDir);

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path source = baseDir.resolve("source");
        Files.createDirectories(source);
        Path sub = source.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(source, "b");
        TestHelper.createRandomFile(source, "c");
        TestHelper.createRandomFile(sub, "d");

        // Move
        Path copyDir = Paths.get("build/test/sub").toAbsolutePath();
        boolean done;
        try {
            PathUtils.recursiveMirror(baseDir, copyDir);
            done = true;
        } catch (IOException e) {
            System.err.println(e.toString());
            done = false;
        }
        assertFalse(done);

        // Move
        copyDir = Paths.get("build/test/sub/subsub").toAbsolutePath();
        Files.createDirectories(copyDir);
        try {
            PathUtils.recursiveMirror(baseDir, copyDir);
            done = true;
        } catch (IOException e) {
            System.err.println(e.toString());
            done = false;
        }
        assertFalse(done);

        // Now actual copy
        Path target = baseDir.resolve("target");
        long souceSum = buildCheckSum(source, 0);
        long targetSum = buildCheckSum(target, 0);
        assertFalse(souceSum == targetSum);
        PathUtils.recursiveMirror(source, target);
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertEquals(souceSum, targetSum);
        // Should be OK!

        int nFiles = 1000;
        Set<Path> testFiles = new HashSet<Path>();
        // Create a initial folder structure
        Path currentSubDir = source;
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
                    currentSubDir = source.resolve(fileName);
                    try {
                        Files.createDirectories(currentSubDir);
                        madeDir = true;
                    } catch (IOException ioe) {
                        madeDir = false;
                    }
                } while (!madeDir);
                System.err.println("New subdir: "
                    + currentSubDir.toAbsolutePath());
            }

            if (!currentSubDir.equals(source)) {
                if (Math.random() > 0.9) {
                    // Go one directory up
                    // System.err.println("Moving up from "
                    // + currentSubDir.getAbsoluteFile());
                    currentSubDir = currentSubDir.getParent();
                } else if (Math.random() > 0.95) {
                    // Go one directory up
                    Path subDirCanidate = currentSubDir.resolve(TestHelper
                        .createRandomFilename());
                    // System.err.println("Moving down to "
                    // + currentSubDir.getAbsoluteFile());
                    if (!Files.isRegularFile(subDirCanidate)) {
                        currentSubDir = subDirCanidate;
                        Files.createDirectories(currentSubDir);
                    }
                }
            }

            Path file = TestHelper.createRandomFile(currentSubDir);
            testFiles.add(file);
        }

        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertFalse(souceSum == targetSum);
        PathUtils.recursiveMirror(source, target);
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
        PathUtils.recursiveMirror(source, target);
        souceSum = buildCheckSum(source, 0);
        targetSum = buildCheckSum(target, 0);
        assertEquals(souceSum, targetSum);

        final Path tempDir = source.resolve("temp");
        Files.createDirectories(tempDir);
        final Path existingDestDir = target.resolve("shouldRemain");
        Files.createDirectories(existingDestDir);
        PathUtils.recursiveMirror(source, target, new Filter<Path>() {
            @Override
            public boolean accept(Path pathname) {
                return !pathname.equals(tempDir)
                    && !existingDestDir.equals(pathname);
            }
        });
        assertFalse(Files.exists(target.resolve(tempDir.getFileName())));
        assertTrue(Files.exists(existingDestDir));
    }

    private long buildCheckSum(Path file, long baseSum) throws IOException {
        try {
            baseSum += Files.size(file);
        } catch (IOException ioe) {
            // Ignore.
        }
        try {
            baseSum += Files.size(file.toRealPath());
        } catch (IOException ioe) {
            // Ignore.
        }
        if (Files.isDirectory(file)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
                for (Path entry : stream) {
                    baseSum += buildCheckSum(entry, baseSum);
                }
            }
        }
        return baseSum;
    }

    public void testIsSubdirectory() throws IOException {
        Path parent = Paths.get("parent");
        Files.createDirectories(parent);

        Path file = Paths.get("parent/sub");
        Files.createDirectories(file);
        assertTrue(PathUtils.isSubdirectory(parent, file));

        file = Paths.get("parent/sub/subsub");
        Files.createDirectories(file);
        assertTrue(PathUtils.isSubdirectory(parent, file));

        file = Paths.get("sub/subsub");
        Files.createDirectories(file);
        assertFalse(PathUtils.isSubdirectory(parent, file));
    }

    public void testBuildFileFromRelativeName() {
        Path base = Paths.get("build");

        try {
            PathUtils.buildFileFromRelativeName(null, "");
            assertTrue("Built a file with no base", true);
        } catch (Exception e) {
            // Ignore
        }

        try {
            PathUtils.buildFileFromRelativeName(Paths.get("test.txt"), "");
            assertTrue("Built a file with base file", true);
        } catch (Exception e) {
            // Ignore
        }

        try {
            PathUtils.buildFileFromRelativeName(base, null);
            assertTrue("Built a file with no relative", true);
        } catch (Exception e) {
            // Ignore
        }

        Path f = PathUtils.buildFileFromRelativeName(base, "bob");
        assertEquals("Bad file name", "bob", f.getFileName().toString());
        assertEquals("Bad file name", "build", f.getParent().getFileName()
            .toString());

        f = PathUtils.buildFileFromRelativeName(base, "bob/jim");
        assertEquals("Bad file name", "jim", f.getFileName().toString());
        assertTrue("Bad file name", f.getParent().toString().endsWith("bob"));
    }

    /**
     * Test the FileUtils hasFiles method.
     */
    public void testHasFiles() throws IOException {
        // Test null directory
        try {
            PathUtils.hasFiles(null);
            fail("Should not work on a null");
        } catch (NullPointerException e) {
            // All good.
        }

        // Test empty dir
        Path base = Paths.get("build/test/x");
        try {
            PathUtils.recursiveDelete(base);
        } catch (IOException e) {
        }
        Files.createDirectories(base);
        assertFalse("Failed because test dir has files",
            PathUtils.hasFiles(base));
        assertFalse("Failed because test dir has contents",
            PathUtils.hasContents(base));

        // Test with a file
        Path randomFile1 = TestHelper.createRandomFile(base, "b");
        assertTrue("Failed because test file not detected",
            PathUtils.hasFiles(base));
        assertTrue("Failed because test file not detected",
            PathUtils.hasContents(base));

        // Test bad directory (a file)
        try {
            PathUtils.hasFiles(randomFile1);
            fail("Should not work on a file");
        } catch (IllegalArgumentException e) {
            // All good.
        }

        // Test bad directory (a file)
        try {
            PathUtils.hasContents(randomFile1);
            fail("Should not work on a file");
        } catch (IllegalArgumentException e) {
            // All good.
        }

        // Test again with file removed
        Files.delete(randomFile1);
        assertFalse("Failed because test file not deleted",
            PathUtils.hasFiles(base));
        assertFalse("Failed because test file not deleted",
            PathUtils.hasContents(base));

        // Test with subdirestory
        Path subDir = base.resolve("sub");
        Files.createDirectories(subDir);
        assertFalse("Failed because test sub dir has files",
            PathUtils.hasFiles(base));
        assertTrue("Failed because test sub dir has files",
            PathUtils.hasContents(base));

        // Test with a file in subDirectory
        Path randomFile2 = TestHelper.createRandomFile(subDir, "c");
        assertTrue("Failed because test file not detected in subDir",
            PathUtils.hasFiles(base));
        assertTrue("Failed because test file not detected in subDir",
            PathUtils.hasContents(base));

        // Test again with file removed from subDirectory
        Files.delete(randomFile2);
        assertFalse("Failed because test file not deleted in subDir",
            PathUtils.hasFiles(base));
        assertTrue("Failed because test file not deleted in subDir",
            PathUtils.hasContents(base));

        // Test with a file in the .PowerFolder dir. Don't care about files
        // here.
        Files.delete(subDir);
        Path dotPowerFolderDir = base.resolve(".PowerFolder");
        Files.createDirectories(dotPowerFolderDir);
        Path randomFile3 = TestHelper.createRandomFile(dotPowerFolderDir, "c");
        assertFalse(
            "Failed because test file not detected in .PowerFolder dir",
            PathUtils.hasFiles(base));
        assertFalse(
            "Failed because test file not detected in .PowerFolder dir",
            PathUtils.hasContents(base));
        Files.delete(randomFile3);
        assertFalse("Failed because test file not deleted in .PowerFolder dir",
            PathUtils.hasFiles(base));
        assertFalse("Failed because test file not deleted in .PowerFolder dir",
            PathUtils.hasContents(base));

        // Bye
        try {
            PathUtils.recursiveDelete(base);
        } catch (IOException e) {
        }
    }

    public void testRawCopy() throws IOException {
        Path s = TestHelper.createRandomFile(Paths.get("build/test/x"), 1);
        Path t = TestHelper.createRandomFile(Paths.get("build/test/x"), 1);
        PathUtils.rawCopy(s, t);
        assertEquals(Files.size(s), Files.size(t));
    }

    public void testNetworkDrive() {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        assertFalse(PathUtils.isNetworkPath(Paths.get("C:\\")));
        assertFalse(PathUtils.isNetworkPath(Paths.get("C:\\subdir\\subdir2")));

        assertFalse(PathUtils.isNetworkPath(Paths
            .get("/home/user/PowerFolders/123")));

        // assertTrue(PathUtils.isNetworkPath(Paths.get("N:\\")));
        // assertTrue(PathUtils.isNetworkPath(Paths.get("N:\\subdir\\subdir2")));

        assertTrue(PathUtils.isNetworkPath(Paths.get("\\\\server\\share")));
        assertTrue(PathUtils.isNetworkPath(Paths
            .get("\\\\server\\share\\subdir")));
    }

    /**
     * Copy build/test/a to build/test/a Should not be permitted.
     *
     * @throws IOException
     */
    public void testRecursiveMoveVisitor() throws IOException {

        Path baseDir = Paths.get("build/test").toAbsolutePath();
        if (Files.exists(baseDir)) {
            PathUtils.recursiveDeleteVisitor(baseDir);
        }

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path dir = baseDir.resolve("dir");
        Files.createDirectories(dir);
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        // Now check the real move function.
        Path moveDir = baseDir.resolve("moveDir");
        PathUtils.recursiveMoveVisitor(dir, moveDir);

        assertTrue(Files.notExists(dir));

        assertTrue(Files.exists(moveDir.resolve("b")));
        assertTrue(Files.exists(moveDir.resolve("c")));
        assertTrue(Files.exists(moveDir.resolve("sub")) && Files.isDirectory(moveDir.resolve("sub")));
        assertTrue(Files.exists(moveDir.resolve("sub").resolve("d")));

        assertFalse(Files.exists(dir.resolve("b")));
        assertFalse(Files.exists(dir.resolve("c")));
        assertFalse(Files.exists(dir.resolve("sub").resolve("d")));

    }

    /**
     * Copy build/test/a to build/test/a Should not be permitted.
     *
     * @throws IOException
     */
    public void testRecursiveCopyVisitor() throws IOException {
        Path testDir = Paths.get("build/test").toAbsolutePath();

        // Setup base dir with dirs and files.
        Files.createDirectories(testDir);
        Path source = testDir.resolve("source");
        Files.createDirectories(source);
        Path sub = source.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(testDir, "a");
        TestHelper.createRandomFile(source, "b");
        TestHelper.createRandomFile(source, "c");
        TestHelper.createRandomFile(sub, "d");

        TestHelper.waitMilliSeconds(200);

        Long[] sourceDirectorySizeBeforeCopy = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeCopy = sourceDirectorySizeBeforeCopy[0];
        long sourceSizeCountBeforeCopy = sourceDirectorySizeBeforeCopy[1];

        // Test 1
        Path target = testDir.resolve("target");

        try {
            PathUtils.recursiveCopyVisitor(source, target);
        } catch (FileAlreadyExistsException e) {
            fail();
        }

        Long[] targetDirectorySizeAfterCopy = PathUtils.calculateDirectorySizeAndCount(target);
        long targetSizeBytesAfterCopy = targetDirectorySizeAfterCopy[0];
        long targetSizeCountBeforeCopy = targetDirectorySizeAfterCopy[1];

        assertTrue(sourceSizeBytesBeforeCopy == targetSizeBytesAfterCopy);
        assertTrue(sourceSizeCountBeforeCopy == targetSizeCountBeforeCopy);

        // Test 2
        TestHelper.createRandomFile(source, "f");
        TestHelper.createRandomFile(source, "b");

        Long[] sourceDirectorySizeBeforeCopy2 = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeCopy2 = sourceDirectorySizeBeforeCopy2[0];
        long sourceSizeCountBeforeCopy2 = sourceDirectorySizeBeforeCopy2[1];

        target = testDir.resolve("target2");

        try {
            PathUtils.recursiveCopyVisitor(source, target);
        } catch (FileAlreadyExistsException e){
            fail("Copy failed.");
        }

        Long[] targetDirectorySizeAfterCopy2 = PathUtils.calculateDirectorySizeAndCount(target);
        long targetSizeBytesAfterCopy2 = targetDirectorySizeAfterCopy2[0];
        long targetSizeCountBeforeCopy2 = targetDirectorySizeAfterCopy2[1];

        assertTrue(sourceSizeBytesBeforeCopy2 == targetSizeBytesAfterCopy2);
        assertTrue(sourceSizeCountBeforeCopy2 == targetSizeCountBeforeCopy2);

        // After test check
        assertTrue(Files.exists(target.resolve("b")));
        assertEquals(Files.getLastModifiedTime(source.resolve("b")), Files.getLastModifiedTime(target.resolve("b")));

        assertTrue(Files.exists(target.resolve("c")));
        assertTrue(Files.exists(target.resolve("sub")) && Files.isDirectory(target.resolve("sub")));
        assertTrue(Files.exists(target.resolve("sub").resolve("d")));

        assertTrue(Files.exists(source.resolve("b")));
        assertTrue(Files.exists(source.resolve("c")));
        assertTrue(Files.exists(source.resolve("sub")) && Files.isDirectory(source.resolve("sub")));
        assertTrue(Files.exists(source.resolve("sub").resolve("d")));
    }

    /**
     * Has to fail if the method is NOT failing.
     *
     * @throws IOException
     */
    public void testRecursiveCopyVisitorFail() throws IOException {
        Path testDir = Paths.get("build/test").toAbsolutePath();

        // Setup base dir with dirs and files.
        Files.createDirectories(testDir);
        Path source = testDir.resolve("source");
        Files.createDirectories(source);
        Path sub = source.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(testDir, "a");
        TestHelper.createRandomFile(source, "b");
        TestHelper.createRandomFile(source, "c");
        TestHelper.createRandomFile(sub, "d");

        Long[] sourceDirectorySizeBeforeCopy = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeCopy = sourceDirectorySizeBeforeCopy[0];
        long sourceSizeCountBeforeCopy = sourceDirectorySizeBeforeCopy[1];

        long targetSizeBytesBeforeCopy = 0;
        long targetSizeCountBeforeCopy = 0;

        // Test 1
        Path target = testDir.resolve("target");
        Files.createDirectories(target);

        try {
            Long[] targetDirectorySizeBeforeCopy = PathUtils.calculateDirectorySizeAndCount(target);
            targetSizeBytesBeforeCopy = targetDirectorySizeBeforeCopy[0];
            targetSizeCountBeforeCopy = targetDirectorySizeBeforeCopy[1];

            PathUtils.recursiveCopyVisitor(source, target);
            fail("File in target already exists but FileAlreadyExists not thrown!");
        } catch (FileAlreadyExistsException e){
            assertTrue("Source file does not exists: " + source, Files.exists(source));
            assertTrue("Target file does not exists: " + source, Files.exists(target));

            Long[] targetDirectorySizeAfterCopy = PathUtils.calculateDirectorySizeAndCount(target);
            long targetSizeBytesAfterCopy = targetDirectorySizeAfterCopy[0];
            long targetSizeCountAfterCopy = targetDirectorySizeAfterCopy[1];

            assertTrue(targetSizeBytesBeforeCopy == targetSizeBytesAfterCopy);
            assertTrue(targetSizeCountBeforeCopy == targetSizeCountAfterCopy);
        }

        // Test 2
        TestHelper.createRandomFile(target, "f");
        TestHelper.createRandomFile(target, "g");

        try {
            Long[] targetDirectorySizeBeforeCopy = PathUtils.calculateDirectorySizeAndCount(target);
            targetSizeBytesBeforeCopy = targetDirectorySizeBeforeCopy[0];
            targetSizeCountBeforeCopy = targetDirectorySizeBeforeCopy[1];

            PathUtils.recursiveCopyVisitor(source, target);
            fail("File in target already exists but FileAlreadyExists not thrown!");
        } catch (FileAlreadyExistsException e){
            // Expected
            assertTrue("Source file does not exists: " + source, Files.exists(source));
            assertTrue("Target file does not exists: " + source, Files.exists(target));

            Long[] targetDirectorySizeAfterCopy = PathUtils.calculateDirectorySizeAndCount(target);
            long targetSizeBytesAfterCopy = targetDirectorySizeAfterCopy[0];
            long targetSizeCountAfterCopy = targetDirectorySizeAfterCopy[1];

            assertTrue(targetSizeBytesBeforeCopy == targetSizeBytesAfterCopy);
            assertTrue(targetSizeCountBeforeCopy == targetSizeCountAfterCopy);
        }

        Long[] sourceDirectorySizeAfterCopy = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesAfterCopy = sourceDirectorySizeAfterCopy[0];
        long sourceSizeCountAfterCopy = sourceDirectorySizeAfterCopy[1];

        assertTrue(sourceSizeBytesBeforeCopy == sourceSizeBytesAfterCopy);
        assertTrue(sourceSizeCountBeforeCopy == sourceSizeCountAfterCopy);

        // After test check
        assertFalse(Files.exists(target.resolve("b")));
        assertFalse(Files.exists(target.resolve("c")));
        assertFalse(Files.exists(target.resolve("sub")) && Files.isDirectory(target.resolve("sub")));
        assertFalse(Files.exists(target.resolve("sub").resolve("d")));

        assertTrue(Files.exists(target.resolve("f")));
        assertTrue(Files.exists(target.resolve("g")));

        assertTrue(Files.exists(source.resolve("b")));
        assertTrue(Files.exists(source.resolve("c")));
        assertTrue(Files.exists(source.resolve("sub")) && Files.isDirectory(source.resolve("sub")));
        assertTrue(Files.exists(source.resolve("sub").resolve("d")));
    }

    /**
     * Copy build/test/a to build/test/a Should not be permitted.
     *
     * @throws IOException
     */
    public void testRecursiveDeleteVisitor() throws IOException {
        Path baseDir = Paths.get("build/test").toAbsolutePath();
        PathUtils.recursiveDeleteVisitor(baseDir);

        // Setup base dir with dirs and files.
        Files.createDirectories(baseDir);
        Path dir = baseDir.resolve("dir");
        Files.createDirectories(dir);
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        TestHelper.createRandomFile(baseDir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        PathUtils.recursiveDeleteVisitor(dir);

        assertFalse(Files.exists(dir.resolve("b")));
        assertFalse(Files.exists(dir.resolve("c")));
        assertFalse(Files.exists(dir.resolve("sub")));
        assertFalse(Files.exists(dir.resolve("sub").resolve("d")));

    }
}
