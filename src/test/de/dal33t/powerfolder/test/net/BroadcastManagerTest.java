/*
 * Copyright 2004 - 2019 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.junit.Test;

/**
 * PFS-3276
 */
public class BroadcastManagerTest extends FiveControllerTestCase {

    public void setUp() throws Exception {
        super.setUp();
        Feature.P2P_REQUIRES_LOGIN_AT_SERVER.disable();

        assertNull(getContollerBart().getBroadcastManager());
        assertNull(getContollerLisa().getBroadcastManager());
        assertNull(getContollerMaggie().getBroadcastManager());
        assertNull(getContollerHomer().getBroadcastManager());
        assertNull(getContollerMarge().getBroadcastManager());
        getContollerBart().openBroadcastManager();
        getContollerLisa().openBroadcastManager();
    }
    @Test
    public void testConnectViaBroadcast() {
        TestHelper.waitForCondition(5, () -> getContollerBart().getNodeManager().countConnectedNodes() == 1);
        TestHelper.waitForCondition(5, () -> getContollerLisa().getNodeManager().countConnectedNodes() == 1);
        assertEquals(0, getContollerMaggie().getNodeManager().countConnectedNodes());

        getContollerMaggie().openBroadcastManager();
        TestHelper.waitForCondition(5, () -> getContollerBart().getNodeManager().countConnectedNodes() == 2);
        TestHelper.waitForCondition(5, () -> getContollerLisa().getNodeManager().countConnectedNodes() == 2);
        TestHelper.waitForCondition(5, () -> getContollerMaggie().getNodeManager().countConnectedNodes() == 2);
    }
}
