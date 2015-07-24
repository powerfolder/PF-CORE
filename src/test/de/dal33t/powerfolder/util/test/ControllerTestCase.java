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
 * $Id: ControllerTestCase.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.util.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Format;

/**
 * Provides basic testcase-setup with a controller.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that controller is
 * running
 * <p>
 * You can access the controller and do manupulating/testing stuff on it
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class ControllerTestCase extends TestCase {
    // For the optional test folder.
    private static final Path TESTFOLDER_BASEDIR = TestHelper.getTestDir()
        .resolve("ControllerBart/testFolder").toAbsolutePath();

    private Controller controller;

    // The optional test folders
    private Folder folder;

    @Override
    protected void setUp() throws Exception {
        System.setProperty("user.home", Paths.get("build/test/home")
            .toAbsolutePath().toString());
        super.setUp();

        Feature.setupForTests();

        // Cleanup
        TestHelper.cleanTestDir();

        // Copy fresh configs
        // Start controllers
        System.out.println("Starting controller...");
        controller = Controller.createController();
        Path source = Paths.get("src/test-resources/ControllerBart.config");
        Path target = Controller.getMiscFilesLocation().resolve(
            "ControllerBart.config");
        Files.copy(source, target);
        assertTrue(Files.exists(target));

        controller.startConfig("ControllerBart");
        waitForStart(controller);

        System.out.println("Controller started");
        
        // Let the start settle down.
        TestHelper.waitMilliSeconds(250);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        controller.shutdown();

        int i = 0;
        while (controller.isShuttingDown()) {
            i++;
            if (i > 100) {
                System.out.println("shutdown of controller 2 failed");
                break;
            }
            Thread.sleep(1000);
        }
        assertFalse(controller.isStarted());

    }

    // For subtest ************************************************************

    protected Controller getController() {
        return controller;
    }

    protected String getControllerNodeID() {
        return controller.getMySelf().getId();
    }

    // Helpers ****************************************************************

    /**
     * @see #setupTestFolder(SyncProfile)
     * @return the test folde. or null if not setup yet.
     */
    protected Folder getFolder() {
        return folder;
    }

    /**
     * Joins the controller into a testfolder. get these testfolder with
     * <code>getFolder()</code>.
     *
     * @see #getFolder()
     * @param syncprofile
     * @param useRecycleBin
     *            whether to folder supports the recycle bin.
     */
    protected void setupTestFolder(SyncProfile syncprofile)
    {
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString());
        folder = joinFolder(testFolder, TESTFOLDER_BASEDIR, syncprofile);
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
    protected Folder joinFolder(FolderInfo foInfo, Path baseDir,
        SyncProfile profile)
    {
        try {
            Files.createDirectories(baseDir);
            baseDir = baseDir.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FolderSettings folderSettings = new FolderSettings(baseDir, profile, 5);
        return getController().getFolderRepository().createFolder(foInfo,
            folderSettings);
    }

    /**
     * Scans a folder and waits for the scan to complete.
     */
    protected synchronized void scanFolder(Folder aFolders) {
        TestHelper.scanFolder(aFolders);
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
                fail("Unable to start controller");
            }
        }
    }

    /**
     * Tests if the diskfile matches the fileinfo. Checks name, lenght/size,
     * modification date and the deletion status.
     *
     * @param diskFile
     *            the diskfile to compare
     * @param fInfo
     *            the fileinfo
     */
    protected void assertFileMatch(Path diskFile, FileInfo fInfo) throws IOException {
        boolean nameMatch = diskFile.getFileName().toString().equals(fInfo.getFilenameOnly());

        boolean fileObjectEquals = diskFile.equals(fInfo.getDiskFile(controller
            .getFolderRepository()));
        boolean deleteStatusMatch = Files.exists(diskFile) == !fInfo.isDeleted();
        long lastModified = 0L;
        try {
            lastModified = Files.getLastModifiedTime(diskFile).toMillis();
        } catch (IOException ioe) {
            // Ignore.
        }
        boolean lastModifiedMatch = lastModified == fInfo.getModifiedDate().getTime();
        long size = 0L;
        try {
            size = Files.size(diskFile);
        } catch (IOException ioe) {
            // Ignore.
        }
        boolean sizeMatch = fInfo.isDeleted()
            || (size == fInfo.getSize());

        // Skip last modification test when diskfile is deleted.
        boolean matches = !Files.isDirectory(diskFile) && nameMatch && sizeMatch
            && (Files.notExists(diskFile) || lastModifiedMatch) && deleteStatusMatch
            && fileObjectEquals;

        assertTrue("FileInfo does not match physical file. \nFileInfo:\n "
            + fInfo.toDetailString() + "\nFile:\n "
            + (Files.exists(diskFile) ? "" : "(del) ") + diskFile.getFileName()
            + ", size: " + Format.formatBytes(size)
            + ", lastModified: " + new Date(lastModified) + " (" + lastModified + ")"
            + "\n\nWhat matches?:\nName: " + nameMatch + "\nSize: " + sizeMatch
            + "\nlastModifiedMatch: " + lastModifiedMatch + "\ndeleteStatus: "
            + deleteStatusMatch + "\nFileObjectEquals: " + fileObjectEquals,
            matches);
    }
}
