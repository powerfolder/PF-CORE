package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TestHelper.Condition;
import de.dal33t.powerfolder.util.IdGenerator;

public class FolderScannerTest extends ControllerTestCase {
    private static final String BASEDIR = "build/test/testFolder";

    private Folder folder;

    public void setUp() throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(new File(BASEDIR));
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        folder = getController().getFolderRepository().createFolder(testFolder,
            new File(BASEDIR));

    }

    public void testScanFiles() throws Exception {

        File file1 = TestHelper.createRandomFile(folder.getLocalBase());

        File file2 = TestHelper
            .createRandomFile(new File(
                folder.getLocalBase(),
                "deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore"));
        File file3 = TestHelper.createRandomFile(folder.getLocalBase());

        File file4 = TestHelper.createRandomFile(folder.getLocalBase());
        ScanResult result = scan(getController(), folder);

        List<FileInfo> newFiles = result.getNewFiles();
        // new Scan should find 4
        assertEquals(4, newFiles.size());
        folder.forceScanOnNextMaintenance();
        folder.maintain();
        // old Scan should find 4
        assertEquals(4, folder.getFiles().length);
        
        // delete a file
        file1.delete();
        
        result = scan(getController(), folder);
        // one deleted file should be found in new Scanning
        assertEquals(1, result.getDeletedFiles().size());

        folder.forceScanOnNextMaintenance();
        folder.maintain();
        // one deleted file should be found in old Scanning
        assertEquals(1, countDeleted(folder.getFiles()));
        
        //change a file
        TestHelper.changeFile(file2);        
        result = scan(getController(), folder);
        assertEquals(1, result.getChangedFiles().size());
        
        //rename a file        
        assertTrue(file3.renameTo(new File(file3.getParentFile() , "newname.txt")));

        //move a file
        File newFileLocation = new File(file4.getParentFile() , "/sub/newname.txt");
        newFileLocation.getParentFile().mkdirs();
        assertTrue(file4.renameTo(newFileLocation));
        result = scan(getController(), folder);
        
        //Find a file rename and movement!
        assertEquals(2, result.getMovedFiles().size());
    }

    
    
    private int countDeleted(FileInfo[] fileInfos) {
        int deletedCount = 0;
        for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.isDeleted()) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private ScanResult scan(Controller controller, Folder folderToScan) throws Exception {
        final FolderScanner scanner = new FolderScanner(controller);
        new Thread(scanner).start();
        scanner.scan(folderToScan, false);
        TestHelper.waitMilliSeconds(500);
        assertTrue(TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return !scanner.isScanning();
            }
        }));
        return scanner.getResult();
    }
}
