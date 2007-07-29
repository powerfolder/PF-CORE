
package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class ConnectedNodesTest extends TwoControllerTestCase {

    public void testConnectedNodes() {
        for (int i = 0; i < 20; i++) {
            assertTrue("Connected nodes (@Bart): " + getContollerBart().getNodeManager().getConnectedNodes(), getContollerBart().getNodeManager().getConnectedNodes().isEmpty());
            assertTrue("Connected nodes (@Lisa): " + getContollerLisa().getNodeManager().getConnectedNodes(), getContollerLisa().getNodeManager().getConnectedNodes().isEmpty());
            connectBartAndLisa();
            assertEquals(1, getContollerBart().getNodeManager().getConnectedNodes().size());
            assertEquals(1, getContollerLisa().getNodeManager().getConnectedNodes().size());
            disconnectBartAndLisa();
        }
    }
}
