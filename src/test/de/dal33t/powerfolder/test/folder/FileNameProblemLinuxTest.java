package de.dal33t.powerfolder.test.folder;

import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.FolderScanner;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.OSUtil;

/**
 * this test only runs on linux, since you cannot create files with these names
 * on windows.
 */
public class FileNameProblemLinuxTest extends ControllerTestCase {
    
    FolderScanner folderScanner;
   
    private int handlerCalledCount = 0;

    protected void setUp() throws Exception {
        if (OSUtil.isLinux()) {
            System.out.println("running linux specific Filename problem test");
            super.setUp();
    
            setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
            folderScanner = new FolderScanner(getController());
            folderScanner.start();

            getController().getFolderRepository().setFileNameProblemHandler(
                new FileNameProblemHandler() {

                    public void fileNameProblemsDetected(
                        FileNameProblemEvent fileNameProblemEvent)
                    {
                        handlerCalledCount++;
                        Map<FileInfo, List<FilenameProblem>> problems = fileNameProblemEvent
                            .getScanResult().getProblemFiles();
                        assertEquals(13, problems.size());
                    }

                });
        }
    }

    /**
     * this test only runs on linux, since you cannot create files with these
     * names on windows.
     */
    public void testFindProblems() {
        if (OSUtil.isLinux()) {

            // not valid on windows (1)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "AUX");
            // not valid on windows (2)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "AUX.txt");
            // not valid on windows (3)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "LPT1");
            // valid on windows
            TestHelper.createRandomFile(getFolder().getLocalBase(), "xLPT1");
            // valid on windows
            TestHelper.createRandomFile(getFolder().getLocalBase(), "xAUX.txt");
            // not valid on windows, but this results in a directory 'test' with file 'test' in it
            
            //TestHelper.createRandomFile(folder.getLocalBase(), "test/test");
            // not valid on windows (4)
            
            //our test fails on this file, because we regard a \ a directory symbol
            //TestHelper.createRandomFile(folder.getLocalBase(), "part1\\part2");
            // not valid on windows (4)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "?hhh");
            // not valid on windows (5)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "ddfgd*");
            // not valid on windows (6)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "<hhf");
            // not valid on windows (7)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "hj\"gfgfg");
            // not valid on windows (8)
            TestHelper.createRandomFile(getFolder().getLocalBase(), ":sds");
            // not valid on windows (9)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "gfgf>");
            // not valid on windows (10)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "ssdffd<");
            // not valid on windows (11)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "5655+gfgf");
            // not valid on windows (12)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "bb[gdgfd");
            // not valid on windows (13)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "]bb");

            ScanResult result = folderScanner.scanFolder(getFolder());
            assertEquals(15, result.getNewFiles().size());
            assertEquals(1, handlerCalledCount);
        }
    }

    protected void tearDown() throws Exception {
        if (OSUtil.isLinux()) {
            super.tearDown();
        }
    }
}
