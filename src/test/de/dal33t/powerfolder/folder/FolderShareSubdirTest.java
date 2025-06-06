package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class FolderShareSubdirTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }
    public void testSplit() throws IOException {
        Folder folder = getFolderAtBart();
        Path subDirPath = Files.createDirectories(folder.getPhysicalDir().resolve("structure/deep/sharedsubdir.123"));
        TestHelper.scanFolder(folder);

        FileInfo subDirInfo = folder.getFileInfo(subDirPath);
        assertTrue(subDirInfo instanceof DirectoryInfo);
        assertEquals("structure/deep", ((DirectoryInfo) subDirInfo).getParent().getRelativeName());

        LoggingManager.setConsoleLogging(Level.FINE);

        // Actually create the subfolder
        Folder subFolder = folder.split((DirectoryInfo) subDirInfo);

        assertNotNull(subFolder);
        assertEquals(folder.getInfo(), subFolder.getInfo().getParent().getFolderInfo());
        assertEquals("structure/deep", subFolder.getInfo().getParent().getRelativeName());

        LoggingManager.setConsoleLogging(Level.OFF);
    }
}
