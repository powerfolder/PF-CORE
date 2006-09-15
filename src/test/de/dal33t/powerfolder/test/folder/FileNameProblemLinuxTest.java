package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderScanner;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.OSUtil;

public class FileNameProblemLinuxTest extends ControllerTestCase {
	private static final String BASEDIR =  "build/test/controller/testFolder";
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
                        assertEquals(1, problems.size());
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
        
            TestHelper.createRandomFile(folder.getLocalBase(), "AUX");
            ScanResult result = folderScanner.scanFolder(folder);
            System.out.println(result);
            assertEquals(1, result.getNewFiles().size());
            assertEquals(1, handlerCalledCount);
        }
    }

    protected void tearDown() throws Exception {
        if (OSUtil.isLinux()) {
            super.tearDown();
        }
    }
}
