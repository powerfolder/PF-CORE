/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.logging.Level;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-3276
 */
public class BroadcastManagerTest extends FiveControllerTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Feature.P2P_REQUIRES_LOGIN_AT_SERVER.disable();

        assertNull(getContollerBart().getBroadcastManager());
        assertNull(getContollerLisa().getBroadcastManager());
        assertNull(getContollerMaggie().getBroadcastManager());
        assertNull(getContollerHomer().getBroadcastManager());
        assertNull(getContollerMarge().getBroadcastManager());
        ConfigurationEntry.NET_BROADCAST_INTERVAL_SECONDS.setValue(getContollerBart(), 1);
        ConfigurationEntry.NET_BROADCAST_INTERVAL_SECONDS.setValue(getContollerLisa(), 1);
        ConfigurationEntry.NET_BROADCAST_INTERVAL_SECONDS.setValue(getContollerMaggie(), 1);

        LoggingManager.setConsoleLogging(Level.FINE);
        getContollerBart().openBroadcastManager();
        TestHelper.waitMilliSeconds(100);
        getContollerLisa().openBroadcastManager();
    }
    @Test
    public void testConnectViaBroadcast() {
        waitForMutualConnection(getContollerBart(), getContollerLisa());
        assertTrue(isDisconnected(getContollerMaggie(), getContollerBart()),
            connectedNodesDump());
        assertTrue(isDisconnected(getContollerMaggie(), getContollerLisa()),
            connectedNodesDump());

        getContollerMaggie().openBroadcastManager();
        waitForMutualConnection(getContollerMaggie(), getContollerBart());
        waitForMutualConnection(getContollerMaggie(), getContollerLisa());
        waitForMutualConnection(getContollerBart(), getContollerLisa());
    }

    private void waitForMutualConnection(final Controller a, final Controller b)
    {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return isConnected(a, b);
            }

            @Override
            public String message() {
                return a.getMySelf().getNick() + " and "
                    + b.getMySelf().getNick()
                    + " did not connect via broadcast. "
                    + connectedNodesDump();
            }
        });
    }

    private static boolean isConnectedAt(Controller from, Controller to) {
        Member node = from.getNodeManager().getNode(to.getMySelf().getId());
        return node != null && node.isCompletelyConnected()
            && from.getNodeManager().getConnectedNodes().contains(node);
    }

    private static boolean isConnected(Controller a, Controller b) {
        return isConnectedAt(a, b) && isConnectedAt(b, a);
    }

    private static boolean isDisconnected(Controller a, Controller b) {
        return !isConnectedAt(a, b) && !isConnectedAt(b, a);
    }

    private String connectedNodesDump() {
        return "Connected nodes @Bart: "
            + getContollerBart().getNodeManager().getConnectedNodes()
            + ", @Lisa: "
            + getContollerLisa().getNodeManager().getConnectedNodes()
            + ", @Maggie: "
            + getContollerMaggie().getNodeManager().getConnectedNodes();
    }
}
