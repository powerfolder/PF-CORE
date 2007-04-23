package de.dal33t.powerfolder.test.folder;

import java.io.File;
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
import de.dal33t.powerfolder.util.os.OSUtil;

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
            folderScanner = getController().getFolderRepository().getFolderScanner();
            
            getController().getFolderRepository().setFileNameProblemHandler(
                new FileNameProblemHandler() {
                    public void fileNameProblemsDetected(
                        FileNameProblemEvent fileNameProblemEvent)
                    {
                        handlerCalledCount++;
                        Map<FileInfo, List<FilenameProblem>> problems = fileNameProblemEvent
                            .getScanResult().getProblemFiles();
                        assertEquals(10, problems.size());
                        for (FileInfo problemFileInfo : problems.keySet()) {
                            List<FilenameProblem> problemList = problems.get(problemFileInfo);
                            
                            for (FilenameProblem problem : problemList) {
                                //solve it
                                FileInfo solved = problem.solve(getController());
                                if (!FilenameProblem.hasProblems(solved.getFilenameOnly())) {
                                    break;
                                }
                            }
                            
                        }
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
            // not valid on windows, but this results in a directory 'test' with
            // file 'test' in it

            // TestHelper.createRandomFile(folder.getLocalBase(), "test/test");
            // not valid on windows (4)

            // our test fails on this file, because we regard a \ a directory
            // symbol
            // TestHelper.createRandomFile(folder.getLocalBase(),
            // "part1\\part2");
            // not valid on windows (4)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "?hhh");
            // not valid on windows (5)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "ddfgd*");
            // not valid on windows (6)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "<hhf");
            // not valid on windows (7)
            TestHelper
                .createRandomFile(getFolder().getLocalBase(), "hj\"gfgfg");
            // not valid on windows (8)
            TestHelper.createRandomFile(getFolder().getLocalBase(), ":sds");
            // not valid on windows (9)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "gfgf>");
            // not valid on windows (10)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "gfgf<");

            ScanResult result = folderScanner.scanFolder(getFolder());
            assertEquals(12, result.getNewFiles().size());
            
            assertEquals(0, handlerCalledCount);
            // TODO SCHAATSER please check, was:
            // assertEquals(1, handlerCalledCount);
            
            File folderBaseDir = getFolder().getLocalBase();
            
            assertTrue(new File(folderBaseDir, "AUX").exists());
            assertTrue(new File(folderBaseDir, "AUX.txt").exists());
            assertTrue(new File(folderBaseDir, "LPT1").exists());
            assertTrue(new File(folderBaseDir, "xLPT1").exists());
            assertTrue(new File(folderBaseDir, "xAUX.txt").exists());
            assertTrue(new File(folderBaseDir, "?hhh").exists());
            assertTrue(new File(folderBaseDir, "ddfgd*").exists());
            assertTrue(new File(folderBaseDir, "<hhf").exists());
            assertTrue(new File(folderBaseDir, "hj\"gfgfg").exists());
            assertTrue(new File(folderBaseDir, ":sds").exists());
            assertTrue(new File(folderBaseDir, "gfgf>").exists());
            assertTrue(new File(folderBaseDir, "gfgf<").exists());            
           
        }
    }

    protected void tearDown() throws Exception {
        if (OSUtil.isLinux()) {
            super.tearDown();
        }
    }
}
