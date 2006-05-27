package de.dal33t.powerfolder.junit;

import junit.framework.Test;
import junit.framework.TestSuite;
import de.dal33t.powerfolder.junit.folder.CheckForDupeFilesTest;
import de.dal33t.powerfolder.junit.folder.FolderJoinTest;
import de.dal33t.powerfolder.junit.transfer.FileTransferTest;
import de.dal33t.powerfolder.junit.util.VersionCompareTest;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test powerfolder");
        
        suite.addTestSuite(RecycleTest.class);
        suite.addTestSuite(BandwidthLimitTest.class);
        suite.addTestSuite(TransferCounterTest.class);    
        suite.addTestSuite(PowerFolderLinkTest.class); 
        
        //folder
        suite.addTestSuite(CheckForDupeFilesTest.class);    
        suite.addTestSuite(FolderJoinTest.class);    
        //transfer
        suite.addTestSuite(FileTransferTest.class);    
        //util
        suite.addTestSuite(VersionCompareTest.class);
        return suite;
    }

}
