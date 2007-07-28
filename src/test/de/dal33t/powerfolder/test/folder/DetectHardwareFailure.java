package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;

public class DetectHardwareFailure extends ControllerTestCase {

    public void setUp() throws Exception {
        
        super.setUp();     

        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        
        File localbase = getFolder().getLocalBase();
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
        scanFolder(getFolder());
        assertEquals(200, getFolder().getKnowFilesAsArray().length);
        // now delete the folder :-D
        FileUtils.deleteDirectory(getFolder().getLocalBase());
        
        scanFolder(getFolder());
        assertEquals(200, getFolder().getKnowFilesAsArray().length);
        // on hardware failure of deletion of folder of disk we don't want to
        // mark them as deleted. to prevent the los of files to spread over more
        // systems
        for (FileInfo fileInfo : getFolder().getKnownFiles()) {
            assertFalse(fileInfo.isDeleted());
        }
    }
}
