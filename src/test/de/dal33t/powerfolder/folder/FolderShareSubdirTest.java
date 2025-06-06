package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FolderShareSubdirTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }
    public void testSplit() throws IOException {
        Path subDirPath = Files.createDirectories(getFolderAtBart().getPhysicalDir().resolve("subdir"));
        TestHelper.scanFolder(getFolderAtBart());

        FileInfo subDirInfo = getFolderAtBart().getFile(FileInfoFactory.lookupInstance(getFolderAtBart(), subDirPath));
        assertTrue(subDirInfo.isDiretory());
    }
}
