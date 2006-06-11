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
 * Provides basic testcase-setup with two controllers. Bart and Lisa
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
    private Controller controllerBart;
    private Controller controllerLisa;

    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty("powerfolder.test", "true");

        // Cleanup
        FileUtils.deleteDirectory(new File("build/test/controllerBart"));
        FileUtils.deleteDirectory(new File("build/test/controllerLisa"));
        FileUtils.deleteDirectory(new File(Controller.getMiscFilesLocation(),
            "build"));

        // Copy fresh configs
        FileUtils.copyFile(new File("src/test-resources/ControllerBart.config"),
            new File("build/test/controllerBart/PowerFolder.config"));
        FileUtils.copyFile(new File("src/test-resources/ControllerLisa.config"),
            new File("build/test/controllerLisa/PowerFolder.config"));

        // Start controllers
        System.out.println("Starting controllers...");
        controllerBart = Controller.createController();
        controllerBart.startConfig("build/test/controllerBart/PowerFolder");
        waitForStart(controllerBart);
        controllerBart.getPreferences()
            .putBoolean("createdesktopshortcuts", false);
        controllerLisa = Controller.createController();
        controllerLisa.startConfig("build/test/controllerLisa/PowerFolder");
        waitForStart(controllerLisa);
        controllerLisa.getPreferences()
            .putBoolean("createdesktopshortcuts", false);
        System.out.println("Controllers started");

        // Wait for connection between both controllers
        connect(controllerBart, controllerLisa);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        controllerBart.shutdown();
        controllerLisa.shutdown();

        // Give them time to shut down
        Thread.sleep(1000);
        int i = 0;
        while (controllerBart.isShuttingDown()) {
            i++;
            if (i > 100) {
                System.out.println("Shutdown of Bart failed");
                break;
            }
            Thread.sleep(1000);
        }
        i = 0;
        while (controllerLisa.isShuttingDown()) {
            i++;
            if (i > 100) {
                System.out.println("Shutdown of Lisa failed");
                break;
            }
            Thread.sleep(1000);
        }
        assertFalse(controllerBart.isStarted());
        assertFalse(controllerLisa.isStarted());
    }

    // For subtest ************************************************************

    protected Controller getContollerBart() {
        return controllerBart;
    }

    protected Controller getContollerLisa() {
        return controllerLisa;
    }

    // Helpers ****************************************************************

    protected void makeFriends() {
        Member member2atCon1 = controllerBart.getNodeManager().getNode(
            controllerLisa.getMySelf().getId());
        member2atCon1.setFriend(true);
        Member member1atCon2 = controllerLisa.getNodeManager().getNode(
            controllerBart.getMySelf().getId());
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
            folder1 = getContollerBart().getFolderRepository().createFolder(
                foInfo, baseDir1);

            folder2 = getContollerLisa().getFolderRepository().createFolder(
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
