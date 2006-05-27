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

/**
 * Provides basic testcase-setup with a controller.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that controller
 * is running 
 * <p>
 * You can access the controller and do manupulating/testing stuff on it
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class ControllerTestCase extends TestCase {
    private Controller controller;
    
    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty("powerfolder.test", "true");
        
        // Cleanup
        FileUtils.deleteDirectory(new File("build/test/controller1"));
    
        // Copy fresh configs
        FileUtils.copyFile(new File("src/test-resources/Controller1.config"),
            new File("build/test/controller1/PowerFolder.config"));
    
        // Start controllers
        System.out.println("Starting controller...");
        controller = Controller.createController();
        controller.startConfig("build/test/Controller1/PowerFolder");
        waitForStart(controller);
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
            if (i>100) {
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
}
