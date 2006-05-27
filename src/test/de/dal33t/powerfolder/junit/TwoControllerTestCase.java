/* $Id: TwoControllerTestCase.java,v 1.2 2006/04/21 22:58:42 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.junit;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic testcase-setup with two controllers.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that both controllers
 * are running and have connection to each other
 * <p>
 * You can access both controllers and do manupulating/testing stuff on them
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
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
        controller1.getPreferences().putBoolean("createdesktopshortcuts", false);
        controller2 = Controller.createController();
        controller2.startConfig("build/test/Controller2/PowerFolder");
        waitForStart(controller2);
        controller2.getPreferences().putBoolean("createdesktopshortcuts", false);
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
        int i=0;
        while (controller1.isShuttingDown()) {
            i++;
            if (i>100) {
                System.out.println("shutdown of controller 1 failed");
                break;
            }
            Thread.sleep(1000);
        }
        i = 0;
        while (controller2.isShuttingDown()) {
            i++;
            if (i>100) {
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

    /**
     * Waits for the controller to startup
     * 
     * @param controller
     * @throws InterruptedException
     */
    protected void waitForStart(Controller controller)
        throws InterruptedException
    {
        int i = 0;
        while (!controller.isStarted()) {
            i++;
            Thread.sleep(100);
            if (i > 100) {
                fail("Unable to start controller");
            }
        }
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
        cont1.connect(cont2.getConnectionListener().getLocalAddress());

        boolean connected = false;
        int i = 0;
        do {
            i++;
            Member testNode1 = cont1.getMySelf().getInfo().getNode(cont2);
            connected = testNode1 != null && testNode1.isCompleteyConnected();
            Thread.sleep(1000);
            if (i > 100) {
                fail("Unable to connect nodes");
            }
        } while (!connected);
        System.out.println("Both Controller connected");
    }
}
