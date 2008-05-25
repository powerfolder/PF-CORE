package de.dal33t.powerfolder.test.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderScanner;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.test.TestHelper;

public class TestFileNameProblemUI {
    private Controller controller;
    private static final File TESTFOLDER_BASEDIR = new File(TestHelper
        .getTestDir(), "/ControllerBart/testFolder");
    private Folder folder;

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            new TestFileNameProblemUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TestFileNameProblemUI() throws Exception {
        controller = Controller.createController();
        File source = new File("src/test-resources/ControllerBartUI.config");
        File target = new File(Controller.getMiscFilesLocation(),
            "ControllerBart.config");
        FileUtils.copyFile(source, target);

        controller.startConfig("ControllerBart");
        waitForStart(controller);

        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);

        FileNameProblemHandler handler = controller.getFolderRepository()
            .getFileNameProblemHandler();
        if (handler == null) {
            throw new NullPointerException();
        }
        ScanResult scanResult = new ScanResult();
        FolderInfo folderInfo = folder.getInfo();

        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        fileInfoList.add(new FileInfo(folderInfo, "sub/AUX"));
        fileInfoList.add(new FileInfo(folderInfo, "?hhh."));
        fileInfoList.add(new FileInfo(folderInfo, "lowercase"));
        fileInfoList.add(new FileInfo(folderInfo, "LOWERCASE"));
        Map<FileInfo, List<FilenameProblem>> problemFiles = FolderScanner
            .tryFindProblems(fileInfoList);
        scanResult.setProblemFiles(problemFiles);
        handler.fileNameProblemsDetected(new FileNameProblemEvent(folder,
            scanResult));
        // controller.shutdown();
    }

    /**
     * Waits for the controller to startup
     * 
     * @param aController
     * @throws InterruptedException
     */
    protected void waitForStart(Controller aController)
        throws InterruptedException
    {
        int i = 0;
        while (!aController.isStarted()) {
            i++;
            Thread.sleep(100);
            if (i > 100) {
                System.out.println("Unable to start controller");
            }
        }
    }

    /**
     * Joins the controller into a testfolder. get these testfolder with
     * <code>getFolder()</code>.
     */
    protected void setupTestFolder(SyncProfile syncprofile) {
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString());
        folder = joinFolder(testFolder, TESTFOLDER_BASEDIR, syncprofile);
        System.out.println(folder.getLocalBase());
    }

    /**
     * Let the controller join the specified folder.
     * 
     * @param foInfo
     *            the folder to join
     * @param baseDir
     *            the local base dir for the controller
     * @param profile
     *            the profile to use
     * @return the folder joined
     */
    protected Folder joinFolder(FolderInfo foInfo, File baseDir,
        SyncProfile profile)
    {
        final Folder aFolder;
            FolderSettings folderSettings = new FolderSettings(baseDir,
                profile, false, true);
            aFolder = controller.getFolderRepository().createFolder(foInfo,
                folderSettings);
        if (aFolder.isDeviceDisconnected()) {
            System.out.println("Unable to join controller to " + foInfo + '.');
        }
        return aFolder;
    }
}
