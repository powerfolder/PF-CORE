package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.IdGenerator;

public class DetectHardwareFailure extends ControllerTestCase {
    private String location = "build/test/controller/testFolder";

    private Folder folder;

    public void setUp() throws Exception {
        // Remove directries
        FileUtils.deleteDirectory(new File(location));
        super.setUp();

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folder = getController().getFolderRepository().createFolder(testFolder,
            new File(location), SyncProfile.MANUAL_DOWNLOAD, false);

        File localbase = folder.getLocalBase();
        // create 100 random files
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(localbase);
        }
        File sub = new File(localbase, "sub");
        sub.mkdir();

        // create 100 random files in sub folder
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(sub);
        }
    }

    public void testHardwareFailure() throws IOException {
        folder.forceScanOnNextMaintenance();
        folder.maintain();
        assertEquals(200, folder.getFiles().length);
        // now delete the folder :-D
        FileUtils.deleteDirectory(folder.getLocalBase());

        folder.forceScanOnNextMaintenance();
        folder.maintain();
        assertEquals(200, folder.getFiles().length);
        // on hardware failure of deletion of folder of disk we don't want to
        // mark them as deleted. to prevent the los of files to spread over more
        // systems
        for (FileInfo fileInfo : folder.getFiles()) {
            assertFalse(fileInfo.isDeleted());
        }
    }
}
