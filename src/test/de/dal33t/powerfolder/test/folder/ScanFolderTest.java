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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.FolderWatcher;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Tests the scanning of file in the local folders.
 * <p>
 * TODO Test scan of folder which already has a database.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
/**
 * @author sprajc
 */
public class ScanFolderTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getController().setPaused(true);
        setupTestFolder(SyncProfile.HOST_FILES);
    }

    public void testRootFileInfo() {
        FileInfo fInfo = FileInfoFactory.lookupInstance(getFolder().getInfo(),
            "");
        Path file = fInfo.getDiskFile(getController().getFolderRepository());
        assertNotNull(file);
        assertEquals(getFolder().getLocalBase(), file);

        try {
            fInfo = FileInfoFactory.lookupInstance(getFolder().getInfo(),
                "../Afile.txt");
            fail("Fileinfo relative name contained ..: " + fInfo);
        } catch (Exception e) {
            // OK
        }
    }

    public void testScanChangedFileMethod() throws IOException {
        Path file = TestHelper.createRandomFile(getFolder().getLocalBase(),
            10 + (int) (Math.random() * 100));

        FileInfo lookup = FileInfoFactory.lookupInstance(getFolder(), file);
        FileInfo fileInfo = getFolder().scanChangedFile(lookup);
        assertFalse(fileInfo.isDeleted());
        assertTrue(Files.exists(file));
        assertNotNull(fileInfo);
        assertNotSame(lookup, fileInfo);
        assertTrue(fileInfo.toDetailString(), lookup.equals(fileInfo));
        assertFalse(fileInfo.toDetailString(),
            lookup.isVersionDateAndSizeIdentical(fileInfo));
        assertFileMatch(file, fileInfo);
        assertEquals(0, fileInfo.getVersion());

        TestHelper.changeFile(file);
        fileInfo = getFolder().scanChangedFile(lookup);
        assertNotNull(fileInfo);
        assertNotSame(lookup, fileInfo);
        assertTrue(fileInfo.toDetailString(), lookup.equals(fileInfo));
        assertFalse(fileInfo.toDetailString(),
            lookup.isVersionDateAndSizeIdentical(fileInfo));
        assertFileMatch(file, fileInfo);
        assertEquals(1, fileInfo.getVersion());
        Files.delete(file);
        fileInfo = getFolder().scanChangedFile(lookup);
        assertNotNull(fileInfo);
        assertNotSame(lookup, fileInfo);
        assertTrue(fileInfo.toDetailString(), lookup.equals(fileInfo));
        assertFalse(fileInfo.toDetailString(),
            lookup.isVersionDateAndSizeIdentical(fileInfo));
        assertFileMatch(file, fileInfo);
        assertEquals(2, fileInfo.getVersion());
        assertTrue(fileInfo.isDeleted());
    }

    public void testScanSingleFileMulti() throws Exception {
        for (int i = 0; i < 40; i++) {
            testScanSingleFile();
            tearDown();
            setUp();
        }
    }

    /**
     * Tests the scan of one single file, including updates, deletion and
     * restore of the file.
     */
    public void testScanSingleFile() throws IOException {
        Path file = TestHelper.createRandomFile(getFolder().getLocalBase(),
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(file);
        scanFolder();

        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Delete.
        Files.delete(file);
        scanFolder();
        assertTrue(Files.notExists(file));
        assertTrue(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(3, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Restore.
        TestHelper.createRandomFile(file.getParent(), file.getFileName().toString());
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // 15 more filechanges
        for (int i = 0; i < 15; i++) {
            TestHelper.changeFile(file);
            scanFolder();
            assertEquals(5 + i, getFolder().getKnownFiles().iterator().next()
                .getVersion());
            assertFalse(getFolder().getKnownFiles().iterator().next()
                .isDeleted());
            assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        }

        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownItemCount());
    }

    /**
     * #1531 -Mixed case names of filenames and sub directories cause problems
     */
    public void testScanChangedSubdirName() throws IOException {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        Path file = TestHelper.createRandomFile(getFolder()
            .getLocalBase().resolve("subdir"), 10 + (int) (Math.random() * 100));
        Path sameName = getFolder().getLocalBase().resolve("SUBDIR/"
            + file.getFileName());

        scanFolder();
        // File + dir
        assertEquals(2, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertFileMatch(sameName, getFolder().getKnownFiles().iterator().next());

        Files.move(file, sameName);
        scanFolder();

        assertEquals(2, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertFileMatch(sameName, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(sameName);
        scanFolder();

        assertEquals(2, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertFileMatch(sameName, getFolder().getKnownFiles().iterator().next());
    }

    /**
     * Tests scanning of a file that only changes the last modification date,
     * but not the size.
     */
    public void testScanLastModifiedOnlyChanged() throws IOException {
        Path file = TestHelper.createRandomFile(getFolder().getLocalBase());
        long s = Files.size(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(s, Files.size(file));
        // 20 secs in future
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files
            .getLastModifiedTime(file).toMillis() + 1000L * 20));
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(s, Files.size(file));
        // 100 secs into the past
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files
            .getLastModifiedTime(file).toMillis() - 1000L * 100));
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(2, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(s, Files.size(file));
    }

    /**
     * Tests the scan of a file that doesn't has changed the last modification
     * date, but the size only.
     */
    public void testScanSizeOnlyChanged() throws IOException {
        Path file = TestHelper.createRandomFile(getFolder().getLocalBase());
        long lm = Files.getLastModifiedTime(file).toMillis();
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(lm, getFolder().getKnownFiles().iterator().next()
            .getModifiedDate().getTime());
        // 20 secs in future
        TestHelper.changeFile(file);
        Files.setLastModifiedTime(file, FileTime.fromMillis(lm));
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(lm, getFolder().getKnownFiles().iterator().next()
            .getModifiedDate().getTime());
        // 100 seks into the past
        TestHelper.changeFile(file);
        Files.setLastModifiedTime(file, FileTime.fromMillis(lm));
        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(2, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(lm, getFolder().getKnownFiles().iterator().next()
            .getModifiedDate().getTime());
    }

    /**
     * Tests the scan of one single file in a subdirectory.
     */
    public void testScanSingleFileInSubdir() throws IOException {
        Path subdir = getFolder().getLocalBase().resolve(
            "subDir1/SUBDIR2.ext");
        Files.createDirectories(subdir);
        Path file = TestHelper.createRandomFile(subdir,
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(3, getFolder().getKnownItemCount());
        assertEquals(1, getFolder().getKnownFiles().size());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Delete.
        Files.delete(file);
        scanFolder();
        assertTrue(Files.notExists(file));
        assertTrue(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertEquals(2, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Restore.
        TestHelper.createRandomFile(file.getParent(), file.getFileName()
            .toString());
        scanFolder();
        assertEquals(3, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Do some afterchecks.
        assertEquals(3, getFolder().getKnownItemCount());
    }

    public void testScanFileMovement() throws IOException {
        Path subdir = getFolder().getLocalBase().resolve(
            "subDir1/SUBDIR2.ext");
        Files.createDirectories(subdir);
        Path srcFile = TestHelper.createRandomFile(subdir,
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(3, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(srcFile, getFolder().getKnownFiles().iterator().next());

        // Move file one subdirectory up
        Path destFile = srcFile.getParent().getParent().resolve(
            srcFile.getFileName());
        Files.move(srcFile, destFile);
        scanFolder();

        // Should have two fileinfos: one deleted and one new.
        assertEquals(4, getFolder().getKnownItemCount());

        FileInfo destFileInfo = retrieveFileInfo(destFile);
        assertEquals(0, destFileInfo.getVersion());
        assertFalse(destFileInfo.isDeleted());
        assertFileMatch(destFile, destFileInfo);

        FileInfo srcFileInfo = retrieveFileInfo(srcFile);
        assertEquals(1, srcFileInfo.getVersion());
        assertTrue(srcFileInfo.isDeleted());
        assertFileMatch(srcFile, srcFileInfo);
    }

    public void testScanFileDeletion() throws IOException {
        Path subdir = getFolder().getLocalBase().resolve(
            "subDir1/SUBDIR2.ext");
        Files.createDirectories(subdir);
        Path file = TestHelper.createRandomFile(subdir,
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(3, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Delete file
        Files.delete(file);
        scanFolder();

        // Check
        FileInfo fInfo = getFolder().getKnownFiles().iterator().next();
        assertEquals(3, getFolder().getKnownItemCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        assertFileMatch(file, fInfo);

        // Scan again some times
        scanFolder();
        scanFolder();
        scanFolder();

        // Check again
        fInfo = getFolder().getKnownFiles().iterator().next();
        assertEquals(3, getFolder().getKnownItemCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        assertFileMatch(file, fInfo);
    }

    /**
     * Tests the scan of multiple files in multiple subdirectories.
     */
    public void testScanMulipleFilesInSubdirs() throws IOException {
        int nFiles = 1000;
        int nDirs = 0; // Count them
        Set<Path> testFiles = new HashSet<Path>();

        // Create a inital folder structure
        Path currentSubDir = getFolder().getLocalBase().resolve("subDir1");
        Files.createDirectory(currentSubDir);
        nDirs++;
        for (int i = 0; i < nFiles; i++) {
            if (Math.random() > 0.95) {
                // Change subdir
                boolean madeDir = false;
                do {
                    int depth = (int) (Math.random() * 3);
                    String fileName = "";
                    for (int j = 0; j < depth; j++) {
                        fileName += TestHelper.createRandomFilename() + "/";
                    }
                    fileName += TestHelper.createRandomFilename();
                    currentSubDir = getFolder().getLocalBase().resolve(
                        fileName);
                    try {
                        Files.createDirectory(currentSubDir);
                        nDirs++;
                        madeDir = true;
                    }
                    catch (IOException ioe) {
                        // Ignore.
                    }
                } while (!madeDir);
                System.err.println("New subdir: "
                    + currentSubDir.toAbsolutePath());
            }

            if (!currentSubDir.equals(getFolder().getLocalBase())) {
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
                        try {
                            Files.createDirectory(currentSubDir);
                            nDirs++;
                        }
                        catch (IOException ioe) {
                            // Ignore.
                        }
                    }
                }
            }

            Path file = TestHelper.createRandomFile(currentSubDir);
            testFiles.add(file);
        }

        for (int i = 0; i < 100; i++) {
            getController().setPaused(false);
            assertTrue(getFolder().scanLocalFiles());
            // syncFolder(getFolder());

            // Test
            // assertEquals("Files count: " + getFolder().getKnownFilesCount() +
            // " :" + getFolder().getKnownFiles() + " in " +
            // getFolder().getKnownDirectories(),
            // nFiles + nDirs, getFolder().getKnownFilesCount());
            Collection<FileInfo> files = getFolder().getKnownFiles();
            for (FileInfo info : files) {
                assertEquals(info.toDetailString(), 0, info.getVersion());
                assertFalse(info.isDeleted());
                Path diskFile = info.getDiskFile(getController()
                    .getFolderRepository());
                assertFileMatch(diskFile, info);
                assertTrue(testFiles.contains(diskFile));
            }

        }

    }

    /**
     * Tests the scan of very many files.
     * <p>
     * TOT Notes: This test takes @ 11000 files aprox. 40-107 (86) seconds.
     */
    public void testScanExtremlyManyFiles() throws IOException {
        final int nFiles = 44000;
        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < nFiles; i++) {
            if (i % 1000 == 0) {
                System.out.println("Still alive " + i + "/" + nFiles);
            }
            files.add(TestHelper
                .createRandomFile(getFolder().getLocalBase(), 5));
        }
        scanFolder();
        assertEquals(nFiles, getFolder().getKnownItemCount());

        for (Path file : files) {
            FileInfo fInfo = retrieveFileInfo(file);
            assertFileMatch(file, fInfo);
            assertEquals(fInfo.getRelativeName(), 0, fInfo.getVersion());
        }
    }

    /**
     * Tests the scan of very many files.
     * <p>
     * TOT Notes: This test takes @ 11000 files aprox. 40-107 (86) seconds.
     */
    public void testScanManyFileChanges() throws IOException {
        final int nFiles = 10;
        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < nFiles; i++) {
            if (i % 1000 == 0) {
                System.out.println("Still alive " + i + "/" + nFiles);
            }
            files.add(TestHelper
                .createRandomFile(getFolder().getLocalBase(), 5));
        }

        // Change all files
        for (int i = 0; i < 200; i++) {
            scanFolder();
            assertEquals(nFiles, getFolder().getKnownItemCount());
            for (Path file : files) {
                FileInfo fInfo = retrieveFileInfo(file);
                assertFileMatch(file, fInfo);
                assertEquals(fInfo.getRelativeName(), i, fInfo.getVersion());
            }
            for (Path file : files) {
                TestHelper.changeFile(file);
            }
        }

    }

    /**
     * Test the scan of file and dirs, that just change the case.
     * <p>
     * e.g. "TestDir/SubDir/MyFile.txt" to "testdir/subdir/myfile.txt"
     * <p>
     * TRAC #232
     */
    public void testCaseChangeScan() throws IOException {
        Path testFile = TestHelper.createRandomFile(getFolder().getLocalBase(),
            "TESTFILE.TXT");
        scanFolder(getFolder());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getKnownItemCount() == 1;
            }
        });

        assertEquals(testFile.getFileName().toString(), getFolder()
            .getKnownFiles().iterator().next().getFilenameOnly());

        // Change case
        Files.move(testFile, getFolder().getLocalBase().resolve("testfile.txt"));

        scanFolder();

        // HOW TO HANDLE THAT? WHAT TO EXPECT??
        // assertEquals(1, getFolderAtBart().getFilesCount());
    }

    public void testSwitchDirFile() throws IOException {
        Path testFile = TestHelper.createRandomFile(getFolder().getLocalBase(),
            "TESTFILE");
        scanFolder(getFolder());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getKnownFiles().size() == 1;
            }
        });
        assertEquals(0, getFolder().getKnownDirectories().size());
        TestHelper.waitMilliSeconds(4000);

        // Switch FILE -> DIR
        Files.delete(testFile);
        Files.createDirectory(testFile);
        assertTrue(Files.isDirectory(testFile));
        assertFalse(Files.isRegularFile(testFile));
        scanFolder(getFolder());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getKnownDirectories().size() == 1;
            }
        });
        assertEquals("Known DIRS: " + getFolder().getKnownDirectories(), 1,
            getFolder().getKnownDirectories().size());
        assertEquals("Known FILES: " + getFolder().getKnownFiles(), 0,
            getFolder().getKnownFiles().size());

        TestHelper.waitMilliSeconds(4000);
        // Switch DIR -> FOLDER
        Files.delete(testFile);
        Files.createFile(testFile);
        TestHelper.changeFile(testFile);
        assertFalse(Files.isDirectory(testFile));
        assertTrue(Files.isRegularFile(testFile));
        scanFolder(getFolder());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getKnownFiles().size() == 1;
            }
        });
        assertEquals(0, getFolder().getKnownDirectories().size());
    }

    /**
     * Tests the scan of one single file that gets changed into the past. This
     * test should ensure definied behavior.
     * <p>
     * Related TRAC ticket: #464
     */
    public void testScanLastModificationDateInPast() throws IOException {
        Path file = TestHelper.createRandomFile(getFolder().getLocalBase(),
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownItemCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Okay from now on we have a good state.
        // Now change the disk file 1 day into the past
        Path diskFile = getFolder().getKnownFiles().iterator().next()
            .getDiskFile(getController().getFolderRepository());
        Files.setLastModifiedTime(diskFile, FileTime.fromMillis(Files.getLastModifiedTime(diskFile).toMillis()
                - 24 * 60 * 60 * 1000));
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles().iterator().next()
            .getVersion());
        assertFalse(getFolder().getKnownFiles().iterator().next().isDeleted());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());

        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownItemCount());
    }

    /**
     * TRAC #1880
     * 
     * @throws IOException
     */
    public void testScanDirMovementWithWatcher() throws IOException {
        getController().setPaused(false);
        if (!FolderWatcher.isLibLoaded()) {
            System.err.println("NOT testing with file watcher. Lib not loaded");
            return;
        }
        LoggingManager.setConsoleLogging(Level.WARNING);
        getFolder().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // Subdir with 2 files
        Path subdir1 = getFolder().getLocalBase().resolve("subdir1");
        TestHelper.createRandomFile(subdir1);
        TestHelper.createRandomFile(subdir1);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolder().getKnownDirectories().size() == 1
                    && getFolder().getKnownFiles().size() == 2;
            }

            public String message() {
                return "Found disk items: " + getFolder().getKnownItemCount();
            }
        });

        // Now move
        Path subdir2 = getFolder().getLocalBase().resolve("SUBDIR2");
        PathUtils.recursiveMove(subdir1, subdir2);
        

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolder().getKnownDirectories().size() == 2
                    && getFolder().getKnownFiles().size() == 4;
            }

            public String message() {
                return "Found files (" + getFolder().getKnownFiles().size()
                    + "): " + getFolder().getKnownFiles() + ". dirs ("
                    + getFolder().getKnownDirectories().size() + "): "
                    + getFolder().getKnownDirectories();
            }
        });

        // Make subdir1 reappear!
        subdir1 = getFolder().getLocalBase().resolve("subdir1");
        TestHelper.createRandomFile(subdir1);
        TestHelper.createRandomFile(subdir1);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolder().getKnownDirectories().size() == 2
                    && getFolder().getKnownFiles().size() == 4;
            }

            public String message() {
                return "Found files (" + getFolder().getKnownFiles().size()
                    + "): " + getFolder().getKnownFiles() + ". dirs ("
                    + getFolder().getKnownDirectories().size() + "): "
                    + getFolder().getKnownDirectories();
            }
        });
    }

    // Helper *****************************************************************

    /**
     * @param file
     * @return the fileinfo in the test folder for this file.
     */
    private FileInfo retrieveFileInfo(Path file) {
        return getFolder().getFile(
            FileInfoFactory.lookupInstance(getFolder(), file));
    }

    private void scanFolder() {
        scanFolder(getFolder());
    }
}
