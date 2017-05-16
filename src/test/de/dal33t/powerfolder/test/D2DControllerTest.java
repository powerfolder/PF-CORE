/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.util.logging.Level;

public class D2DControllerTest extends TwoControllerTestCase
{
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LoggingManager.setConsoleLogging(Level.ALL);
    }

    @Override
    protected void startControllerBart() {
        controllerBart = createControllerBart();
        controllerBart.initTranslation();
        controllerBart.loadConfigFile("build/test/ControllerBart/PowerFolder");

        //ConfigurationEntry.NET_BROADCAST.setValue(controllerBart, true);
        ConfigurationEntry.D2D_ENABLED.setValue(controllerBart, true);
        ConfigurationEntry.D2D_PORT.setValue(controllerBart, 7331);

        controllerBart.start();

        TestHelper.addStartedController(controllerBart);
        waitForStart(controllerBart);
        assertNotNull(controllerBart.getConnectionListener());
    }

    @Override
    protected void startControllerLisa() {
        controllerLisa = createControllerLisa();
        controllerLisa.initTranslation();
        controllerLisa.loadConfigFile("build/test/ControllerLisa/PowerFolder");

        //ConfigurationEntry.NET_BROADCAST.setValue(controllerLisa, true);
        ConfigurationEntry.D2D_ENABLED.setValue(controllerLisa, true);
        ConfigurationEntry.D2D_PORT.setValue(controllerLisa, 7332);

        controllerLisa.start();

        TestHelper.addStartedController(controllerLisa);
        waitForStart(controllerLisa);
        assertNotNull("Connection listener of lisa is null",
            controllerLisa.getConnectionListener());
    }

    public void testD2DConnection() throws Exception {

        try {
            /* Find D2D listener of Bart */
            for(ConnectionListener cl :
                    controllerBart.getAdditionalConnectionListeners()) {

                /* Connect Lisa to Bart via D2D */
                if(cl.getPort() ==
                        ConfigurationEntry.D2D_PORT.getValueInt(controllerBart)) {
                    controllerLisa.connect(cl.getAddress(), true);
                }
            }
        } catch (Exception ignored) {
        }

        Thread.sleep(5000);

        //disconnectBartAndLisa();
    }
}
