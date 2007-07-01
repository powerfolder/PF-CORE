/* $Id: TwoControllerTestCase.java,v 1.2 2006/04/21 22:58:42 totmacherr Exp $
 */
package de.dal33t.powerfolder.test;

import java.io.File;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;

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
public class ControllerTestCase extends TestCase {
    // For the optional test folder.
    private static final File TESTFOLDER_BASEDIR = new File(TestHelper
        .getTestDir(), "/ControllerBart/testFolder");

    private Controller controller;

    // The optional test folders
    private Folder folder;

    private boolean initalScanOver = false;

    protected void setUp() throws Exception {
        super.setUp();

        Logger.removeExcludeConsoleLogLevel(Logger.VERBOSE);
        System.setProperty("powerfolder.test", "true");

        // Cleanup
        TestHelper.cleanTestDir();

        // Copy fresh configs

        // Start controllers
        System.out.println("Starting controller...");
        controller = Controller.createController();
        File source = new File("src/test-resources/ControllerBart.config");
        File target = new File(Controller.getMiscFilesLocation(),
            "ControllerBart.config");
        FileUtils.copyFile(source, target);
        assertTrue(target.exists());

        controller.startConfig("ControllerBart");
        waitForStart(controller);
        // Wait for initial maintenance
        triggerAndWaitForInitialMaitenenace(controller);
        controller.getPreferences().putBoolean("createdesktopshortcuts", false);
        
        System.out.println("Controller started");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        controller.shutdown();

        // Give them time to shut down
        Thread.sleep(1000);
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
    protected void setupTestFolder(SyncProfile syncprofile,
        boolean useRecycleBin)
    {
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);
        folder = joinFolder(testFolder, TESTFOLDER_BASEDIR, syncprofile,
            useRecycleBin);
        System.out.println(folder.getLocalBase());
    }

    /**
     * Joins the controller into a testfolder. get these testfolder with
     * <code>getFolder()</code>. Uses recycle bin.
     * 
     * @see #getFolder()
     */
    protected void setupTestFolder(SyncProfile syncprofile) {
        setupTestFolder(syncprofile, true);
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
        SyncProfile profile, boolean useRecycleBin)
    {
        final Folder afolder;
        try {
            FolderSettings folderSettings = new FolderSettings(baseDir,
                profile, false, useRecycleBin);
            afolder = getController().getFolderRepository().createFolder(
                foInfo, folderSettings);
        } catch (FolderException e) {
            e.printStackTrace();
            fail("Unable to join controller to " + foInfo + ". " + e.toString());
            return null;
        }
        return afolder;
    }

    private boolean scanned;

    /**
     * Scans a folder and waits for the scan to complete.
     */
    protected synchronized void scanFolder(Folder aFolders) {
        scanned = false;
        folder.getController().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(1000);
        FolderRepositoryListener listener = new FolderRepositoryListener() {
            public void folderCreated(FolderRepositoryEvent e) {
            }

            public void folderRemoved(FolderRepositoryEvent e) {
            }

            public void maintenanceFinished(FolderRepositoryEvent e) {
                scanned = true;
            }

            public void maintenanceStarted(FolderRepositoryEvent e) {
            }

            public boolean fireInEventDispathThread() {
                return false;
            }
        };
        folder.forceScanOnNextMaintenance();
        folder.getController().getFolderRepository()
            .addFolderRepositoryListener(listener);
        folder.getController().getFolderRepository().triggerMaintenance();
        TestHelper.waitForCondition(200, new Condition() {
            public boolean reached() {
                return scanned;
            }
        });
        folder.getController().getFolderRepository()
            .removeFolderRepositoryListener(listener);
        assertTrue("Folder was not scanned as requested", scanned);
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
    
    private void triggerAndWaitForInitialMaitenenace(Controller cont) {
        initalScanOver = false;
        MyFolderRepoListener listener = new MyFolderRepoListener();
        cont.getFolderRepository().addFolderRepositoryListener(listener);
        cont.getFolderRepository().triggerMaintenance();
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return initalScanOver;
            }
        });
        cont.getFolderRepository().removeFolderRepositoryListener(listener);
    }

    private final class MyFolderRepoListener implements
        FolderRepositoryListener
    {
        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            initalScanOver = true;
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
