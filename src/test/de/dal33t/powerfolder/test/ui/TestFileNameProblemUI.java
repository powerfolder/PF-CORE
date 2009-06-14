/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.problem.FilenameProblemHelper;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.test.TestHelper;

public class TestFileNameProblemUI {
    private final Controller controller;
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

        setupTestFolder(SyncProfile.HOST_FILES);

        FileNameProblemHandler handler = controller.getFolderRepository()
            .getFileNameProblemHandler();
        if (handler == null) {
            throw new NullPointerException();
        }
        ScanResult scanResult = new ScanResult(ScanResult.ResultState.SCANNED);
        FolderInfo folderInfo = folder.getInfo();

        List<FileInfo> fileInfoList = new ArrayList<FileInfo>();

        fileInfoList.add(FileInfo.getTemplate(folderInfo, "sub/AUX"));
        fileInfoList.add(FileInfo.getTemplate(folderInfo, "?hhh."));
        fileInfoList.add(FileInfo.getTemplate(folderInfo, "lowercase"));
        fileInfoList.add(FileInfo.getTemplate(folderInfo, "LOWERCASE"));
        Map<FileInfo, List<Problem>> problemFiles = getProblems(fileInfoList);
        scanResult.setProblemFiles(problemFiles);
        handler.fileNameProblemsDetected(new FileNameProblemEvent(folder,
            scanResult));
        // controller.shutdown();
    }

    private Map<FileInfo, List<Problem>> getProblems(
        Collection<FileInfo> files)
    {
        Map<FileInfo, List<Problem>> p = new HashMap<FileInfo, List<Problem>>();
        for (FileInfo fileInfo : files) {
            p.put(fileInfo, FilenameProblemHelper.getProblems(fileInfo));
        }
        return p;
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
        FolderSettings folderSettings = new FolderSettings(baseDir, profile,
            false, true, ArchiveMode.NO_BACKUP);
        aFolder = controller.getFolderRepository().createFolder(foInfo,
            folderSettings);
        if (aFolder.isDeviceDisconnected()) {
            System.out.println("Unable to join controller to " + foInfo + '.');
        }
        return aFolder;
    }
}
