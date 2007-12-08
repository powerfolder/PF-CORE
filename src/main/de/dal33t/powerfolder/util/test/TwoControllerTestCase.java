/* $Id: TwoControllerTestCase.java,v 1.2 2006/04/21 22:58:42 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic testcase-setup with two controllers. Bart and Lisa
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that both controllers
 * are running. There are several utility methods to bring the test into a usual
 * state. To connect both controllers just call
 * <code>{@link #connectBartAndLisa()}</code> in <code>{@link #setUp()}</code>.
 * After both controllers are connected, Lisa runs in normal node, Bart as
 * supernode.
 * <p>
 * It is possible to access both controllers and do manupulating/testing stuff
 * on them.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class TwoControllerTestCase extends TestCase {
    // For the optional test folder.
    protected static final File TESTFOLDER_BASEDIR_BART = new File(TestHelper
        .getTestDir(), "ControllerBart/testFolder");
    protected static final File TESTFOLDER_BASEDIR_LISA = new File(TestHelper
        .getTestDir(), "ControllerLisa/testFolder");

    private Controller controllerBart;
    private Controller controllerLisa;

    // The optional test folders
    private Folder folderBart;
    private Folder folderLisa;

    protected void setUp() throws Exception {
        super.setUp();

        // Default exception logger
        Thread
            .setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                public void uncaughtException(Thread t, Throwable e) {
                    System.err.println("Exception in " + t + ": "
                        + e.toString());
                    e.printStackTrace();
                }
            });

        Logger.setPrefixEnabled(true);
        Feature.disableAll();
        Feature.REMIND_COMPLETED_DOWNLOADS.enable();

        // Cleanup
        TestHelper.cleanTestDir();
        FileUtils.deleteDirectory(new File(Controller.getMiscFilesLocation(),
            "build"));

        // Copy fresh configs
        FileUtils.copyFile(
            new File("src/test-resources/ControllerBart.config"), new File(
                "build/test/ControllerBart/PowerFolder.config"));
        FileUtils.copyFile(
            new File("src/test-resources/ControllerLisa.config"), new File(
                "build/test/ControllerLisa/PowerFolder.config"));

        // Start controllers
        System.out.println("Starting controllers...");
        controllerBart = Controller.createController();
        controllerBart.startConfig("build/test/ControllerBart/PowerFolder");
        waitForStart(controllerBart);
        assertNotNull(controllerBart.getConnectionListener());
        //triggerAndWaitForInitialMaitenenace(controllerBart);
        controllerBart.getPreferences().putBoolean("createdesktopshortcuts",
            false);

        controllerLisa = Controller.createController();
        controllerLisa.startConfig("build/test/ControllerLisa/PowerFolder");
        waitForStart(controllerLisa);
        assertNotNull(controllerLisa.getConnectionListener());
       // triggerAndWaitForInitialMaitenenace(controllerLisa);
        controllerLisa.getPreferences().putBoolean("createdesktopshortcuts",
            false);
        System.out
            .println("-------------- Controllers started -----------------");
    }

    protected void tearDown() throws Exception {
        System.out.println("-------------- tearDown -----------------");
        super.tearDown();
        if (controllerBart.isStarted()) {
            controllerBart.shutdown();
        }
        if (controllerLisa.isStarted()) {
            controllerLisa.shutdown();
        }

        // Give them time to shut down
        Thread.sleep(200);
        int i = 0;
        while (controllerBart.isShuttingDown()) {
            i++;
            if (i > 1000) {
                System.out.println("Shutdown of Bart failed");
                break;
            }
            Thread.sleep(100);
        }
        i = 0;
        while (controllerLisa.isShuttingDown()) {
            i++;
            if (i > 1000) {
                System.out.println("Shutdown of Lisa failed");
                break;
            }
            Thread.sleep(100);
        }
        assertFalse(controllerBart.isStarted());
        assertFalse(controllerLisa.isStarted());

        // add a pause to make sure files can be cleaned before next test.
        TestHelper.waitMilliSeconds(500);
    }

    // For subtest ************************************************************

    protected Controller getContollerBart() {
        return controllerBart;
    }

    protected Controller getContollerLisa() {
        return controllerLisa;
    }

    // Helpers ****************************************************************

    /**
     * Makes lisa and bart friends. Sweet! ;)
     */
    protected void makeFriends() {
        Member lisaAtBart = controllerBart.getNodeManager().getNode(
            controllerLisa.getMySelf().getId());
        if (lisaAtBart == null) {
            lisaAtBart = controllerBart.getNodeManager().addNode(
                controllerLisa.getMySelf().getInfo());
        }
        lisaAtBart.setFriend(true);

        Member bartAtLisa = controllerLisa.getNodeManager().getNode(
            controllerBart.getMySelf().getId());
        if (bartAtLisa == null) {
            bartAtLisa = controllerBart.getNodeManager().addNode(
                controllerBart.getMySelf().getInfo());
        }
        bartAtLisa.setFriend(true);
    }

    /**
     * @see #joinTestFolder(SyncProfile)
     * @return the test folder @ bart. or null if not setup.
     */
    protected Folder getFolderAtBart() {
        return folderBart;
    }

    /**
     * @see #joinTestFolder(SyncProfile)
     * @return the test folder @ lisa. or null if not setup.
     */
    protected Folder getFolderAtLisa() {
        return folderLisa;
    }

    /**
     * Joins both controllers into a testfolder. get these testfolders with
     * <code>getTestFolderBart()</code> and <code>getTestFolderLisa()</code>
     * 
     * @see #getFolderAtBart()
     * @see #getFolderAtLisa()
     */
    protected void joinTestFolder(SyncProfile syncprofile) {
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART,
            TESTFOLDER_BASEDIR_LISA, syncprofile);
        folderBart = getContollerBart().getFolderRepository().getFolder(
            testFolder);
        folderLisa = getContollerLisa().getFolderRepository().getFolder(
            testFolder);
    }

    /**
     * Deletes the test folder (physically) on lisa an bart
     */
    protected void deleteTestFolderContents() {
        try {
            FileUtils.deleteDirectory(TESTFOLDER_BASEDIR_BART);
            FileUtils.deleteDirectory(TESTFOLDER_BASEDIR_LISA);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits for the controller to startup
     * 
     * @param controller
     */
    protected static void waitForStart(final Controller controller) {
        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return controller.isStarted();
            }
        });
    }

    /**
     * Try to connect controllers.
     * 
     * @return true if the lisa and bart are connected.
     */
    protected boolean tryToConnectBartAndLisa() {
        // Wait for connection between both controllers
        try {
            return connect(controllerLisa, controllerBart);
        } catch (ConnectionException e) {
            return false;
        }
    }

    /**
     * Connects both controllers.
     */
    protected void connectBartAndLisa() {
        // Wait for connection between both controllers
        try {
            if (!connect(controllerLisa, controllerBart)) {
                fail("Unable to connect Bart and Lisa");
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertTrue("Bart is not detected as local @ lisa", controllerLisa
            .getNodeManager().getConnectedNodes().iterator().next().isOnLAN());
        assertTrue("Lisa is not detected as local @ bart", controllerBart
            .getNodeManager().getConnectedNodes().iterator().next().isOnLAN());

        // Bart should be supernode
        assertTrue(controllerBart.getMySelf().isSupernode());

        // For whatever.....
        TestHelper.waitMilliSeconds(500);
    }

    /**
     * Disconnectes Lisa and Bart.
     */
    protected void disconnectBartAndLisa() {
        final Member lisaAtBart = getContollerBart().getNodeManager().getNode(
            getContollerLisa().getMySelf().getId());
        lisaAtBart.shutdown();
        final Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());
        bartAtLisa.shutdown();
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return !bartAtLisa.isConnected() && !lisaAtBart.isConnected();
            }
        });
        // Wait to make sure all affected threads (ConnectionHandler) have
        // finished theier work.
        TestHelper.waitMilliSeconds(500);
        System.out.println("Both Controller DISconnected");
    }

    /**
     * Connects and waits for connection of both controllers
     * 
     * @param cont1
     * @param cont2
     * @throws InterruptedException
     * @throws ConnectionException
     */
    private static boolean connect(final Controller cont1,
        final Controller cont2) throws ConnectionException
    {
        Reject.ifTrue(!cont1.isStarted(), "Controller1 not started yet");
        Reject.ifTrue(!cont2.isStarted(), "Controller2 not started yet");

        // Connect
        System.out.println("Connecting controllers...");
        System.out.println("Con to: "
            + cont2.getConnectionListener().getAddress());

        cont1.connect(cont2.getConnectionListener().getAddress());
        try {
            TestHelper.waitForCondition(20, new Condition() {
                public boolean reached() {
                    Member member2atCon1 = cont1.getNodeManager().getNode(
                        cont2.getMySelf().getId());
                    Member member1atCon2 = cont2.getNodeManager().getNode(
                        cont1.getMySelf().getId());
                    boolean connected = member2atCon1 != null
                        && member1atCon2 != null
                        && member2atCon1.isCompleteyConnected()
                        && member1atCon2.isCompleteyConnected();
                    boolean nodeManagersOK = cont1.getNodeManager()
                        .getConnectedNodes().contains(member2atCon1)
                        && cont2.getNodeManager().getConnectedNodes().contains(
                            member1atCon2);
                    return connected && nodeManagersOK;
                }
            });
        } catch (RuntimeException e) {
            System.out.println("Unable to connect Controllers");
            return false;
        }
        System.out.println("Both Controller connected");
        return true;
    }

    /**
     * Let both controller join the specified folder.
     * <p>
     * After the method is invoked, it is ensured that folders on both
     * controllers have two members. Otherwise the test will fail.
     * <p>
     * Sets the syncprofile to <code>MANUAL_DOWNLOAD</code>
     * 
     * @param foInfo
     *            the folder to join
     * @param bartFolderDir
     *            the local base dir for folder at bart
     * @param lisaFolderDir
     *            the local base dir for folder at lisa
     */
    protected void joinFolder(FolderInfo foInfo, File bartFolderDir,
        File lisaFolderDir)
    {
        joinFolder(foInfo, bartFolderDir, lisaFolderDir,
            SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * Let both controller join the specified folder.
     * <p>
     * After the method is invoked, it is ensured that folders on both
     * controllers have two members. Otherwise the test will fail.
     * 
     * @param foInfo
     *            the folder to join
     * @param baseDir1
     *            the local base dir for the first controller
     * @param baseDir2
     *            the local base dir for the second controller
     * @param profile
     *            the profile to use
     */
    protected void joinFolder(FolderInfo foInfo, File baseDir1, File baseDir2,
        SyncProfile profile)
    {
        final Folder folder1;
        final Folder folder2;
        try {
            FolderSettings folderSettings1 = new FolderSettings(baseDir1,
                profile, false, true);
            folder1 = getContollerBart().getFolderRepository().createFolder(
                foInfo, folderSettings1);

            FolderSettings folderSettings2 = new FolderSettings(baseDir2,
                profile, false, true);
            folder2 = getContollerLisa().getFolderRepository().createFolder(
                foInfo, folderSettings2);
        } catch (FolderException e) {
            e.printStackTrace();
            fail("Unable to join both controller to " + foInfo + ". "
                + e.toString());
            return;
        }

        try {
            // Give them time to join
            TestHelper.waitForCondition(30, new Condition() {
                public boolean reached() {
                    return folder1.getMembersCount() >= 2
                        && folder2.getMembersCount() >= 2;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Bart: "
                + folder1.getMembersCount() + ", Lisa: "
                + folder2.getMembersCount() + ". Folder: " + foInfo + " id: "
                + foInfo.id);
        }
    }

    /**
     * Scans a folder and waits for the scan to complete.
     */
    protected synchronized void scanFolder(Folder folder) {
        TestHelper.scanFolder(folder);
    }

    /**
     * Tests if the diskfile matches the fileinfo. Checks name, lenght/size,
     * modification date and the deletion status.
     * 
     * @param diskFile
     *            the diskfile to compare
     * @param fInfo
     *            the fileinfo
     * @param controller
     *            the controller to use.
     */
    protected void assertFileMatch(File diskFile, FileInfo fInfo,
        Controller controller)
    {
        boolean nameMatch = diskFile.getName().equals(fInfo.getFilenameOnly());
        boolean sizeMatch = diskFile.length() == fInfo.getSize();
        boolean fileObjectEquals = diskFile.equals(fInfo.getDiskFile(controller
            .getFolderRepository()));
        boolean deleteStatusMatch = diskFile.exists() == !fInfo.isDeleted();
        boolean lastModifiedMatch = diskFile.lastModified() == fInfo
            .getModifiedDate().getTime();

        // Skip last modification test when diskfile is deleted.
        boolean matches = !diskFile.isDirectory() && nameMatch && sizeMatch
            && (!diskFile.exists() || lastModifiedMatch) && deleteStatusMatch
            && fileObjectEquals;

        assertTrue("FileInfo does not match physical file. \nFileInfo:\n "
            + fInfo.toDetailString() + "\nFile:\n " + diskFile.getName()
            + ", size: " + Format.formatBytes(diskFile.length())
            + ", lastModified: " + new Date(diskFile.lastModified()) + " ("
            + diskFile.lastModified() + ")" + "\n\nWhat matches?:\nName: "
            + nameMatch + "\nSize: " + sizeMatch + "\nlastModifiedMatch: "
            + lastModifiedMatch + "\ndeleteStatus: " + deleteStatusMatch
            + "\nFileObjectEquals: " + fileObjectEquals, matches);
    }

    private boolean initalScanOver = false;

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
