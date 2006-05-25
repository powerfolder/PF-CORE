package de.dal33t.powerfolder.junit;

import java.io.File;
import java.io.FileWriter;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;

public class RecycleTest extends ControllerTestCase {
    private String location = "build/test/controller/testFolder";

    private Folder folder;

    public void setUp() throws Exception {
        // Remove directries
        FileUtils.deleteDirectory(new File(location));
        super.setUp();

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folder = getController().getFolderRepository().createFolder(testFolder,
            new File(location));

        File localbase = folder.getLocalBase();
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }

        assertTrue(testFile.createNewFile());

        FileWriter writer = new FileWriter(testFile);
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        folder.scan();

    }

    public void testRycycleBin() {
        System.out.println("testRycycleBin");
        FileInfo[] files = folder.getFiles();
        FileInfo testfile = files[0];
        File file = folder.getDiskFile(testfile);
        RecycleBin bin = getController().getRecycleBin();

        folder.removeFilesLocal(files);
        assertFalse(file.exists());
        assertTrue(bin.restoreFromRecycleBin(testfile));
        assertTrue(file.exists());
        folder.removeFilesLocal(files);
        assertFalse(file.exists());
        bin.delete(testfile);
        assertFalse(file.exists());
    }
    
}
