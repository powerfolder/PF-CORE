package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.FileWriter;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;

public class RecycleTest extends ControllerTestCase {
    
    public void setUp() throws Exception {
        // Remove directries
        
        super.setUp();

       setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        File localbase = getFolder().getLocalBase();
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }

        assertTrue(testFile.createNewFile());

        FileWriter writer = new FileWriter(testFile);
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        getFolder().forceScanOnNextMaintenance();
        getFolder().maintain();        
    }

    public void testRecycleBin() {
        System.out.println("testRecycleBin");
        FileInfo[] files = getFolder().getFiles();
        FileInfo testfile = files[0];
        File file = getFolder().getDiskFile(testfile);
        RecycleBin bin = getController().getRecycleBin();

        getFolder().removeFilesLocal(files);
        assertFalse(file.exists());
        assertTrue(bin.restoreFromRecycleBin(testfile));
        assertTrue(file.exists());
        getFolder().removeFilesLocal(files);
        assertFalse(file.exists());
        bin.delete(testfile);
        assertFalse(file.exists());
    }
    
}
