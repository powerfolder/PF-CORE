package de.dal33t.powerfolder.junit;

import java.io.File;
import java.io.FileWriter;

import junit.framework.TestCase;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class RecycleTest extends TestCase {
    private Folder folder;
    private Controller controller;
    public void setUp() {

        folder = createTempFolder();
        folder.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        File localbase = folder.getLocalBase();
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }
        try {
            assertTrue(testFile.createNewFile());

            FileWriter writer = new FileWriter(testFile);
            writer
                .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
            writer.close();            
            folder.scan();
            
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    public void testRycycleBin() {
        FileInfo[] files = folder.getFiles();
        FileInfo testfile = files[0];
        File file = folder.getDiskFile(testfile);
        RecycleBin bin = controller.getRecycleBin();
        
        folder.removeFilesLocal(files);
        assertTrue(!file.exists());
        assertTrue(bin.restoreFromRecycleBin(testfile));
        assertTrue(file.exists());
        folder.removeFilesLocal(files);
        assertTrue(!file.exists());
        bin.delete(testfile);
        assertTrue(!file.exists());
    }

    private Folder createTempFolder() {
        Options options = new Options();
        options
            .addOption(
                "c",
                "config",
                true,
                "<config file>. Sets the configuration file to start. Default: PowerFolder.config");

        String[] arg = new String[2];
        arg[0] = "--config";
        arg[1] = "test";
        CommandLineParser parser = new PosixParser();
        controller = Controller.createController();
        try {
            controller.startConfig(parser.parse(options, arg));
        } catch (ParseException pe) {
            fail(pe.toString());
        }
        FolderRepository repo = controller.getFolderRepository();
        FolderInfo folderInfo = new FolderInfo("Test", "$##$%#$%#$%", true);
        String tmpDir = System.getProperty("java.io.tmpdir")+  "PowerFolderTest\\";        
        assertTrue(tmpDir != null);
        assertTrue(tmpDir.length() > 0);

        File localDir = new File(tmpDir, "pf_junit_test");
        if (!localDir.exists()) {            
            assertTrue(localDir.mkdirs());
        }
        assertTrue(localDir.exists());

        try {
            // first run            
            return repo.createFolder(folderInfo, localDir);
        } catch (FolderException e) {
            // if runned before
            return repo.getFolders()[0];
        }
        
    }
}
