package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.Level;

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

        LoggingManager.setConsoleLogging(Level.FINE);

        // Actually create the subfolder
        Folder subFolder = folder.share(subDirInfo);

        assertNotNull(subFolder);
        assertEquals(folder.getInfo(), subFolder.getInfo().getParent().getFolderInfo());
        assertEquals("structure/deep", subFolder.getInfo().getParent().getRelativeName());

        LoggingManager.setConsoleLogging(Level.OFF);
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
        System.out.println(subFolderInfo.getName());

        FileInfo subFileInfo = FileInfoFactory.mapToSubFolder(topFileInfo, subFolderInfo);

        assertEquals("project/path/Info.txt", subFileInfo.getRelativeName());

        assertEquals(topFileInfo.getFilenameOnly(), subFileInfo.getFilenameOnly());
        assertEquals(topFileInfo.getSize(), subFileInfo.getSize());
        assertEquals(topFileInfo.getModifiedDate(), subFileInfo.getModifiedDate());
        assertEquals(subFolderInfo, subFileInfo.getFolderInfo());

        System.out.println(subFileInfo.getRelativeName());
        System.out.println(subFileInfo.toDetailString());
        System.out.println(subFileInfo.getFolderInfo());

        FileInfo topFileInfoBack = FileInfoFactory.mapToTopFolder(subFileInfo);

        assertEquals(topFileInfo.getFilenameOnly(), topFileInfoBack.getFilenameOnly());
        assertEquals(topFileInfo.getRelativeName(), topFileInfoBack.getRelativeName());
        assertEquals(topFileInfo.getSize(), topFileInfoBack.getSize());
        assertEquals(topFileInfo.getModifiedDate(), topFileInfoBack.getModifiedDate());
        assertEquals(topFileInfo.getFolderInfo(), topFileInfoBack.getFolderInfo());
    }

    public void testFileInfoDAO() {
        FileInfoDAO parentFileInfoDAO = new FileInfoDAOHashMapImpl("ME", null);


    }
}
