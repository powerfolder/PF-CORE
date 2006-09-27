package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;

import de.dal33t.powerfolder.disk.FolderScanner;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;

public class FolderScannerTest extends ControllerTestCase {
   
    FolderScanner folderScanner;
   

    public void setUp() throws Exception {
        super.setUp();       
        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        folderScanner = new FolderScanner(getController());
        folderScanner.start();
    }

    public void testScanFiles() throws Exception {

        File file1 = TestHelper.createRandomFile(getFolder().getLocalBase());

        File file2 = TestHelper
            .createRandomFile(new File(
                getFolder().getLocalBase(),
                "deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore/deep/path/verydeep/more/andmore"));
        File file3 = TestHelper.createRandomFile(getFolder().getLocalBase());

        File file4 = TestHelper.createRandomFile(getFolder().getLocalBase());
        ScanResult result = folderScanner.scanFolder(getFolder());
        assertTrue(ScanResult.ResultState.SCANNED == result.getResultState());
        
        List<FileInfo> newFiles = result.getNewFiles();
        // new Scan should find 4
        assertEquals(4, newFiles.size());
        getFolder().forceScanOnNextMaintenance();
        getFolder().maintain();
        // old Scan should find 4
        assertEquals(4, getFolder().getFiles().length);
        
        // delete a file
        file1.delete();
        
        result = folderScanner.scanFolder(getFolder());
        assertTrue(ScanResult.ResultState.SCANNED == result.getResultState());
        
        // one deleted file should be found in new Scanning
        assertEquals(1, result.getDeletedFiles().size());

        getFolder().forceScanOnNextMaintenance();
        getFolder().maintain();
        // one deleted file should be found in old Scanning
        assertEquals(1, countDeleted(getFolder().getFiles()));
        
        //change a file
        TestHelper.changeFile(file2);        
        result = folderScanner.scanFolder(getFolder());
        assertTrue(ScanResult.ResultState.SCANNED == result.getResultState());
        
        assertEquals(1, result.getChangedFiles().size());
        
        //rename a file        
        assertTrue(file3.renameTo(new File(file3.getParentFile() , "newname.txt")));

        //move a file
        File newFileLocation = new File(file4.getParentFile() , "/sub/newname.txt");
        newFileLocation.getParentFile().mkdirs();
        assertTrue(file4.renameTo(newFileLocation));
        result = folderScanner.scanFolder(getFolder());
        assertTrue(ScanResult.ResultState.SCANNED == result.getResultState());
        
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
    
}
