package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;

public class FolderShareSubdirTest extends TwoControllerTestCase {

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

        FileInfo rootOfSubdir = FileInfoFactory.lookupInstance(subFolder.getInfo(), "");
        rootOfSubdir = subFolder.getFile(rootOfSubdir);
        assertNotNull(rootOfSubdir);
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
        System.out.println(allFiles);
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


}
