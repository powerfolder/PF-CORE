/* $Id: TwoControllerTestCase.java,v 1.2 2006/04/21 22:58:42 totmacherr Exp $
 */
package de.dal33t.powerfolder.test;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.test.TestHelper.Task;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic testcase-setup with two controllers.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that both controllers
 * are running and have connection to each other
 * <p>
 * You can access both controllers and do manupulating/testing stuff on them
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class TwoControllerTestCase extends TestCase {
    private Controller controller1;
    private Controller controller2;

    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty("powerfolder.test", "true");

        // Cleanup
        FileUtils.deleteDirectory(new File("build/test/controller1"));
        FileUtils.deleteDirectory(new File("build/test/controller2"));
        FileUtils.deleteDirectory(new File(Controller.getMiscFilesLocation(),
            "build"));

        // Copy fresh configs
        FileUtils.copyFile(new File("src/test-resources/Controller1.config"),
            new File("build/test/controller1/PowerFolder.config"));
        FileUtils.copyFile(new File("src/test-resources/Controller2.config"),
            new File("build/test/controller2/PowerFolder.config"));

        // Start controllers
        System.out.println("Starting controllers...");
        controller1 = Controller.createController();
        controller1.startConfig("build/test/Controller1/PowerFolder");
        waitForStart(controller1);
        controller1.getPreferences()
            .putBoolean("createdesktopshortcuts", false);
        controller2 = Controller.createController();
        controller2.startConfig("build/test/Controller2/PowerFolder");
        waitForStart(controller2);
        controller2.getPreferences()
            .putBoolean("createdesktopshortcuts", false);
        System.out.println("Controllers started");

        // Wait for connection between both controllers
        connect(controller1, controller2);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        controller1.shutdown();
        controller2.shutdown();

        // Give them time to shut down
        Thread.sleep(1000);
        int i = 0;
        while (controller1.isShuttingDown()) {
            i++;
            if (i > 100) {
                System.out.println("shutdown of controller 1 failed");
                break;
            }
            Thread.sleep(1000);
        }
        i = 0;
        while (controller2.isShuttingDown()) {
            i++;
            if (i > 100) {
                System.out.println("shutdown of controller 2 failed");
                break;
            }
            Thread.sleep(1000);
        }
        assertFalse(controller1.isStarted());
        assertFalse(controller2.isStarted());
    }

    // For subtest ************************************************************

    protected Controller getContoller1() {
        return controller1;
    }

    protected String getController1NodeID() {
        return controller1.getMySelf().getId();
    }

    protected Controller getContoller2() {
        return controller2;
    }

    protected String getController2NodeID() {
        return controller2.getMySelf().getId();
    }

    // Helpers ****************************************************************

    protected void makeFriends() {
        Member member2atCon1 = controller1.getNodeManager().getNode(
            controller2.getMySelf().getId());
        member2atCon1.setFriend(true);
        Member member1atCon2 = controller2.getNodeManager().getNode(
            controller1.getMySelf().getId());
        member1atCon2.setFriend(true);

    }

    /**
     * Waits for the controller to startup
     * 
     * @param controller
     */
    protected void waitForStart(final Controller controller) {
        boolean success = TestHelper.waitForTask(30, new Task() {
            public boolean completed() {
                return controller.isStarted();
            }
        });
        assertTrue("Unable to start controller", success);
    }

    /**
     * Connects and waits for connection of both controllers
     * 
     * @param cont1
     * @param cont2
     * @throws InterruptedException
     * @throws ConnectionException
     */
    protected void connect(Controller cont1, Controller cont2)
        throws InterruptedException, ConnectionException
    {
        Reject.ifTrue(!cont1.isStarted(), "Controller1 not started yet");
        Reject.ifTrue(!cont2.isStarted(), "Controller2 not started yet");

        // Connect
        System.out.println("Connecting controllers...");
        System.out.println("Con to: "
            + cont2.getConnectionListener().getLocalAddress());

        boolean connected = false;
        int i = 0;
        do {
            if (i % 20 == 0) {
                cont1.connect(cont2.getConnectionListener().getLocalAddress());
            }
            if (i % 20 == 10) {
                cont2.connect(cont1.getConnectionListener().getLocalAddress());
            }

            Member member2atCon1 = cont1.getNodeManager().getNode(
                cont2.getMySelf().getId());
            Member member1atCon2 = cont2.getNodeManager().getNode(
                cont1.getMySelf().getId());
            if (member2atCon1 != null && member1atCon2 != null) {
                if (member2atCon1.isCompleteyConnected()
                    && member1atCon2.isCompleteyConnected())
                {
                    break;
                }
            }
            i++;
            // Member testNode1 = cont1.getMySelf().getInfo().getNode(cont2);
            // connected = testNode1 != null &&
            // testNode1.isCompleteyConnected();
            Thread.sleep(100);
            if (i > 50) {
                fail("Unable to connect nodes");
            }
        } while (!connected);
        System.out.println("Both Controller connected");
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
     */
    protected void joinFolder(FolderInfo foInfo, File baseDir1, File baseDir2) {
        final Folder folder1;
        final Folder folder2;
        try {
            folder1 = getContoller1().getFolderRepository().createFolder(
                foInfo, baseDir1);

            folder2 = getContoller2().getFolderRepository().createFolder(
                foInfo, baseDir2);
        } catch (FolderException e) {
            e.printStackTrace();
            fail("Unable to join both controller to " + foInfo + ". "
                + e.toString());
            return;
        }

        // Give them time to join
        boolean success = TestHelper.waitForTask(30, new Task() {
            public boolean completed() {
                return folder1.getMembersCount() >= 2
                    && folder2.getMembersCount() >= 2;
            }
        });

        assertTrue("Unable to join both controller to " + foInfo + ".",
            success);
    }
}
