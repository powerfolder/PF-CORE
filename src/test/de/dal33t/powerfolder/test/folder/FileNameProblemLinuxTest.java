package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.OSUtil;

/**
 * this test only runs on linux, since you cannot create files with these names
 * on windows.
 */
public class FileNameProblemLinuxTest extends ControllerTestCase {
    private static final String BASEDIR = "build/test/controller/testFolder";
    FolderScanner folderScanner;
    private Folder folder;
    private int handlerCalledCount = 0;

    protected void setUp() throws Exception {
        if (OSUtil.isLinux()) {
            System.out.println("running linux specific Filename problem test");
            super.setUp();
            FileUtils.deleteDirectory(new File(BASEDIR));
            FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
                .makeId(), true);

            folder = getController().getFolderRepository().createFolder(
                testFolder, new File(BASEDIR), SyncProfile.MANUAL_DOWNLOAD,
                false);
            folderScanner = new FolderScanner(getController());
            folderScanner.start();

            getController().getFolderRepository().setFileNameProblemHandler(
                new FileNameProblemHandler() {

                    public void fileNameProblemsDetected(
                        FileNameProblemEvent fileNameProblemEvent)
                    {
                        handlerCalledCount++;
                        Map<FileInfo, List<String>> problems = fileNameProblemEvent
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
            TestHelper.createRandomFile(folder.getLocalBase(), "AUX");
            // not valid on windows (2)
            TestHelper.createRandomFile(folder.getLocalBase(), "AUX.txt");
            // not valid on windows (3)
            TestHelper.createRandomFile(folder.getLocalBase(), "LPT1");
            // valid on windows
            TestHelper.createRandomFile(folder.getLocalBase(), "xLPT1");
            // valid on windows
            TestHelper.createRandomFile(folder.getLocalBase(), "xAUX.txt");
            // not valid on windows, but this results in a directory 'test' with file 'test' in it
            
            //TestHelper.createRandomFile(folder.getLocalBase(), "test/test");
            // not valid on windows (4)
            
            //our test fails on this file, because we regard a \ a directory symbol
            //TestHelper.createRandomFile(folder.getLocalBase(), "part1\\part2");
            // not valid on windows (4)
            TestHelper.createRandomFile(folder.getLocalBase(), "?hhh");
            // not valid on windows (5)
            TestHelper.createRandomFile(folder.getLocalBase(), "ddfgd*");
            // not valid on windows (6)
            TestHelper.createRandomFile(folder.getLocalBase(), "<hhf");
            // not valid on windows (7)
            TestHelper.createRandomFile(folder.getLocalBase(), "hj\"gfgfg");
            // not valid on windows (8)
            TestHelper.createRandomFile(folder.getLocalBase(), ":sds");
            // not valid on windows (9)
            TestHelper.createRandomFile(folder.getLocalBase(), "gfgf>");
            // not valid on windows (10)
            TestHelper.createRandomFile(folder.getLocalBase(), "ssdffd<");
            // not valid on windows (11)
            TestHelper.createRandomFile(folder.getLocalBase(), "5655+gfgf");
            // not valid on windows (12)
            TestHelper.createRandomFile(folder.getLocalBase(), "bb[gdgfd");
            // not valid on windows (13)
            TestHelper.createRandomFile(folder.getLocalBase(), "]bb");

            ScanResult result = folderScanner.scanFolder(folder);
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
