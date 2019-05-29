package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.TestHelper;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PathUtilsTest extends TestCase {

    private Path baseDir = Paths.get("build/test").toAbsolutePath();

    @Override
    public void tearDown() throws IOException {
        PathUtils.recursiveDeleteVisitor(baseDir);
        Files.createDirectories(baseDir);
    }

    /**
     * PFS-3239
     *
     * @throws IOException
     */
    public void testReplicatedSubdirs() throws IOException {
        PathUtils.isReplicatedSubdir(baseDir.resolve("nonexisting"));
        PathUtils.isReplicatedSubdir(TestHelper.createRandomFile(baseDir)); // Is OK on files
        testReplicatedSubdir(false,"dir");
        testReplicatedSubdir(false, "dir/adirectory/sd/sd/sd/sd");
        testReplicatedSubdir(false,"dir/adirectory/sd/adirectory/sd/sd");
        testReplicatedSubdir(true,"dir/adirectory/x/x/x/x/x/x/x/x/x/x/x");
        testReplicatedSubdir(false, "dir/adirectory/x/x/x/x/x/x/x/x/x//x/x/x/w");
        testReplicatedSubdir(false,"replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354");
        testReplicatedSubdir(true,"replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354");

        createDirs = true;
        testReplicatedSubdir(false,"dir");
        testReplicatedSubdir(false, "dir/adirectory/sd/sd/sd/sd");
        testReplicatedSubdir(false,"dir/adirectory/sd/adirectory/sd/sd");
        testReplicatedSubdir(true,"dir/adirectory/x/x/x/x/x/x/x/x/x/x/x");
        testReplicatedSubdir(false, "dir/adirectory/x/x/x/x/x/x/x/x/x//x/x/x/w");
        testReplicatedSubdir(false,"replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354");
        testReplicatedSubdir(true,"replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/" +
                "replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354/replidf4354");
    }

    private boolean createDirs = false;
    private void testReplicatedSubdir(boolean expectReplicated, String dirName) throws IOException {
        Path dir = baseDir.resolve(dirName);
        if (createDirs) {
            Files.createDirectories(dir);
        }
        assertEquals("expectReplicated=" + expectReplicated + " for " + dir,
                expectReplicated, PathUtils.isReplicatedSubdir(dir));
    }

    public void testURLEncoding() {
        String url = "https://www.my-server.com:8822";
        String filename = "PowerFolder" + PathUtils.encodeURLinFilename(url) + ".exe";
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

        Path testDir = TestHelper.getTestDir();

        // Test null file.
        try {
            PathUtils.isFileInDirectory(null, testDir);
            fail("Null file should not work");
        } catch (IllegalArgumentException e) {
            // expected Exception
        }

        // Test null directory.
        try {
            PathUtils.isFileInDirectory(Paths.get("x"), null);
            fail("Null directory should not work");
        } catch (IllegalArgumentException e) {
            // expected Exception
        }

        // Test directory for file
        assertFalse(PathUtils.isFileInDirectory(testDir, testDir));

        // Test file for directory
        assertFalse(PathUtils.isFileInDirectory(Paths.get("X"), Paths.get("Y")));

        boolean okay = false;

        // Test file not in directory
        try {
            Path dir = testDir.resolve("P");
            Files.createDirectories(dir);
            assertTrue(Files.isDirectory(dir));
            okay = PathUtils.isFileInDirectory(testDir.resolve("X"), dir);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        assertFalse("Process a file not in directory", okay);

        // Test file in directory
        try {
            Path dir = testDir.resolve("Q");
            Files.createDirectories(dir);
            assertTrue(Files.isDirectory(dir));
            okay = PathUtils.isFileInDirectory(dir.resolve("X"), testDir);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
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
        assertTrue(PathUtils.isSameName("foo (BRACKETS) (owner)", "foo (BRACKETS)"));
        // Never worked: Should it?
        // assertTrue(PathUtils.isSameName("foo (owner XXX)", "foo (owner YYY)"));
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
     * build/test/dir/sub/d ... to ... build/test/move/
     *
     * @throws IOException
     */
    public void testFileMove() throws IOException {
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
        TestHelper.createRandomFile(dir, "a");
        TestHelper.createRandomFile(dir, "b");
        TestHelper.createRandomFile(dir, "c");
        TestHelper.createRandomFile(sub, "d");

        Long[] sourceDirectorySizeBeforeMove = PathUtils.calculateDirectorySizeAndCount(dir);
        long sourceSizeBytesBeforeMove = sourceDirectorySizeBeforeMove[0];
        long sourceSizeCountBeforeMove = sourceDirectorySizeBeforeMove[1];

        // Move it.
        Path moveDir = Paths.get("build/test/move").toAbsolutePath();
        Files.move(dir, moveDir);

        // Check move.
        assertTrue(Files.exists(moveDir));

        // After move check
        Long[] sourceDirectorySizeAfterMove = PathUtils.calculateDirectorySizeAndCount(moveDir);
        long sourceSizeBytesAfterMove = sourceDirectorySizeAfterMove[0];
        long sourceSizeCountAfterMove = sourceDirectorySizeAfterMove[1];

        assertEquals(sourceSizeBytesBeforeMove, sourceSizeBytesAfterMove);
        assertEquals(sourceSizeCountBeforeMove, sourceSizeCountAfterMove);

        assertTrue(Files.exists(moveDir.resolve("a")));
        assertTrue(Files.exists(moveDir.resolve("b")));
        assertTrue(Files.exists(moveDir.resolve("c")));
        assertTrue(Files.exists(moveDir.resolve("sub")));
        assertTrue(Files.exists(moveDir.resolve("sub").resolve("d")));

        assertTrue(Files.notExists(dir.resolve("a")));
        assertTrue(Files.notExists(dir.resolve("b")));
        assertTrue(Files.notExists(dir.resolve("c")));
        assertTrue(Files.notExists(dir.resolve("sub")));
        assertTrue(Files.notExists(dir.resolve("sub").resolve("d")));

        // Check the original is gone.
        assertTrue(Files.notExists(dir));
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
        assertEquals(2, PathUtils.getNumberOfSiblings(copyDir)); // a and dir
        boolean foundDir = false;
        boolean foundSub = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(copyDir)) {
            for (Path dirFile : stream) {
                if (Files.isDirectory(dirFile)) {
                    foundDir = true;
                    assertEquals(3, PathUtils.getNumberOfSiblings(dirFile)); // b,
                    // c
                    // and
                    // sub

                    try (DirectoryStream<Path> subStream = Files
                            .newDirectoryStream(dirFile)) {
                        for (Path subFile : subStream) {
                            if (Files.isDirectory(subFile)) {
                                foundSub = true;
                                assertEquals(1, PathUtils
                                        .getNumberOfSiblings(subFile)); // d
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
                    assertEquals(3, PathUtils.getNumberOfSiblings(dirFile)); // b,
                    // c,
                    // and
                    // sub

                    try (DirectoryStream<Path> subStream = Files
                            .newDirectoryStream(dirFile)) {
                        for (Path subFile : subStream) {
                            if (Files.isDirectory(subFile)) {
                                foundSub = true;
                                assertEquals(1, PathUtils
                                        .getNumberOfSiblings(subFile)); // d
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
        assertEquals("start: sourceSum=" + souceSum + ". targetSum=" + targetSum, souceSum, targetSum);
        // Should be OK!

        int nFiles = 250;
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
        assertEquals("Stage 2: sourceSum=" + souceSum + ". targetSum=" + targetSum, souceSum, targetSum);

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
        assertEquals("Stage 3: sourceSum=" + souceSum + ". targetSum=" + targetSum,souceSum, targetSum);

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
        PathUtils.recursiveDelete(base);
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
        PathUtils.recursiveDelete(base);
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

        try {
            assertFalse(PathUtils.isNetworkPath(Paths.get("C:\\")));
            assertFalse(PathUtils.isNetworkPath(Paths.get("C:\\subdir\\subdir2")));

            assertFalse(PathUtils.isNetworkPath(Paths
                    .get("/home/user/PowerFolders/123")));

            // assertTrue(PathUtils.isNetworkPath(Paths.get("N:\\")));
            // assertTrue(PathUtils.isNetworkPath(Paths.get("N:\\subdir\\subdir2")));

            assertTrue(PathUtils.isNetworkPath(Paths.get("\\\\server\\share")));
            assertTrue(PathUtils.isNetworkPath(Paths
                    .get("\\\\server\\share\\subdir")));

        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve symlink!");
        }
    }

    public void testRecursiveMoveVisitor() throws IOException {

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

        FileTime timeFromFileBBeforeMove = Files.getLastModifiedTime(source.resolve("b"));

        assertTrue(Files.exists(TestHelper.createRandomFile(sub, "d")));

        TestHelper.waitMilliSeconds(200);

        Long[] sourceDirectorySizeBeforeMove = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeMove = sourceDirectorySizeBeforeMove[0];
        long sourceSizeCountBeforeMove = sourceDirectorySizeBeforeMove[1];

        // Test 1
        Path target = testDir.resolve("target");

        try {
            PathUtils.recursiveMoveVisitor(source, target);
        } catch (FileAlreadyExistsException e) {
            fail();
        }

        Long[] targetDirectorySizeAfterMove = PathUtils.calculateDirectorySizeAndCount(target);
        long targetSizeBytesAfterMove = targetDirectorySizeAfterMove[0];
        long targetSizeCountBeforeMove = targetDirectorySizeAfterMove[1];

        assertEquals(sourceSizeBytesBeforeMove, targetSizeBytesAfterMove);
        assertEquals(sourceSizeCountBeforeMove, targetSizeCountBeforeMove);
        assertTrue(Files.notExists(source));

        // Test 2
        TestHelper.createRandomFile(source, "f");
        TestHelper.createRandomFile(source, "b");

        Long[] sourceDirectorySizeBeforeMove2 = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeMove2 = sourceDirectorySizeBeforeMove2[0];
        long sourceSizeCountBeforeMove2 = sourceDirectorySizeBeforeMove2[1];

        Path target2 = testDir.resolve("target2");

        try {
            PathUtils.recursiveMoveVisitor(source, target2);
        } catch (FileAlreadyExistsException e) {
            fail("Copy failed.");
        }

        Long[] targetDirectorySizeAfterMove2 = PathUtils.calculateDirectorySizeAndCount(target2);
        long targetSizeBytesAfterMove2 = targetDirectorySizeAfterMove2[0];
        long targetSizeCountBeforeMove2 = targetDirectorySizeAfterMove2[1];

        assertEquals(sourceSizeBytesBeforeMove2, targetSizeBytesAfterMove2);
        assertEquals(sourceSizeCountBeforeMove2, targetSizeCountBeforeMove2);
        assertTrue(Files.notExists(source));

        // After test check
        assertTrue(Files.exists(target.resolve("b")));
        assertEquals(timeFromFileBBeforeMove, Files.getLastModifiedTime(target.resolve("b")));

        assertTrue(Files.exists(target.resolve("c")));
        assertTrue(Files.exists(target.resolve("sub")) && Files.isDirectory(target.resolve("sub")));
        assertTrue(Files.exists(target.resolve("sub").resolve("d")));

        assertTrue(Files.exists(target2.resolve("b")));
        assertTrue(Files.exists(target2.resolve("f")));

        assertTrue(Files.notExists(source.resolve("b")));
        assertTrue(Files.notExists(source.resolve("c")));
        assertTrue(Files.notExists(source.resolve("sub")));
        assertTrue(Files.notExists(source.resolve("sub").resolve("d")));
    }

    public void testRecursiveMoveVisitorFail() throws IOException {
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

        Long[] sourceDirectorySizeBeforeMove = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesBeforeMove = sourceDirectorySizeBeforeMove[0];
        long sourceSizeCountBeforeMove = sourceDirectorySizeBeforeMove[1];

        long targetSizeBytesBeforeMove = 0;
        long targetSizeCountBeforeMove = 0;

        // Test 1
        Path target = testDir.resolve("target");
        Files.createDirectories(target);

        try {
            Long[] targetDirectorySizeBeforeMove = PathUtils.calculateDirectorySizeAndCount(target);
            targetSizeBytesBeforeMove = targetDirectorySizeBeforeMove[0];
            targetSizeCountBeforeMove = targetDirectorySizeBeforeMove[1];

            PathUtils.recursiveMoveVisitor(source, target);
            fail("Target already exists but FileAlreadyExists not thrown!");
        } catch (FileAlreadyExistsException e) {
            assertTrue("Source file does not exists: " + source, Files.exists(source));
            assertTrue("Target file does not exists: " + source, Files.exists(target));

            Long[] targetDirectorySizeAfterMove = PathUtils.calculateDirectorySizeAndCount(target);
            long targetSizeBytesAfterMove = targetDirectorySizeAfterMove[0];
            long targetSizeCountAfterMove = targetDirectorySizeAfterMove[1];

            assertEquals(targetSizeBytesBeforeMove, targetSizeBytesAfterMove);
            assertEquals(targetSizeCountBeforeMove, targetSizeCountAfterMove);
        }

        Long[] sourceDirectorySizeAfterMove = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesAfterMove = sourceDirectorySizeAfterMove[0];
        long sourceSizeCountAfterMove = sourceDirectorySizeAfterMove[1];

        assertEquals(sourceSizeBytesBeforeMove, sourceSizeBytesAfterMove);
        assertEquals(sourceSizeCountBeforeMove, sourceSizeCountAfterMove);

        // Test 2
        TestHelper.createRandomFile(target, "f");
        TestHelper.createRandomFile(target, "g");

        try {
            Long[] targetDirectorySizeBeforeMove = PathUtils.calculateDirectorySizeAndCount(target);
            targetSizeBytesBeforeMove = targetDirectorySizeBeforeMove[0];
            targetSizeCountBeforeMove = targetDirectorySizeBeforeMove[1];

            PathUtils.recursiveMoveVisitor(source, target);
            fail("File in target already exists but FileAlreadyExists not thrown!");
        } catch (FileAlreadyExistsException e) {
            // Expected
            assertTrue("Source file does not exists: " + source, Files.exists(source));
            assertTrue("Target file does not exists: " + source, Files.exists(target));

            Long[] targetDirectorySizeAfterMove = PathUtils.calculateDirectorySizeAndCount(target);
            long targetSizeBytesAfterMove = targetDirectorySizeAfterMove[0];
            long targetSizeCountAfterMove = targetDirectorySizeAfterMove[1];

            assertEquals(targetSizeBytesBeforeMove, targetSizeBytesAfterMove);
            assertEquals(targetSizeCountBeforeMove, targetSizeCountAfterMove);
        }

        Long[] sourceDirectorySizeAfterMove2 = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesAfterMove2 = sourceDirectorySizeAfterMove2[0];
        long sourceSizeCountAfterMove2 = sourceDirectorySizeAfterMove2[1];

        assertEquals(sourceSizeBytesAfterMove2, sourceSizeBytesAfterMove);
        assertEquals(sourceSizeCountAfterMove2, sourceSizeCountAfterMove);

        // After test check
        assertTrue(Files.notExists(target.resolve("b")));
        assertTrue(Files.notExists(target.resolve("c")));
        assertTrue(Files.notExists(target.resolve("sub")));
        assertTrue(Files.notExists(target.resolve("sub").resolve("d")));

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

        assertEquals(sourceSizeBytesBeforeCopy, targetSizeBytesAfterCopy);
        assertEquals(sourceSizeCountBeforeCopy, targetSizeCountBeforeCopy);

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

        assertEquals(sourceSizeBytesBeforeCopy2, targetSizeBytesAfterCopy2);
        assertEquals(sourceSizeCountBeforeCopy2, targetSizeCountBeforeCopy2);

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

            assertEquals(targetSizeBytesBeforeCopy, targetSizeBytesAfterCopy);
            assertEquals(targetSizeCountBeforeCopy, targetSizeCountAfterCopy);
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

            assertEquals(targetSizeBytesBeforeCopy, targetSizeBytesAfterCopy);
            assertEquals(targetSizeCountBeforeCopy, targetSizeCountAfterCopy);
        }

        Long[] sourceDirectorySizeAfterCopy = PathUtils.calculateDirectorySizeAndCount(source);
        long sourceSizeBytesAfterCopy = sourceDirectorySizeAfterCopy[0];
        long sourceSizeCountAfterCopy = sourceDirectorySizeAfterCopy[1];

        assertEquals(sourceSizeBytesBeforeCopy, sourceSizeBytesAfterCopy);
        assertEquals(sourceSizeCountBeforeCopy, sourceSizeCountAfterCopy);

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

    public void testIsDesktopIniNull() {
        Path file = null;
        try {
            PathUtils.isDesktopIni(file);
            fail("Did not throw NullPointerException but the path was null");
        } catch (NullPointerException e){
            //OK since path was null and it is supposed to throw this exception
        }
    }

    public void testIsDesktopIniOk() {
        Path path = new File(PathUtils.DESKTOP_INI_FILENAME).toPath();
        assertTrue(PathUtils.isDesktopIni(path));

        Path notDesktopIni = new File("TestFile.txt").toPath();
        assertFalse(PathUtils.isDesktopIni(notDesktopIni));

        Path stillNotDesktopIni = new File("File"+PathUtils.DESKTOP_INI_FILENAME).toPath();
        assertFalse(PathUtils.isDesktopIni(stillNotDesktopIni));
    }

    public void testIsValidZipFileNull() {
        Path file = null;
        try {
            PathUtils.isValidZipFile(file);
            fail("Did not throw NullPointerException but the path was null");
        } catch (NullPointerException e){
            //OK since path was null and it is supposed to throw this exception
        }
    }

    public void testIsValidZipFileOk() throws IOException {
        Path zipFileThatDoesNotExist = new File("myArchive.zip").toPath();
        //Throws IOExceptio
        assertFalse(PathUtils.isValidZipFile(zipFileThatDoesNotExist));

        File notZipFile = new File("build/myFile.txt");
        notZipFile.createNewFile();
        Path notZipFilePath = notZipFile.toPath();
        assertFalse(PathUtils.isValidZipFile(notZipFilePath));

        File zipFile = new File("build/emptyArchive.zip");
        zipFile.createNewFile();
        Path zipFilePath = zipFile.toPath();
        //Because zip file is empty
        assertFalse(PathUtils.isValidZipFile(zipFilePath));

        StringBuilder sb = new StringBuilder();
        sb.append("Test String");

        File fullZipFile = new File("build/fullArchive.zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(fullZipFile));
        ZipEntry e = new ZipEntry("mytext.txt");
        out.putNextEntry(e);

        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();

        out.close();

        Path fullZipFilePath = fullZipFile.toPath();
        assertTrue(PathUtils.isValidZipFile(fullZipFilePath));

        FileUtils.forceDelete(notZipFile);
        FileUtils.forceDelete(zipFile);
        FileUtils.forceDelete(fullZipFile);
    }

    public void testIsSameNameNull(){

        Path nullPath = null;
        File file = new File("test.txt");
        Path notNullPath = file.toPath();

        try {
            PathUtils.isSameName(nullPath, notNullPath);
            fail("Did not throw NullPointerException but first path was null");
        } catch (NullPointerException e){
            //OK since first argument was null
        }


        try {
            PathUtils.isSameName(notNullPath, nullPath);
            fail("Did not throw NullPointerException but first path was null");
        } catch (NullPointerException e){
            //OK since first argument was null
        }
    }

    public void testIsSameNameFileNull() {
        File file = new File("/");
        Path nullFileName = file.toPath();

        File another = new File("myFile.txt");
        Path notNullFileName = another.toPath();

        assertFalse(PathUtils.isSameName(nullFileName, notNullFileName));
        assertFalse(PathUtils.isSameName(notNullFileName, nullFileName));
    }

    public void testIsSameNameOk() {

        File firstFile = new File("myFile.txt");
        File secondFile = new File("myFile.pdf");

        Path firstPath = firstFile.toPath();
        Path secondPath = secondFile.toPath();
        assertFalse(PathUtils.isSameName(firstPath, secondPath));

        File oneFile = new File("testFile.csv");
        File sameFile = new File("testFile.csv");
        assertTrue(PathUtils.isSameName(oneFile.toPath(), sameFile.toPath()));

        File file = new File("/build/test/file.docx");
        File anotherFile = new File("/build/file.docx");
        assertTrue(PathUtils.isSameName(file.toPath(), anotherFile.toPath()));
    }

    public void testRecursiveMoveCopyFallbackVisitorAlreadyExist() throws IOException {

        File firstDirectory = new File("build/directoryOne");
        firstDirectory.mkdir();

        File secondDirectory = new File("build/directoryTwo");
        secondDirectory.mkdir();

        try {
            PathUtils.recursiveMoveCopyFallbackVisitor(firstDirectory.toPath(), secondDirectory.toPath());
            fail("Did not throw FileAlreadyExistsException when target directory already existed");
        } catch (FileAlreadyExistsException e) {
            //OK Since target directory already exists
        }

        FileUtils.deleteDirectory(firstDirectory);
        FileUtils.deleteDirectory(secondDirectory);
    }

    public void testRecursiveMoveCopyFallbackVisitorCreatesDirectories() throws IOException {

        File sourceDirectory = new File("build/directoryOne");
        sourceDirectory.mkdir();

        File targetDirectory = new File("build/directoryTwo");

        PathUtils.recursiveMoveCopyFallbackVisitor(sourceDirectory.toPath(), targetDirectory.toPath());

        assertTrue(targetDirectory.exists());
        assertFalse(sourceDirectory.exists());

        FileUtils.deleteDirectory(targetDirectory);

    }

    public void testRecursiveMoveCopyFallbackVisitorCopiesFiles() throws IOException {

        File sourceDirectory = new File("build/directoryOne");
        sourceDirectory.mkdir();

        File targetDirectory = new File("build/directoryTwo");
        File firstFile = new File("build/directoryOne/myFile.txt");
        firstFile.createNewFile();
        File secondFile = new File("build/directoryOne/anotherFile.pdf");
        secondFile.createNewFile();
        File thirdFile = new File("build/directoryOne/yetAnotherFile.docx");
        thirdFile.createNewFile();

        PathUtils.recursiveMoveCopyFallbackVisitor(sourceDirectory.toPath(), targetDirectory.toPath());

        assertTrue(new File("build/directoryTwo/myFile.txt").exists());
        assertTrue(new File("build/directoryTwo/anotherFile.pdf").exists());
        assertTrue(new File("build/directoryTwo/yetAnotherFile.docx").exists());

        assertFalse(firstFile.exists());
        assertFalse(secondFile.exists());
        assertFalse(thirdFile.exists());

        FileUtils.deleteDirectory(targetDirectory);
    }

    public void testRecursiveMoveCopyFallbackVisitorCopiesDirectory() throws IOException {

        //Create source directory and a directory within the source directory
        File sourceDirectory = new File("build/directoryOne");
        sourceDirectory.mkdir();
        File subsourceDirectory = new File("build/directoryOne/subdirectory");
        subsourceDirectory.mkdir();

        File targetDirectory = new File("build/directoryTwo");

        //Populate the source directory and the subsource directory with files
        File firstFile = new File("build/directoryOne/myFile.txt");
        firstFile.createNewFile();
        File secondFile = new File("build/directoryOne/anotherFile.pdf");
        secondFile.createNewFile();
        File thirdFile = new File("build/directoryOne/yetAnotherFile.docx");
        thirdFile.createNewFile();
        File fourthFile = new File("build/directoryOne/subdirectory/subfile.csv");
        fourthFile.createNewFile();

        PathUtils.recursiveMoveCopyFallbackVisitor(sourceDirectory.toPath(), targetDirectory.toPath());

        assertTrue(new File("build/directoryTwo/myFile.txt").exists());
        assertTrue(new File("build/directoryTwo/anotherFile.pdf").exists());
        assertTrue(new File("build/directoryTwo/yetAnotherFile.docx").exists());
        assertTrue(new File("build/directoryTwo/subdirectory").isDirectory());
        assertTrue(new File("build/directoryTwo/subdirectory/subfile.csv").exists());

        assertFalse(firstFile.exists());
        assertFalse(secondFile.exists());
        assertFalse(thirdFile.exists());
        assertFalse(sourceDirectory.exists());
        assertFalse(fourthFile.exists());

        FileUtils.deleteDirectory(targetDirectory);
    }

    public void testIsEmptyDirNullPath() {
        boolean result = PathUtils.isEmptyDir(null, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return false;
            }
        });

        assertFalse(result);
    }

    public void testIsEmptyDirIoException() {
        File directory = new File("build/test/myDirectory");
        boolean result = PathUtils.isEmptyDir(directory.toPath(), new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return false;
            }
        });
        assertFalse(result);
    }

    public void testIsEmptyDirOk() throws IOException {
        File directory = new File("build/test/myDirectory");
        directory.mkdir();

        File fileToAdd = new File("build/test/myDirectory/myFile.csv");
        fileToAdd.createNewFile();

        boolean result = PathUtils.isEmptyDir(directory.toPath(), new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return true;
            }
        });

        assertFalse(result);

        FileUtils.forceDelete(fileToAdd);

        boolean resultAfterDelete = PathUtils.isEmptyDir(directory.toPath(), new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return true;
            }
        });

        assertTrue(resultAfterDelete);
        FileUtils.deleteDirectory(directory);
    }

    public void testIsEmptyDirOneArgument() throws IOException {
        File directory = new File("build/test/myDirectory");
        directory.mkdir();

        File fileToAdd = new File("build/test/myDirectory/myFile.csv");
        fileToAdd.createNewFile();

        boolean result = PathUtils.isEmptyDir(directory.toPath());
        assertFalse(result);

        FileUtils.forceDelete(fileToAdd);
        boolean resultAfterDelete = PathUtils.isEmptyDir(directory.toPath());
        assertTrue(resultAfterDelete);

        FileUtils.deleteDirectory(directory);
    }

    public void testGetSuggestedFolderNameNullPath() {
        Path file = null;
        assertNull(PathUtils.getSuggestedFolderName(file));
    }

    public void testGetSuggestedFolderNameEmpty() {
        File file = new File("");
        Path path = file.toPath();
        assertEquals(path.toAbsolutePath().toString(), PathUtils.getSuggestedFolderName(path));
    }

    public void testGetSuggestedFolderNameOk() {
        File file = new File("build/test/myFile.csv");
        Path path = file.toPath();
        assertEquals("myFile.csv", PathUtils.getSuggestedFolderName(path));

        File pdfFile = new File("build/test/pdfFile.pdf");
        Path pdfPath = pdfFile.toPath();
        assertEquals("pdfFile.pdf", PathUtils.getSuggestedFolderName(pdfPath));

        File docFile = new File("build/test/myFile.docx");
        Path docPath = docFile.toPath();
        assertEquals("myFile.docx", PathUtils.getSuggestedFolderName(docPath));

    }

    public void testCopyFileExceptionsNull() throws IOException {
        Path fromPath = null;
        Path toPath = new File("build/test/someFile.txt").toPath();
        try {
            PathUtils.copyFile(fromPath, toPath);
            fail("Did not throw NullPointerException but from file was null");
        } catch (NullPointerException e) {
            //OK to throw since from file was null
        }
    }

    public void testCopyFileIoExceptions() throws IOException {
        Path fromPath = new File("build/test/thisDoesNotExist.txt").toPath();
        Path toPath = new File("build/test/anotherFile.txt").toPath();

        try {
            PathUtils.copyFile(fromPath, toPath);
            fail("Did not throw IOException but from path did not exist");
        } catch (IOException e) {
            //OK since from path does not exist
        }

        File thisExists = new File("build/test/exists.txt");
        thisExists.createNewFile();

        try {
            PathUtils.copyFile(thisExists.toPath(), thisExists.toPath());
            fail("Did not throw IOException but from and to were the same file");
        } catch (IOException e) {
            //OK since from and two were identical
        }

        FileUtils.forceDelete(thisExists);
    }

    public void testInvalidFileNameDots() {
        assertTrue(PathUtils.containsInvalidChar("."));
        assertTrue(PathUtils.containsInvalidChar(".."));
        assertTrue(PathUtils.containsInvalidChar("my<file"));
        assertTrue(PathUtils.containsInvalidChar("another>File"));
        assertTrue(PathUtils.containsInvalidChar("file\\file"));
        assertTrue(PathUtils.containsInvalidChar("file/toFile"));
        assertTrue(PathUtils.containsInvalidChar("file?"));
        assertTrue(PathUtils.containsInvalidChar("file*test"));
        assertTrue(PathUtils.containsInvalidChar("file|filetest"));
        assertTrue(PathUtils.containsInvalidChar("path\"test"));

        assertFalse(PathUtils.containsInvalidChar("myFile.txt"));
        assertFalse(PathUtils.containsInvalidChar("myFile123.csv"));
        assertFalse(PathUtils.containsInvalidChar("Homer.pdf.txt"));
        assertFalse(PathUtils.containsInvalidChar("Lisa_Bart"));
        assertFalse(PathUtils.containsInvalidChar("test-simpsons_Testing.csv"));
    }

    public void testRemoveInvalidCharsNullFilename() {
        File file = new File("/");
        Path path = file.toPath();
        Path returnedPath = PathUtils.removeInvalidFilenameChars(path);
        assertEquals(path, returnedPath);
    }

    public void testRemoveInvalidCharsOk() {
        File file = new File("build/test/someFile.csv");
        Path path = file.toPath();
        Path returnedPath = PathUtils.removeInvalidFilenameChars(path);
        assertEquals(path, returnedPath);

        file = new File("build/test/stars***Stars.csv   ");
        path = file.toPath();
        returnedPath = PathUtils.removeInvalidFilenameChars(path);
        assertEquals("starsStars.csv",returnedPath.getFileName().toString());


        file = new File("myFile?Yes");
        path = file.toPath();
        returnedPath = PathUtils.removeInvalidFilenameChars(path);
        assertEquals("myFileYes",returnedPath.getFileName().toString());
    }

    public void testRawCopyNullInputStreamTest() throws IOException {
        File file = new File("build/test/file.txt");
        file.createNewFile();

        OutputStream outputStream = new FileOutputStream(file);
        InputStream inputStream = null;

        try {
            PathUtils.rawCopy(inputStream, outputStream);
            fail("Did not throw NullPointerException but input stream was null");
        } catch (NullPointerException e){
            //OK since InputStream was null
        }

        outputStream.close();
        FileUtils.forceDelete(file);
    }

    public void testRawCopyNullOutputStreamTest() throws IOException {
        File file = new File("build/test/file.txt");
        file.createNewFile();

        FileInputStream fileInputStream = new FileInputStream(file);
        OutputStream outputStream = null;
        try {
            PathUtils.rawCopy(fileInputStream, outputStream);
            fail("Did not throw NullPointerException but output stream was null");
        } catch (NullPointerException e){
            //OK since OutputStream was null
        }
        fileInputStream.close();
    }

    public void testRawCopyOk() throws IOException {
        File file = new File("build/test/file.txt");
        file.createNewFile();

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Mary had a little lamb, little lamb...");
        fileWriter.close();

        FileInputStream fileInputStream = new FileInputStream(file);

        File copiedFile = new File("build/copiedFile.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(copiedFile);

        PathUtils.rawCopy(fileInputStream, fileOutputStream);
        assertTrue(copiedFile.exists());
        assertEquals(file.length(), copiedFile.length());

        FileUtils.forceDelete(copiedFile);
        fileInputStream.close();
    }

    public void testRawCopyLarge() throws IOException {
        File file = new File("build/test/file.txt");
        file.createNewFile();

        FileWriter fileWriter = new FileWriter(file);
        for (int index = 0; index < 100000000; index++) {
            fileWriter.write(index);
        }
        fileWriter.close();

        FileInputStream fileInputStream = new FileInputStream(file);

        File copiedFile = new File("build/copiedFile.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(copiedFile);

        PathUtils.rawCopy(fileInputStream, fileOutputStream);
        assertTrue(copiedFile.exists());
        assertEquals(file.length(), copiedFile.length());

        FileUtils.forceDelete(copiedFile);
        fileInputStream.close();
    }

    public void testNCopyRandomAccessFileEOF() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        fileWriter.write("The quick brown fox jumps over the lazy dog");
        fileWriter.close();

        RandomAccessFile input = new RandomAccessFile(inputFile, "rw");

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");

        try {
            PathUtils.ncopy(input, output, 128);
            fail("EOF not thrown");
        } catch (EOFException e){
            //OK since it was supposed to throw EOF
        }

        input.close();
        output.close();
    }

    public void testNCopyRandomAccessFilePartial() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        for (int index = 0; index < 100000; index++) {
            fileWriter.write(index);
        }
        fileWriter.close();


        RandomAccessFile input = new RandomAccessFile(inputFile, "rw");

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");

        PathUtils.ncopy(input, output, 1);
        //8192 is the byte chunk size of the PathUtils
        assertEquals(8192, outputFile.length());
        input.close();
        output.close();

    }

    public void testNCopyStreamRafEOF() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        fileWriter.write("The quick brown fox jumps over the lazy dog");
        fileWriter.close();

        InputStream input = new FileInputStream(inputFile);

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");

        try {
            PathUtils.ncopy(input, output, 128);
            fail("EOF not thrown");
        } catch (EOFException e){
            //OK since it was supposed to throw EOF
        }

        input.close();
        output.close();
    }

    public void testNCopyStreamRafOk() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        for (int index = 0; index < 100000; index++) {
            fileWriter.write(index);
        }
        fileWriter.close();


        InputStream input = new FileInputStream(inputFile);

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");

        PathUtils.ncopy(input, output, 8192 * 9);
        //8192 is the byte chunk size of the PathUtils
        assertEquals(8192 * 9, outputFile.length());

        input.close();
        output.close();
    }

    public void testNCopyFileChannelFileChannelEOF() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        fileWriter.write("The quick brown fox jumps over the lazy dog");
        fileWriter.close();

        FileChannel input = new RandomAccessFile(inputFile, "rw").getChannel();

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        FileChannel output = new RandomAccessFile(outputFile, "rw").getChannel();

        try {
            PathUtils.ncopy(input, output, 128);
            fail("EOF not thrown");
        } catch (EOFException e){
            //OK since it was supposed to throw EOF
        }

        input.close();
        output.close();
    }

    public void testNCopyStreamFileChannelFileChannelOk() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        for (int index = 0; index < 100000; index++) {
            fileWriter.write(index);
        }
        fileWriter.close();

        FileChannel input = new RandomAccessFile(inputFile,"rw").getChannel();
        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        FileChannel output = new RandomAccessFile(outputFile, "rw").getChannel();

        PathUtils.ncopy(input, output, 8192 * 5);
        //8192 is the byte chunk size of the PathUtils
        assertEquals(8192 * 5, outputFile.length());

        input.close();
        output.close();

    }

    public void testNCopyInputStreamFileChannelEOF() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        fileWriter.write("The quick brown fox jumps over the lazy dog");
        fileWriter.close();

        InputStream input = new FileInputStream(inputFile);

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        FileChannel output = new RandomAccessFile(outputFile, "rw").getChannel();

        try {
            PathUtils.ncopy(input, output, 128);
            fail("EOF not thrown");
        } catch (EOFException e){
            //OK since it was supposed to throw EOF
        }

        input.close();
        output.close();
    }

    public void testNCopyStreamFileChannelOk() throws IOException {
        File inputFile = new File("build/test/firstFile.txt");
        inputFile.createNewFile();

        FileWriter fileWriter = new FileWriter(inputFile);
        for (int index = 0; index < 100000; index++) {
            fileWriter.write(index);
        }
        fileWriter.close();

        InputStream input = new FileInputStream(inputFile);

        File outputFile = new File("build/test/secondFile.txt");
        outputFile.createNewFile();
        FileChannel output = new RandomAccessFile(outputFile, "rw").getChannel();

        PathUtils.ncopy(input, output, 8192 * 10);
        //8192 is the byte chunk size of the PathUtils
        assertEquals(8192 * 10, outputFile.length());

        input.close();
        output.close();
    }

    public void testSetAttributeOnWindowsNull() {
        File file = new File("build/test/myFile.txt");
        assertTrue(PathUtils.setAttributesOnWindows(file.toPath(), null, null));
    }

    public void testSetAttributeOnWindowsHidden() throws IOException {
        File file = new File("build/test/myFile.txt");
        file.createNewFile();
        PathUtils.setAttributesOnWindows(file.toPath(), true, null);
        assertTrue(file.isHidden());
        PathUtils.setAttributesOnWindows(file.toPath(), false, null);
        assertFalse(file.isHidden());
    }

    public void testSetAttributeOnWindowsSystem() throws IOException {
        File file = new File("build/test/myFile.txt");
        file.createNewFile();
        PathUtils.setAttributesOnWindows(file.toPath(), null, true);
        assertEquals(true, Files.getAttribute(file.toPath(), "dos:system") );
        PathUtils.setAttributesOnWindows(file.toPath(), null, false);
        assertEquals(false, Files.getAttribute(file.toPath(), "dos:system") );
    }

    public void testSetAttributeOnWindowsIoException() throws IOException {
        File file = new File("build/test/myFile.txt");
        assertTrue(PathUtils.setAttributesOnWindows(file.toPath(), null, true));
        assertTrue(PathUtils.setAttributesOnWindows(file.toPath(), true, null));
    }

    public void testRecursiveMoveDirectories() throws IOException {
        File sourceDirectory = new File("build/test/directoryOne");
        sourceDirectory.mkdir();

        File targetDirectory = new File("build/test/directoryTwo");
        sourceDirectory.mkdir();

        TestHelper.createRandomFile(sourceDirectory.toPath(),"TestOne");
        TestHelper.createRandomFile(sourceDirectory.toPath(), "TestTwo");
        TestHelper.createRandomFile(sourceDirectory.toPath(), "TestThree");

        PathUtils.recursiveMove(sourceDirectory.toPath(), targetDirectory.toPath());

        File firstFile = new File("build/test/directoryTwo/TestOne");
        File secondFile = new File("build/test/directoryTwo/TestTwo");
        File thirdFile = new File("build/test/directoryTwo/TestThree");

        assertTrue(firstFile.exists());
        assertTrue(secondFile.exists());
        assertTrue(thirdFile.exists());
    }

    public void testRecursiveMoveFiles() throws IOException {
        File sourceFile = new File("build/test/fileOne.txt");
        sourceFile.createNewFile();
        FileWriter fileWriter = new FileWriter(sourceFile);
        fileWriter.write("The force is strong in you!!!");
        fileWriter.close();
        long fileSize = sourceFile.length();
        File targetFile = new File("build/test/fileTwo.txt");

        PathUtils.recursiveMove(sourceFile.toPath(), targetFile.toPath());
        assertTrue(targetFile.exists());
        assertEquals(fileSize, targetFile.length());
    }

    public void testRecursiveMoveFilesUnsupported() throws IOException {
        File sourceDirectory = new File("build/test/directoryOne");
        sourceDirectory.mkdir();

        TestHelper.createRandomFile(sourceDirectory.toPath(),"FirstFile");

        File targetFile = new File("build/test/fileTwo.txt");
        targetFile.createNewFile();

        try {
            PathUtils.recursiveMove(sourceDirectory.toPath(), targetFile.toPath());
            fail("Did not throw UnsupportedOperationException when moving a directory to a file");
        } catch (UnsupportedOperationException e){
            //OK since we're trying to move a directory to a file
        }
    }

    public void testDeleteDesktopIni() throws IOException {
        File directory = new File("build/test/directoryOne");
        directory.mkdir();
        File desktopIniFile = new File("build/test/directoryOne/" + PathUtils.DESKTOP_INI_FILENAME);
        desktopIniFile.createNewFile();
        PathUtils.deleteDesktopIni(directory.toPath());
        assertFalse(desktopIniFile.exists());
        assertEquals(false, Files.getAttribute(directory.toPath(),"dos:system"));
    }

    public void testMaintainDesktopIniRecent() throws IOException {
        Controller controller = new Controller();
        File configFile = new File("build/test/basic.config");
        configFile.createNewFile();
        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/basic.config");

        File directory = new File("build/test/myDirectory");
        directory.mkdir();
        File desktopIniFile = new File("build/test/myDirectory/" + PathUtils.DESKTOP_INI_FILENAME);
        desktopIniFile.createNewFile();
        long lastModified = desktopIniFile.lastModified();

        PathUtils.maintainDesktopIni(controller, directory.toPath());

        assertEquals(lastModified, desktopIniFile.lastModified());
    }

    public void testMaintainDesktopIniLastModifiedPfIconFalse() throws IOException {
        Controller controller = new Controller();
        File configFile = new File("build/test/basic.config");
        configFile.createNewFile();
        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("disableui=true \n use.pf.icon=false");
        fileWriter.close();
        controller.startConfig("build/test/basic.config");
        File directory = new File("build/test/myDirectory");
        directory.mkdir();
        File desktopIniFile = new File("build/test/myDirectory/" + PathUtils.DESKTOP_INI_FILENAME);
        desktopIniFile.createNewFile();
        desktopIniFile.setLastModified(0);

        PathUtils.maintainDesktopIni(controller, directory.toPath());

        assertFalse(desktopIniFile.exists());
    }

    public void testMaintainDesktopIniLastModifiedPfIconTrue() throws IOException {
        Controller controller = new Controller();
        File configFile = new File("build/test/basic.config");
        configFile.createNewFile();

        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("disableui=true \n use.pf.icon=true");
        fileWriter.close();

        controller.startConfig("build/test/basic.config");

        File directory = new File("build/test/myDirectory");
        directory.mkdir();
        File desktopIniFile = new File("build/test/myDirectory/" + PathUtils.DESKTOP_INI_FILENAME);
        desktopIniFile.createNewFile();
        desktopIniFile.setLastModified(0);

        File folderIcoDir = new File(controller.getMiscFilesLocation().toFile(), "skin/client");
        folderIcoDir.mkdirs();
        File folderIcoFile = new File(folderIcoDir,"Folder.ico");
        folderIcoFile.createNewFile();

        PathUtils.maintainDesktopIni(controller, directory.toPath());

        assertTrue(desktopIniFile.exists());
        assertTrue(desktopIniFile.length() > 0);
        assertTrue(desktopIniFile.lastModified() > 0);
        assertTrue(desktopIniFile.isHidden());
    }

    public void testGetDiskFileName(){
        assertEquals("/text/myFile", PathUtils.getDiskFileName("build","/text/myFile"));
        assertEquals("myFile", PathUtils.getDiskFileName("build/text/","myFile"));
        assertEquals("myFile", PathUtils.getDiskFileName("","myFile"));
        assertEquals("myFile.csv", PathUtils.getDiskFileName("C:/Builds/Testing","myFile.csv"));
    }

    public void testDigest() throws IOException, NoSuchAlgorithmException, InterruptedException {
        File file = new File("build/test/myFile.txt");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write("1234567890");
        writer.close();

        byte[] result = PathUtils.digest(file.toPath(), MessageDigest.getInstance("MD5"), new ProgressListener() {
            @Override
            public void progressReached(double percentageReached) {

            }
        });

        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] resultCalculated = digest.digest("1234567890".getBytes());

        for (int index = 0; index < result.length; index++) {
            assertEquals(result[index], resultCalculated[index]);
        }
    }

    public void testOpenFileNull() {
        Path path = null;
        try {
            PathUtils.openFile(path);
            fail("NullPointerException was not thrown");
        } catch (NullPointerException e){
            //OK
        }
    }

    public void testOpenFile() throws IOException {
        File file = new File("build/test/myFile.txt");
        file.createNewFile();
        assertTrue(PathUtils.openFile(file.toPath()));
    }

    public void testOpenDirectory() {
        File directory = new File("build/test/directoryOne");
        directory.mkdir();
        assertTrue(PathUtils.openFile(directory.toPath()));
    }

    public void testOpenFileIfExists() throws IOException {
        File nonExistent = new File("build/test/otherFile.pdf");
        assertFalse(PathUtils.openFileIfExists(nonExistent.toPath()));
        nonExistent.createNewFile();
        assertTrue(PathUtils.openFileIfExists(nonExistent.toPath()));
        FileUtils.forceDelete(nonExistent);
        assertFalse(PathUtils.openFileIfExists(nonExistent.toPath()));
    }

    public void testIsWebDavFolder() {
        File someFile = new File("build/test/folder" + Constants.FOLDER_WEBDAV_SUFFIX);
        assertTrue(PathUtils.isWebDAVFolder(someFile.toPath()));

        someFile = new File("myFile.txt");
        assertFalse(PathUtils.isWebDAVFolder(someFile.toPath()));

        someFile = new File("build/test/myFile" + Constants.FOLDER_WEBDAV_SUFFIX + "/Test");
        assertFalse(PathUtils.isWebDAVFolder(someFile.toPath()));

        someFile = new File("build/test/myFile" + Constants.FOLDER_WEBDAV_SUFFIX + ".csv");
        assertTrue(PathUtils.isWebDAVFolder(someFile.toPath()));
    }

    public void testIsScannableNull() {
        Controller controller = new Controller();
        String nullString = null;
        try {
            PathUtils.isScannable(nullString, new FolderRepository(controller).getFolders().iterator().next());
        } catch (NullPointerException e) {
            //OK since first argument was null
        }

        try {
            PathUtils.isScannable("test", null);
        } catch (NullPointerException e) {
            //OK since second argument was null
        }
    }

    public void testIsScannable() throws IOException {
        Controller controller = new Controller();
        File configFile = new File("build/test/basic.config");
        configFile.createNewFile();
        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/basic.config");

        FolderInfo testFolder = new FolderInfo("testFolder",
                IdGenerator.makeFolderId());
        FolderSettings folderSettings = new FolderSettings(Paths.get("build/test"),
                SyncProfile.HOST_FILES, 0);
        controller.getFolderRepository().createFolder(testFolder, folderSettings);


        assertFalse(PathUtils.isScannable("Test" + Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR, controller.getFolderRepository().getFolder(testFolder)));
        assertFalse(PathUtils.isScannable("TestIcon\r", controller.getFolderRepository().getFolder(testFolder)));
        assertTrue(PathUtils.isScannable("Testing", controller.getFolderRepository().getFolder(testFolder)));


    }

    public void testIsScannableMetaFolder() throws IOException {
        Controller controller = new Controller();
        File configFile = new File("build/test/basic.config");
        configFile.createNewFile();
        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/basic.config");

        FolderInfo testFolder = new FolderInfo("testFoldermeta", "meta|folder");
        FolderSettings folderSettings = new FolderSettings(Paths.get("build/test"),
                SyncProfile.HOST_FILES, 0);
        controller.getFolderRepository().createFolder(testFolder, folderSettings);

        assertFalse(PathUtils.isScannable("Test" + Constants.POWERFOLDER_SYSTEM_SUBDIR, controller.getFolderRepository().getFolders(true).iterator().next()));



    }
}