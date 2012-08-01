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
 * $Id$
 */
package de.dal33t.powerfolder.util.test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PowerFolder;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.logging.LoggingManager;

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
 * It is possible to access both controllers and do manipulating/testing stuff
 * on them.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class TwoControllerTestCase extends TestCase {
    // For the optional test folder.
    public static final File TESTFOLDER_BASEDIR_BART = new File(TestHelper
        .getTestDir().getAbsoluteFile(), "ControllerBart/testFolder.pfzip");
    public static final File TESTFOLDER_BASEDIR_LISA = new File(TestHelper
        .getTestDir().getAbsoluteFile(), "ControllerLisa/testFolder");

    private Controller controllerBart;
    private Controller controllerLisa;

    // The optional test folder
    private FolderInfo testFolder;

    @Override
    protected void setUp() throws Exception {
        System.setProperty("user.home",
            new File("build/test/home").getCanonicalPath());
        Loggable.setLogNickPrefix(true);
        super.setUp();

        if ((getContollerBart() != null && getContollerBart().isStarted())
            || (getContollerLisa() != null && getContollerLisa().isStarted()))
        {
            // Ensure shutdown of controller. Maybe tearDown was not called
            // because of previous failing test.
            stopControllers();
        }
        // Also for cleaning up failed tests, where tearDown has not been run.
        TestHelper.shutdownStartedController();

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

        Feature.setupForTests();

        // Cleanup
        TestHelper.cleanTestDir();
        FileUtils.recursiveDelete(new File(Controller.getMiscFilesLocation(),
            "build"));
        cleanPreferences(Preferences.userNodeForPackage(PowerFolder.class)
            .node("build/test/ControllerBart/PowerFolder"));
        cleanPreferences(Preferences.userNodeForPackage(PowerFolder.class)
            .node("build/test/ControllerLisa/PowerFolder"));

        // Copy fresh configs
        FileUtils.copyFile(
            new File("src/test-resources/ControllerBart.config"), new File(
                "build/test/ControllerBart/PowerFolder.config"));
        FileUtils.copyFile(
            new File("src/test-resources/ControllerLisa.config"), new File(
                "build/test/ControllerLisa/PowerFolder.config"));

        // Start controllers
        System.out.println("Starting controllers...");
        startControllerBart();
        startControllerLisa();

        System.out
            .println("-------------- Controllers started -----------------");
    }

    @Override
    protected void tearDown() throws Exception {
        LoggingManager.setConsoleLogging(Level.OFF);
        System.out.println("-------------- tearDown -----------------");
        super.tearDown();
        stopControllers();
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
     * Allows overriding controller creation
     */
    protected Controller createControllerLisa() {
        return Controller.createController();
    }

    /**
     * Allows overriding controller creation
     */
    protected Controller createControllerBart() {
        return Controller.createController();
    }

    protected void startControllerBart() {
        controllerBart = createControllerBart();
        controllerBart.startConfig("build/test/ControllerBart/PowerFolder");
        TestHelper.addStartedController(controllerBart);
        waitForStart(controllerBart);
        assertNotNull(controllerBart.getConnectionListener());
    }

    protected void startControllerLisa() {
        controllerLisa = createControllerLisa();
        controllerLisa.startConfig("build/test/ControllerLisa/PowerFolder");
        TestHelper.addStartedController(controllerLisa);
        waitForStart(controllerLisa);
        assertNotNull("Connection listener of lisa is null",
            controllerLisa.getConnectionListener());
    }

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
        lisaAtBart.setFriend(true, null);

        Member bartAtLisa = controllerLisa.getNodeManager().getNode(
            controllerBart.getMySelf().getId());
        if (bartAtLisa == null) {
            bartAtLisa = controllerBart.getNodeManager().addNode(
                controllerBart.getMySelf().getInfo());
        }
        bartAtLisa.setFriend(true, null);
    }

    /**
     * @see #joinTestFolder(SyncProfile)
     * @return the test folder @ bart. or null if not setup.
     */
    protected Folder getFolderAtBart() {
        return testFolder.getFolder(getContollerBart());
    }

    /**
     * @see #joinTestFolder(SyncProfile)
     * @return the test folder @ lisa. or null if not setup.
     */
    protected Folder getFolderAtLisa() {
        return testFolder.getFolder(getContollerLisa());
    }

    /**
     * Joins both controllers into a testfolder. get these testfolders with
     * <code>getTestFolderBart()</code> and <code>getTestFolderLisa()</code>
     * 
     * @see #getFolderAtBart()
     * @see #getFolderAtLisa()
     */
    protected void joinTestFolder(SyncProfile syncprofile) {
        testFolder = new FolderInfo("testFolder", UUID.randomUUID().toString());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART,
            TESTFOLDER_BASEDIR_LISA, syncprofile);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        getFolderAtLisa().getFolderWatcher().setIngoreAll(true);
    }

    /**
     * Deletes the test folder (physically) on lisa an bart
     */
    protected void deleteTestFolderContents() {
        try {
            FileUtils.recursiveDelete(TESTFOLDER_BASEDIR_BART);
            FileUtils.recursiveDelete(TESTFOLDER_BASEDIR_LISA);
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
        connectBartAndLisa(false);
    }

    /**
     * Connects both controllers and optionally logs in lisa at bart.
     */
    protected void connectBartAndLisa(boolean loginLisa) {
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

        if (loginLisa) {
            Member bartAtLisa = controllerBart.getMySelf().getInfo()
                .getNode(controllerLisa, true);
            ServerClient client = getContollerLisa().getOSClient();
            client.setServer(bartAtLisa, true);
            client.getAccountService().register("lisa", "password", false,
                null, null, false);
            client.login("lisa", "password".toCharArray());
        }

        // Bart should be supernode
        assertTrue(controllerBart.getMySelf().isSupernode());
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
        System.out.println("Both Controllers disconnected");
    }

    private void stopControllers() throws InterruptedException {
        if (controllerBart.isStarted()) {
            controllerBart.shutdown();
            TestHelper.removeStartedController(controllerBart);
        }
        if (controllerLisa.isStarted()) {
            controllerLisa.shutdown();
            TestHelper.removeStartedController(controllerLisa);
        }

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

        try {
            cont1.connect(cont2.getConnectionListener().getAddress());
        } catch (Exception e) {
            // Try harder.
            cont1.connect(cont2.getConnectionListener().getAddress());
        }
        try {
            TestHelper.waitForCondition(20, new Condition() {
                public boolean reached() {
                    Member member2atCon1 = cont1.getNodeManager().getNode(
                        cont2.getMySelf().getId());
                    Member member1atCon2 = cont2.getNodeManager().getNode(
                        cont1.getMySelf().getId());
                    boolean connected = member2atCon1 != null
                        && member1atCon2 != null
                        && member2atCon1.isCompletelyConnected()
                        && member1atCon2.isCompletelyConnected();
                    boolean nodeManagersOK = cont1.getNodeManager()
                        .getConnectedNodes().contains(member2atCon1)
                        && cont2.getNodeManager().getConnectedNodes()
                            .contains(member1atCon2);
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
     * Sets the transfer mode to {@link SyncProfile#HOST_FILES}
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
        joinFolder(foInfo, bartFolderDir, lisaFolderDir, SyncProfile.HOST_FILES);
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
    protected void joinTestFolder(File baseDir1, File baseDir2,
        SyncProfile profile)
    {
        testFolder = new FolderInfo("testFolder", UUID.randomUUID().toString());
        joinFolder(testFolder, baseDir1, baseDir2, profile);
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
        final Folder meta1;
        final Folder folder2;
        final Folder meta2;
        FolderSettings folderSettings1 = new FolderSettings(baseDir1, profile,
            false, ArchiveMode.FULL_BACKUP, 5);
        folder1 = getContollerBart().getFolderRepository().createFolder(foInfo,
            folderSettings1);
        meta1 = getContollerBart().getFolderRepository()
            .getMetaFolderForParent(folder1.getInfo());

        FolderSettings folderSettings2 = new FolderSettings(baseDir2, profile,
            false, ArchiveMode.FULL_BACKUP, 5);
        folder2 = getContollerLisa().getFolderRepository().createFolder(foInfo,
            folderSettings2);
        if (folder1.isDeviceDisconnected() || folder2.isDeviceDisconnected()) {
            fail("Unable to join both controller to " + foInfo);
        }

        meta2 = getContollerLisa().getFolderRepository()
            .getMetaFolderForParent(folder2.getInfo());

        try {
            // Give them time to join
            TestHelper.waitForCondition(30, new Condition() {
                public boolean reached() {
                    return folder1.getMembersCount() >= 2
                        && folder2.getMembersCount() >= 2
                        && (meta1 == null || meta1.getMembersCount() >= 2)
                        && (meta2 == null || meta2.getMembersCount() >= 2);
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
        boolean sizeMatch = fInfo.isDeleted()
            || diskFile.length() == fInfo.getSize();
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

    private void cleanPreferences(Preferences p) {
        try {
            String[] childs = p.childrenNames();
            for (String child : childs) {
                cleanPreferences(p.node(child));
            }
            String[] keys = p.keys();
            for (String key : keys) {
                p.remove(key);
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(
                "Unable to cleanup the preferencs: " + e, e);
        }

    }
}
