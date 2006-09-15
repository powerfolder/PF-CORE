package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderScanner;
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
    private String location = "build/test/controller/testFolder";
    FolderScanner folderScanner;
    private Folder folder;

    protected void setUp() throws Exception {
        if (OSUtil.isLinux()) {
            System.out.println("running linux specific Filename problem test");
            super.setUp();

            FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
                .makeId(), true);

            folder = getController().getFolderRepository().createFolder(
                testFolder, new File(location), SyncProfile.MANUAL_DOWNLOAD,
                false);
            folderScanner = new FolderScanner(getController());
            folderScanner.start();

            getController().getFolderRepository().setFileNameProblemHandler(
                new FileNameProblemHandler() {

                    public void fileNameProblemsDetected(
                        FileNameProblemEvent fileNameProblemEvent)
                    {
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

            TestHelper.createRandomFile(folder.getSystemSubDir(), "AUX");
        }
    }

    protected void tearDown() throws Exception {
        if (OSUtil.isLinux()) {
            super.tearDown();
        }
    }
}
