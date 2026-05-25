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
 */
package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SubFolderFileArchiverProxy;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SubFolderTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }
    public void testShare() throws IOException {
        String subDir = "structure/deep/sharedsubdir.123";

        Folder folder = getFolderAtBart();
        Files.createDirectories(folder.getPhysicalDir().resolve(subDir));
        TestHelper.scanFolder(folder);

        DirectoryInfo subDirInfo = (DirectoryInfo) folder.getFileInfo(subDir);
        assertEquals("structure/deep", subDirInfo.getParent().getRelativeName());

        // Actually create the subfolder
        Folder subFolder = folder.share(subDirInfo);

        assertNotNull(subFolder);
        assertEquals(folder.getInfo(), subFolder.getInfo().getParent().getFolderInfo());
        assertEquals("structure/deep", subFolder.getInfo().getParent().getRelativeName());

        Folder subFolderAgain = folder.share(subDirInfo);
        assertSame(subFolder, subFolderAgain);

        try {
            subFolder.share(subDirInfo);
            fail("Was able to share a subfolder in subfolder");
        } catch (Exception e) {
            // OK, should fail
        }

    }

    public void testSubdirDAOSingleFile() throws IOException {
        String subDir = "subdir";

        Folder folder = getFolderAtBart();
        TestHelper.createRandomFile(folder.getLocalBase(), "ONLY_in_TOP_FOLDER.txt");

        Path subdirPath = Files.createDirectories(folder.getPhysicalDir().resolve(subDir));
        Path testFile = TestHelper.createRandomFile(subdirPath, "GANZERNAME.txt");
        TestHelper.scanFolder(folder);

        FileInfo testFileInfo = folder.getFileInfo(testFile);
        assertNotNull(testFileInfo);
        assertNotNull(folder.getFile(testFileInfo));
        testFileInfo = folder.getFile(testFileInfo);

        DirectoryInfo subDirInfo = (DirectoryInfo) folder.getFileInfo(subDir);
        assertEquals("", subDirInfo.getParent().getRelativeName());

        // Actually create the subfolder
        Folder subFolder = folder.share(subDirInfo);

        FileInfo testFileInfoInSub = FileInfoFactory.mapToSubFolder(testFileInfo, subFolder.getInfo());
        assertNotNull(testFileInfoInSub);
        assertEquals(testFileInfo.getFilenameOnly(), testFileInfoInSub.getFilenameOnly());
        FileInfo testFileInfoAfterMapping = FileInfoFactory.mapToTopFolder(testFileInfoInSub);
        assertNotNull(testFileInfoAfterMapping);
        assertEquals(testFileInfo, testFileInfoAfterMapping);
        assertNotNull(folder.getDAO().find(testFileInfoAfterMapping, null));

        // ---

        LoggingManager.setConsoleLogging(Level.INFO);
        FileInfo rootOfSubdir = FileInfoFactory.lookupInstance(subFolder.getInfo(), "");
        rootOfSubdir = subFolder.getFile(rootOfSubdir);
        assertEquals("", rootOfSubdir.getRelativeName());
    }

    public void testSubdirDAOWithNestedSubdir() throws IOException {
        String subDir = "subdir";
        String nestedSubDir = "subdir-1";

        Folder folder = getFolderAtBart();
        TestHelper.createRandomFile(folder.getLocalBase(), "ONLY_in_TOP_FOLDER.txt");

        // create subdir and one file
        Path subdirPath = Files.createDirectories(folder.getPhysicalDir().resolve(subDir));
        Path testFile = TestHelper.createRandomFile(subdirPath, "GANZERNAME.txt");
        TestHelper.scanFolder(folder);

        FileInfo testFileInfo = folder.getFileInfo(testFile);
        assertNotNull(testFileInfo);
        assertNotNull(folder.getFile(testFileInfo));

        DirectoryInfo subDirInfo = (DirectoryInfo) folder.getFileInfo(subDir);
        assertEquals("", subDirInfo.getParent().getRelativeName());

        // Actually create the subfolder as new shared folder
        Folder subFolder = folder.share(subDirInfo);

        FileInfo testFileInfoInSub = FileInfoFactory.mapToSubFolder(testFileInfo, subFolder.getInfo());
        assertNotNull(testFileInfoInSub);
        assertEquals(testFileInfo.getFilenameOnly(), testFileInfoInSub.getFilenameOnly());

        FileInfo testFileInfoAfterMapping = FileInfoFactory.mapToTopFolder(testFileInfoInSub);
        assertNotNull(testFileInfoAfterMapping);
        assertEquals(testFileInfo, testFileInfoAfterMapping);
        assertNotNull(folder.getDAO().find(testFileInfoAfterMapping, null));

        // --- Check root of subdir
        FileInfo rootOfSubdir = FileInfoFactory.lookupInstance(subFolder.getInfo(), "");
        rootOfSubdir = subFolder.getFile(rootOfSubdir);
        assertNotNull(rootOfSubdir);

        // --- Now create nested subdir inside subFolder
        Path nestedSubdirPath = Files.createDirectories(subdirPath.resolve(nestedSubDir));
        Path nestedFile = TestHelper.createRandomFile(nestedSubdirPath, "NESTEDFILE.txt");
        TestHelper.scanFolder(folder);

        FileInfo nestedFileInfo = folder.getFileInfo(nestedFile);
        assertNotNull(nestedFileInfo);

        // map to subfolder view
        FileInfo nestedFileInfoInSub = FileInfoFactory.mapToSubFolder(nestedFileInfo, subFolder.getInfo());
        assertNotNull(nestedFileInfoInSub);
        assertEquals(nestedFileInfo.getFilenameOnly(), nestedFileInfoInSub.getFilenameOnly());

        // back to top
        FileInfo nestedFileInfoBack = FileInfoFactory.mapToTopFolder(nestedFileInfoInSub);
        assertEquals(nestedFileInfo, nestedFileInfoBack);

        // ensure DAO finds it
        assertNotNull(folder.getDAO().find(nestedFileInfoBack, null));

        DirectoryInfo nestedDirInfo = (DirectoryInfo) folder.getFileInfo(subDir + "/" + nestedSubDir);
        assertNotNull(nestedDirInfo);
        assertEquals(subDir, nestedDirInfo.getParent().getRelativeName());
    }


    public void testFileInfo() {
        String subDir = "structure/deep/sharedsubdir.123";

        FolderInfo topFolderInfo = FolderInfoFactory.newTopFolder("TOP", "TopFolder");
        FileInfo topFileInfo = FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                subDir + "/project/path/Info.txt",
                null, 100, null, null, new Date(), 0, null, false, null);

        DirectoryInfo subDirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                subDir,
                null, 0, null, null, new Date(), 0, null, true, null);

        FolderInfo subFolderInfo = FolderInfoFactory.newFolder(subDirInfo);
        FileInfo subFileInfo = FileInfoFactory.mapToSubFolder(topFileInfo, subFolderInfo);

        assertEquals("project/path/Info.txt", subFileInfo.getRelativeName());
        assertEquals(topFileInfo.getFilenameOnly(), subFileInfo.getFilenameOnly());
        assertEquals(topFileInfo.getSize(), subFileInfo.getSize());
        assertEquals(topFileInfo.getModifiedDate(), subFileInfo.getModifiedDate());
        assertEquals(subFolderInfo, subFileInfo.getFolderInfo());


        FileInfo topFileInfoBack = FileInfoFactory.mapToTopFolder(subFileInfo);

        assertEquals(topFileInfo.getFilenameOnly(), topFileInfoBack.getFilenameOnly());
        assertEquals(topFileInfo.getRelativeName(), topFileInfoBack.getRelativeName());
        assertEquals(topFileInfo.getSize(), topFileInfoBack.getSize());
        assertEquals(topFileInfo.getModifiedDate(), topFileInfoBack.getModifiedDate());
        assertEquals(topFileInfo.getFolderInfo(), topFileInfoBack.getFolderInfo());

        FileInfo topBaseFileInfo = FileInfoFactory.unmarshallExistingFile(topFolderInfo, subDir,
                null, 100, null, null, new Date(), 0, null, true, null);
        subFileInfo = FileInfoFactory.mapToSubFolder(topBaseFileInfo, subFolderInfo);

        assertEquals("", subFileInfo.getRelativeName());
        assertEquals("", subFileInfo.getFilenameOnly());
        assertEquals(topBaseFileInfo.getModifiedDate(), subFileInfo.getModifiedDate());
        assertEquals(subFolderInfo, subFileInfo.getFolderInfo());

        FileInfo topSingleCharFilename = FileInfoFactory.unmarshallExistingFile(topFolderInfo, subDir + "/N",
                null, 100, null, null, new Date(), 0, null, false, null);
        subFileInfo = FileInfoFactory.mapToSubFolder(topSingleCharFilename, subFolderInfo);

        assertEquals("N", subFileInfo.getRelativeName());
        assertEquals("N", subFileInfo.getFilenameOnly());
        assertEquals(topSingleCharFilename.getModifiedDate(), subFileInfo.getModifiedDate());
        assertEquals(subFolderInfo, subFileInfo.getFolderInfo());
    }

    public void testDAO() {
        String subDir = "structure/deep/sharedsubdir.123";
        FolderInfo topFolderInfo = FolderInfoFactory.newTopFolder("TOP", "TopFolder");
        DirectoryInfo subDirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                subDir,
                null, 0, null, null, new Date(), 0, null, true, null);
        FolderInfo subFolderInfo = FolderInfoFactory.newFolder(subDirInfo);

        FileInfoDAOHashMapImpl topDAO = new FileInfoDAOHashMapImpl("ME", null);
        FileInfoDAO subDAO = new SubFolderFileInfoDAOProxy(topDAO, subFolderInfo);

        FileInfo dirInSubdir = FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                "structure/deep/sharedsubdir.123/project/path",
                null, 100, null, null, new Date(), 0, null, true, null);
        topDAO.store(null, dirInSubdir);

        FileInfo subFile = FileInfoFactory.unmarshallExistingFile(subFolderInfo,
                "project/path/Info.txt",
                null, 100, null, null, new Date(), 0, null, false, null);
        subDAO.store(null, subFile);

        int topCount = topDAO.count(null, true, true);
        int subCount = subDAO.count(null, true, true);
        assertEquals(2, topCount);
        assertEquals(1, topDAO.findAllFiles(null).size());
        assertEquals(2, subCount);
        assertEquals(1, subDAO.findAllFiles(null).size());

        FileInfo topOnlyFile = FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                "in/a/different/subdir/not.txt",
                null, 100, null, null, new Date(), 0, null, false, null);

        try {
            subDAO.store(null, topOnlyFile);
            fail("Must not be able to store file in subDAO");
        } catch (Exception e) {
            // Expected
        }

        topDAO.store(null, topOnlyFile);

        topCount = topDAO.count(null, true, true);
        subCount = subDAO.count(null, true, true);
        assertEquals(3, topCount);
        assertEquals(2, topDAO.findAllFiles(null).size());
        assertEquals(1, topDAO.findAllDirectories(null).size());
        assertEquals(2, subCount);
        assertEquals(1, subDAO.findAllFiles(null).size());
        assertEquals(1, subDAO.findAllDirectories(null).size());

        subDAO.deleteDomain(null, 4);
        topCount = topDAO.count(null, true, true);
        subCount = subDAO.count(null, true, true);
        assertEquals(1, topCount);
        assertEquals(0, subCount);
        // --- Re-store subFile for findFiles tests ---
        subDAO.store(null, subFile);

        // --- findFiles ---

        FileInfoCriteria topCriteria = new FileInfoCriteria();
        topCriteria.addDomain("ME");
        topCriteria.setRecursive(true);
        topCriteria.setIncludeDeleted(true);

        FileInfoCriteria subCriteria = new FileInfoCriteria();
        subCriteria.addDomain("ME");
        subCriteria.setRecursive(true);
        subCriteria.setIncludeDeleted(true);

        Collection<FileInfo> resultTop = topDAO.findFiles(topCriteria);
        Collection<FileInfo> resultSub = subDAO.findFiles(subCriteria);

        assertEquals("Top DAO should return both files", 2, resultTop.size());
        assertEquals("Sub DAO should return only scoped file via findFiles", 1, resultSub.size());

        // --- findFilesFast ---

        FileInfoCriteria topCriteriaFast = new FileInfoCriteria();
        topCriteriaFast.addDomain("ME");
        topCriteriaFast.setRecursive(true);
        topCriteriaFast.setIncludeDeleted(true);

        FileInfoCriteria subCriteriaFast = new FileInfoCriteria();
        subCriteriaFast.addDomain("ME");
        subCriteriaFast.setRecursive(true);
        subCriteriaFast.setIncludeDeleted(true);

        Collection<FileInfo> resultTopFast = topDAO.findFilesFast(topCriteriaFast);
        Collection<FileInfo> resultSubFast = subDAO.findFilesFast(subCriteriaFast);

        assertEquals("Top DAO should return both files (fast)", 2, resultTopFast.size());
        assertEquals("Sub DAO should return only scoped file via findFilesFast", 1, resultSubFast.size());

        FileInfo subFileFound = resultSubFast.iterator().next();
        assertEquals(subFile, subFileFound);
      }

    public void testSubFolderDAOComplexHierarchyWithDeletedFilesAndDirs() {
        // --- Setup: Top and subfolder ---
        String subDir = "structure/deep/sharedsubdir.123";
        FolderInfo topFolderInfo = FolderInfoFactory.newTopFolder("TOP", "TopFolder");
        DirectoryInfo subDirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                topFolderInfo, subDir, null, 0, null, null, new Date(), 0, null, true, null);
        FolderInfo subFolderInfo = FolderInfoFactory.newFolder(subDirInfo);

        FileInfoDAOHashMapImpl topDAO = new FileInfoDAOHashMapImpl("ME", null);
        FileInfoDAO subDAO = new SubFolderFileInfoDAOProxy(topDAO, subFolderInfo);

        // --- Top-level files outside subfolder ---
        topDAO.store(null, FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                "readme.md", null, 10, null, null, new Date(), 1, null, false, null));
        topDAO.store(null, FileInfoFactory.unmarshallExistingFile(topFolderInfo,
                "structure/deep/unrelated/file.log", null, 20, null, null, new Date(), 1, null, false, null));

        // --- Files inside subfolder (active) ---
        FileInfo[] activeFiles = new FileInfo[]{
                FileInfoFactory.unmarshallExistingFile(subFolderInfo, "index.html", null, 100, null, null, new Date(), 1, null, false, null),
                FileInfoFactory.unmarshallExistingFile(subFolderInfo, "sub/data.csv", null, 200, null, null, new Date(), 1, null, false, null),
                FileInfoFactory.unmarshallExistingFile(subFolderInfo, "sub/deep/info.log", null, 300, null, null, new Date(), 1, null, false, null)
        };
        for (FileInfo file : activeFiles) {
            subDAO.store(null, file);
        }

        // --- Deleted file in subfolder ---
        FileInfo deletedFile = FileInfoFactory.unmarshallDeletedFile(
                subFolderInfo,
                "sub/deep/old_data.cfg",
                null, null, null, new Date(), 1, null, false, null);
        subDAO.store(null, deletedFile);

        // --- Directory inside subfolder ---
        FileInfo activeDir = FileInfoFactory.unmarshallExistingFile(
                topFolderInfo,
                "structure/deep/sharedsubdir.123/sub/deep",
                null, 0, null, null, new Date(), 1, null, true, null);
        topDAO.store(null, activeDir);

        // --- Deleted directory in subfolder ---
        FileInfo deletedDir = FileInfoFactory.unmarshallDeletedFile(
                subFolderInfo,
                "sub/deprecated_dir",
                null, null, null, new Date(), 1, null, true, null);
        subDAO.store(null, deletedDir);

        // --- File with similar path outside subfolder ---
        topDAO.store(null, FileInfoFactory.unmarshallExistingFile(
                topFolderInfo,
                "structure/deep/sharedsubdir_fake/sub/deep/old_data.cfg",
                null, 999, null, null, new Date(), 1, null, false, null));

        // --------------------------------------------
        // Assertions and Validations
        // --------------------------------------------

        // --- TopDAO should see everything ---
        assertEquals(7, topDAO.findAllFiles(null).size());

        // --- SubDAO should only see scoped entries ---
        assertEquals(4, subDAO.findAllFiles(null).size()); // 3 active + 1 deleted
        assertEquals(2, subDAO.findAllDirectories(null).size());

        // --- Criteria: include deleted ---
        FileInfoCriteria critAll = new FileInfoCriteria();
        critAll.addDomain("ME");
        critAll.setRecursive(true);
        critAll.setIncludeDeleted(true);
        Collection<FileInfo> allFiles = subDAO.findFiles(critAll);
        assertEquals(6, allFiles.size());

        // --- Criteria: exclude deleted ---
        FileInfoCriteria critNoDeleted = new FileInfoCriteria();
        critNoDeleted.addDomain("ME");
        critNoDeleted.setRecursive(true);
        critNoDeleted.setIncludeDeleted(false);
        Collection<FileInfo> activeOnly = subDAO.findFiles(critNoDeleted);
        assertEquals(4, activeOnly.size());

        // --- All files in sub must be correctly mapped ---
        for (FileInfo f : allFiles) {
            assertTrue("Must be in subfolder", f.isInSubFolder(subFolderInfo));
            assertFalse("Path should not leak full folder", f.getRelativeName().contains("sharedsubdir.123"));
            assertFalse("Must not contain sibling folder", f.getRelativeName().contains("sharedsubdir_fake"));
        }

        // --- findFilesFast parity ---
        Collection<FileInfo> fastAll = subDAO.findFilesFast(critAll);
        Collection<FileInfo> fastActive = subDAO.findFilesFast(critNoDeleted);
        assertEquals(6, fastAll.size());
        assertEquals(4, fastActive.size());

        // --- Path filter test ---
        FileInfoCriteria pathCrit = new FileInfoCriteria();
        pathCrit.addDomain("ME");
        pathCrit.setPath("sub/deep");
        pathCrit.setRecursive(false);
        pathCrit.setIncludeDeleted(true);
        Collection<FileInfo> filtered = subDAO.findFiles(pathCrit);
        assertEquals(2, filtered.size()); // info.log + old_data.cfg

        // --- Deletion scope test ---
        subDAO.deleteDomain(null, 0);
        assertEquals(3, topDAO.findAllFiles(null).size()); // 2 outside + 1 similar-path
        assertEquals(0, subDAO.findAllFiles(null).size());
    }

    public void testGetSubFoldersExplicitVsDirectoryOnly() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();

        /*
         * Logical structure:
         *
         * /top
         * ├── sharedA              (explicit subfolder)
         * ├── sharedB              (explicit subfolder)
         * │   └── implicitC        (directory, NOT a subfolder)
         * └── implicitD            (directory, NOT a subfolder)
         */
        Path root = topFolder.getPhysicalDir();

        Files.createDirectories(root.resolve("sharedA"));
        Files.createDirectories(root.resolve("sharedB"));
        Files.createDirectories(root.resolve("sharedB/implicitC"));
        Files.createDirectories(root.resolve("implicitD"));

        TestHelper.scanFolder(topFolder);

        // --- Explicitly create subfolders ---
        DirectoryInfo sharedAInfo =
                (DirectoryInfo) topFolder.getFileInfo("sharedA");
        DirectoryInfo sharedBInfo =
                (DirectoryInfo) topFolder.getFileInfo("sharedB");

        LoggingManager.setConsoleLogging(Level.INFO);
        Folder subA = topFolder.share(sharedAInfo);
        Folder subB = topFolder.share(sharedBInfo);

        assertEquals(3, getContollerBart().getFolderRepository().getFoldersCount());

        assertNotNull(subA);
        assertNotNull(subB);

        // --- Call API under test ---
        Map<DirectoryInfo, Folder> result =
                repository.getSubFolders(topFolder);

        // --- Only explicitly shared folders must be returned and the top folder ---
        assertEquals(3, result.size());
        assertTrue(result.toString(), result.containsKey(sharedAInfo));
        assertTrue(result.toString(), result.containsKey(sharedBInfo));

        // --- Directory-only entries (not subfolders) ---
        DirectoryInfo implicitC =
                (DirectoryInfo) topFolder.getFileInfo("sharedB/implicitC");
        DirectoryInfo implicitD =
                (DirectoryInfo) topFolder.getFileInfo("implicitD");

        assertNotNull(implicitC);
        assertNotNull(implicitD);

        assertFalse(result.containsKey(implicitC));
        assertFalse(result.containsKey(implicitD));
    }


    public void testGetSubFoldersWithNestedSharedFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository =
                getContollerBart().getFolderRepository();

        /*
         * Physical structure:
         *
         * /top
         * ├── sharedA                    (shared subfolder)
         * │   ├── implicit1              (directory, NOT shared)
         * │   └── sharedA1               (shared subfolder, same top)
         * │       └── implicit2          (directory, NOT shared)
         * └── implicitRoot               (directory, NOT shared)
         */
        Path root = topFolder.getPhysicalDir();

        Files.createDirectories(root.resolve("sharedA/implicit1"));
        Files.createDirectories(root.resolve("sharedA/sharedA1/implicit2"));
        Files.createDirectories(root.resolve("implicitRoot"));

        TestHelper.scanFolder(topFolder);

        // --- Share subfolder ---
        DirectoryInfo sharedAInfo =
                (DirectoryInfo) topFolder.getFileInfo("sharedA");
        Folder subA = topFolder.share(sharedAInfo);
        assertNotNull(subA);

        // --- Share nested folder (still same top folder) ---
        DirectoryInfo sharedA1Info =
                (DirectoryInfo) topFolder.getFileInfo("sharedA/sharedA1");
        Folder subA1 = topFolder.share(sharedA1Info);
        assertNotNull(subA1);

        // --- Sanity: both must point to the same top folder ---
        assertEquals(topFolder, subA.getTopFolder());
        assertEquals(topFolder, subA1.getTopFolder());

        // --- Call API under test ---
        Map<DirectoryInfo, Folder> result =
                repository.getSubFolders(topFolder);

        // --- Both shared folders must be returned and top folder ---
        assertEquals(3, result.size());
        assertTrue(result.containsKey(sharedAInfo));
        assertTrue(result.containsKey(sharedA1Info));

        // --- Implicit directories must NOT be returned ---
        DirectoryInfo implicit1 =
                (DirectoryInfo) topFolder.getFileInfo("sharedA/implicit1");
        DirectoryInfo implicit2 =
                (DirectoryInfo) topFolder.getFileInfo("sharedA/sharedA1/implicit2");
        DirectoryInfo implicitRoot =
                (DirectoryInfo) topFolder.getFileInfo("implicitRoot");

        assertNotNull(implicit1);
        assertNotNull(implicit2);
        assertNotNull(implicitRoot);

        assertFalse(result.containsKey(implicit1));
        assertFalse(result.containsKey(implicit2));
        assertFalse(result.containsKey(implicitRoot));
    }

    public void testUnshare() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();

        String subDir = "shared";
        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path testFile = TestHelper.createRandomFile(sharedPath, "data.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo sharedDirInfo =
                (DirectoryInfo) topFolder.getFileInfo(subDir);

        // --- Share ---
        Folder subFolder = topFolder.share(sharedDirInfo);
        assertNotNull(subFolder);
        assertTrue(subFolder.isSubFolder());
        assertEquals(2, repository.getFoldersCount());

        // File exists in top folder DAO
        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        assertNotNull(fileInTop);

        // --- Unshare ---
        topFolder.unshare(sharedDirInfo);

        // Subfolder is removed from repository
        assertEquals(1, repository.getFoldersCount());
        assertNull(repository.findSubFolder(sharedDirInfo));

        // File still exists in top folder DAO
        fileInTop = topFolder.getFileInfo(testFile);
        assertNotNull(fileInTop);

        // Physical file still on disk
        assertTrue(Files.exists(testFile));
    }

    public void testUnshareNonExistentIsNoop() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();

        Path root = topFolder.getPhysicalDir();
        Files.createDirectories(root.resolve("notshared"));
        TestHelper.scanFolder(topFolder);

        DirectoryInfo notSharedDir =
                (DirectoryInfo) topFolder.getFileInfo("notshared");

        int folderCountBefore = repository.getFoldersCount();

        // Unshare on a non-shared directory should be a no-op
        topFolder.unshare(notSharedDir);

        assertEquals(folderCountBefore, repository.getFoldersCount());
    }

    public void testUnshareTriggeredByFilesystemDeletion() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();

        String subDir = "todelete";
        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        TestHelper.createRandomFile(sharedPath, "content.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo =
                (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);
        assertNotNull(subFolder);
        assertEquals(2, repository.getFoldersCount());

        // Delete the directory on the filesystem
        PathUtils.recursiveDelete(sharedPath);
        assertFalse(Files.exists(sharedPath));

        // Scan should detect deletion and trigger unshare
        TestHelper.scanFolder(topFolder);

        assertNull(repository.findSubFolder(dirInfo));
        assertEquals(1, repository.getFoldersCount());
    }

    public void testUnshareTriggeredByRemoteDeletion() throws IOException {
        LoggingManager.setConsoleLogging(Level.FINE);
        Folder topFolderBart = getFolderAtBart();
        Folder topFolderLisa = getFolderAtLisa();
        FolderRepository repositoryBart = getContollerBart().getFolderRepository();

        topFolderBart.setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        topFolderLisa.setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        String subDir = "remotedel";

        // Create directory and file on Bart
        Path bartRoot = topFolderBart.getPhysicalDir();
        Path bartSharedPath = Files.createDirectories(bartRoot.resolve(subDir));
        TestHelper.createRandomFile(bartSharedPath, "remote.txt");
        TestHelper.scanFolder(topFolderBart);

        // Share the subfolder on Bart
        DirectoryInfo dirInfo =
                (DirectoryInfo) topFolderBart.getFileInfo(subDir);
        Folder subFolder = topFolderBart.share(dirInfo);
        assertNotNull(subFolder);
        assertEquals(2, repositoryBart.getFoldersCount());

        // Wait for Lisa to receive the files
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return topFolderLisa.getKnownItemCount() >= 2;
            }
            @Override
            public String message() {
                return "Known items at Lisa: " + topFolderLisa.getKnownItemCount();
            }
        });

        // Delete the directory on Lisa's filesystem
        Path lisaSharedPath = topFolderLisa.getPhysicalDir().resolve(subDir);
        assertTrue(Files.exists(lisaSharedPath));
        PathUtils.recursiveDelete(lisaSharedPath);
        assertFalse(Files.exists(lisaSharedPath));

        // Lisa scans and detects the deletion
        TestHelper.scanFolder(topFolderLisa);

        // Wait for Lisa's deletion to be broadcast to Bart
        final Member lisaAtBart = getContollerBart().getNodeManager().getNode(
                getContollerLisa().getMySelf().getInfo());
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                Collection<DirectoryInfo> lisaDirs =
                        topFolderBart.getDirectoriesAsCollection(lisaAtBart);
                if (lisaDirs == null) return false;
                for (DirectoryInfo d : lisaDirs) {
                    if (d.getRelativeName().equals(subDir) && d.isDeleted()) {
                        return true;
                    }
                }
                return false;
            }
            @Override
            public String message() {
                return "Lisa's deleted dir not yet visible at Bart";
            }
        });

        // Explicitly trigger remote deletion sync on Bart
        topFolderBart.syncRemoteDeletedFiles(true);

        assertNull(repositoryBart.findSubFolder(dirInfo));
        assertEquals(1, repositoryBart.getFoldersCount());
    }

    public void testUnshareTriggeredByScanChangedFile() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();

        String subDir = "scanchange";
        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        TestHelper.createRandomFile(sharedPath, "file.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo =
                (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);
        assertNotNull(subFolder);
        assertEquals(2, repository.getFoldersCount());

        // Delete directory then trigger single-file scan
        PathUtils.recursiveDelete(sharedPath);

        FileInfo dirFileInfo = topFolder.getFile(
                FileInfoFactory.lookupDirectory(topFolder.getInfo(), subDir));
        assertNotNull(dirFileInfo);

        topFolder.scanChangedFile(dirFileInfo);

        assertNull(repository.findSubFolder(dirInfo));
        assertEquals(1, repository.getFoldersCount());
    }

    public void testSubFolderUsesArchiverProxy() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "projects/team/shared";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        TestHelper.createRandomFile(sharedPath, "file1.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);
        assertNotNull(subFolder);

        assertTrue("Subfolder archiver should be a proxy",
            subFolder.getFileArchiver() instanceof SubFolderFileArchiverProxy);
        assertNotSame("Subfolder archiver should not be the same instance as top folder's",
            topFolder.getFileArchiver(), subFolder.getFileArchiver());
    }

    public void testSubFolderArchiverArchivesIntoTopFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "data/reports/monthly";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path nestedDir = Files.createDirectories(sharedPath.resolve("2024/q1"));
        Path testFile = TestHelper.createRandomFile(nestedDir, "versioned.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        assertNotNull(fileInTop);
        assertEquals("data/reports/monthly/2024/q1/versioned.txt", fileInTop.getRelativeName());

        FileInfo fileInSub = FileInfoFactory.mapToSubFolder(fileInTop, subFolder.getInfo());
        assertNotNull(fileInSub);
        assertEquals("2024/q1/versioned.txt", fileInSub.getRelativeName());

        // Archive the file via the subfolder's archiver (proxy)
        subFolder.getFileArchiver().archive(fileInSub, testFile, true);

        // The archived file should be findable via both archivers
        assertTrue("Top archiver should find the archived file",
            topFolder.getFileArchiver().hasArchivedFileInfo(fileInTop));
        assertTrue("Sub archiver should find the archived file",
            subFolder.getFileArchiver().hasArchivedFileInfo(fileInSub));

        // Retrieve archived versions via subfolder archiver
        List<FileInfo> subVersions = subFolder.getFileArchiver().getArchivedFilesInfos(fileInSub);
        assertFalse("Should have archived versions", subVersions.isEmpty());

        for (FileInfo version : subVersions) {
            assertEquals("Archived version should have subfolder's FolderInfo",
                subFolder.getInfo(), version.getFolderInfo());
        }

        // Retrieve via top archiver
        List<FileInfo> topVersions = topFolder.getFileArchiver().getArchivedFilesInfos(fileInTop);
        assertFalse("Top should also have archived versions", topVersions.isEmpty());
        assertEquals(subVersions.size(), topVersions.size());
    }

    public void testSubFolderArchiverRestore() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "workspace/modules/core";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path deepDir = Files.createDirectories(sharedPath.resolve("src/main"));
        Path testFile = TestHelper.createRandomFile(deepDir, "toRestore.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        FileInfo fileInSub = FileInfoFactory.mapToSubFolder(fileInTop, subFolder.getInfo());
        assertEquals("src/main/toRestore.txt", fileInSub.getRelativeName());

        // Archive via subfolder proxy
        subFolder.getFileArchiver().archive(fileInSub, testFile, true);

        // Get archived version info
        List<FileInfo> versions = subFolder.getFileArchiver().getArchivedFilesInfos(fileInSub);
        assertFalse(versions.isEmpty());
        FileInfo archivedVersion = versions.get(0);

        // Restore to a temporary path
        Path restoreTarget = root.resolve("restored_toRestore.txt");
        boolean restored = subFolder.getFileArchiver().restore(archivedVersion, restoreTarget);
        assertTrue("Restore should succeed", restored);
        assertTrue("Restored file should exist", Files.exists(restoreTarget));
    }

    public void testSubFolderArchiverSizeIsZero() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "departments/engineering/builds";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        TestHelper.createRandomFile(sharedPath, "file.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        assertEquals("Subfolder archiver size should be 0", 0, subFolder.getFileArchiver().getSize());
    }

    public void testSubFolderArchiverMaintainAndCleanupIsNoop() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "storage/archive/2024";

        Path root = topFolder.getPhysicalDir();
        Files.createDirectories(root.resolve(subDir));
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        List<FileInfo> result = subFolder.getFileArchiver().maintainAndCleanup(
            new Date(), subFolder.getDAO(), subFolder.getInfo(),
            getContollerBart().getMySelf().getAccountInfo());
        assertTrue("maintainAndCleanup on proxy should return empty list", result.isEmpty());
    }

    public void testSubFolderArchiverPurgeFile() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "projects/docs/internal";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path testFile = TestHelper.createRandomFile(sharedPath, "report.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        FileInfo fileInSub = FileInfoFactory.mapToSubFolder(
            fileInTop, subFolder.getInfo());

        // Archive via subfolder proxy
        subFolder.getFileArchiver().archive(fileInSub, testFile, true);
        assertTrue(subFolder.getFileArchiver().hasArchivedFileInfo(fileInSub));
        assertTrue(topFolder.getFileArchiver().hasArchivedFileInfo(fileInTop));

        // Purge via subfolder proxy
        subFolder.getFileArchiver().purge(fileInSub, subFolder, null);

        // Archived version should be gone from both views
        assertFalse("Sub archiver should no longer have archived file",
            subFolder.getFileArchiver().hasArchivedFileInfo(fileInSub));
        assertFalse("Top archiver should no longer have archived file",
            topFolder.getFileArchiver().hasArchivedFileInfo(fileInTop));
    }

    public void testSubFolderArchiverPurgeFolderMismatch() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "teams/backend/services";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        TestHelper.createRandomFile(sharedPath, "service.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);

        // Passing the top folder to the subfolder's archiver should fail
        try {
            subFolder.getFileArchiver().purge(topFolder, null);
            fail("Should have thrown IllegalArgumentException "
                + "for folder archive mismatch");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("mismatch"));
        }
    }

    /**
     * PF-1790: mapToSubFolder creates the subfolder's base directory as a
     * lookup instance (size=null). This is by design — the base directory
     * has an empty filename and cannot pass validate() with a real size.
     * Callers like findSameFiles must guard against calling getSize() on it.
     */
    public void testMapToSubFolderBaseDirectoryIsLookupInstance() {
        String subDir = "structure/deep/sharedsubdir.123";

        FolderInfo topFolderInfo = FolderInfoFactory.newTopFolder("TOP", "TopFolder");
        DirectoryInfo topBaseDir = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                topFolderInfo, subDir,
                null, 0, null, null, new Date(), 1, null, true, null);

        FolderInfo subFolderInfo = FolderInfoFactory.newFolder(topBaseDir);

        FileInfo subBaseDir = FileInfoFactory.mapToSubFolder(topBaseDir, subFolderInfo);
        assertEquals("", subBaseDir.getRelativeName());
        assertTrue("Mapped base dir must be a lookup instance", subBaseDir.isLookupInstance());
    }

    /**
     * Reproduces the production NPE: readExternal converts size=-1 to null,
     * then getSize() auto-unboxes null → NPE.
     * This is the v27-to-v27 server communication path: one node has a FileInfo
     * with size=null (e.g. from DB or internal creation), writes it as -1 via
     * writeExternal, the receiver reads -1 → null, then findSameFiles calls getSize().
     */
    public void testReadExternalSizeMinusOneGetSizeNPE() throws Exception {
        FolderInfo topFolderInfo = FolderInfoFactory.newTopFolder("TOP", "TopFolder");
        DirectoryInfo dirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                topFolderInfo, "somedir",
                null, 0, null, null, new Date(), 1, null, true, null);

        // Simulate writeExternal → readExternal roundtrip (v27 to v27)
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        dirInfo.writeExternal(oos);
        oos.flush();

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        FileInfo deserialized = FileInfoFactory.readExt(ois);

        try {
            long size = deserialized.getSize();
            assertEquals(0L, size);
        } catch (NullPointerException e) {
            fail("getSize() must not throw NPE after readExternal deserializes size=-1 as null");
        }
    }

    public void testUnshareOnlyAllowedFromTopFolder() throws IOException {
        Folder topFolder = getFolderAtBart();

        Path root = topFolder.getPhysicalDir();
        Files.createDirectories(root.resolve("sub1"));
        TestHelper.scanFolder(topFolder);

        DirectoryInfo sub1Info =
                (DirectoryInfo) topFolder.getFileInfo("sub1");
        Folder subFolder = topFolder.share(sub1Info);

        try {
            subFolder.unshare(sub1Info);
            fail("Unshare from subfolder should throw");
        } catch (Exception expected) {
            // correct
        }
    }
}
