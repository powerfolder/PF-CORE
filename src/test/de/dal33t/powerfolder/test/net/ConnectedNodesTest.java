package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.InvalidIdentityException;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test the reconnection behaviour.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ConnectedNodesTest extends FiveControllerTestCase {

    public void testConnectedNodes() {
        for (int i = 0; i < 50; i++) {
            assertTrue("Connected nodes (@Bart): "
                + getContollerBart().getNodeManager().getConnectedNodes(),
                getContollerBart().getNodeManager().getConnectedNodes()
                    .isEmpty());
            assertTrue("Connected nodes (@Lisa): "
                + getContollerLisa().getNodeManager().getConnectedNodes(),
                getContollerLisa().getNodeManager().getConnectedNodes()
                    .isEmpty());
            connectSimpsons();
            assertEquals(4, getContollerBart().getNodeManager()
                .getConnectedNodes().size());
            assertEquals(4, getContollerLisa().getNodeManager()
                .getConnectedNodes().size());
            getContollerBart().getNodeManager().shutdown();
            getContollerBart().getNodeManager().start();
            getContollerLisa().getNodeManager().shutdown();
            getContollerLisa().getNodeManager().start();
        }
    }

    public void testAutoReconnectAfterDisconnect() {
        connectSimpsons();
        assertEquals(4, getContollerBart().getNodeManager().getConnectedNodes()
            .size());
        assertEquals(4, getContollerLisa().getNodeManager().getConnectedNodes()
            .size());

        final Member bartAtHomer = getContollerHomer().getNodeManager()
            .getNode(getContollerBart().getMySelf().getInfo());
        assertTrue(bartAtHomer.isCompleteyConnected());
        bartAtHomer.shutdown();

        // No RECONNECT should happen!
        // Both are not friends so no connect!
        TestHelper.waitMilliSeconds(5000);
        assertFalse(bartAtHomer.isCompleteyConnected());

        // Make friend
        bartAtHomer.setFriend(true, "");

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Bart has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return bartAtHomer.isCompleteyConnected();
            }
        });

        // Again shutdown
        bartAtHomer.shutdown();

        // RECONNECT should happen!
        // Both are friends so connect!
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Bart has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return bartAtHomer.isCompleteyConnected();
            }
        });
    }

    public void testFriendAutoConnect() {
        final Member bartAtHomer = getContollerBart().getMySelf().getInfo()
            .getNode(getContollerHomer(), true);
        assertFalse(bartAtHomer.isCompleteyConnected());

        // Make friend
        bartAtHomer.setFriend(true, "");

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Bart has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return bartAtHomer.isCompleteyConnected();
            }
        });

        // Again shutdown
        bartAtHomer.shutdown();

        // RECONNECT should happen!
        // Both are friends so connect!
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Bart has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return bartAtHomer.isCompleteyConnected();
            }
        });
    }

    public void testNonConnectWrongIdentity() {
        final Member bartAtHomer = getContollerBart().getMySelf().getInfo()
            .getNode(getContollerHomer(), true);
        final Member lisaAtHomer = getContollerLisa().getMySelf().getInfo()
            .getNode(getContollerHomer(), true);

        // Connect to bart, but it is actual lisa!
        bartAtHomer.getInfo().setConnectAddress(
            lisaAtHomer.getReconnectAddress());

        // Trigger connect
        bartAtHomer.setFriend(true, "");
        try {
            assertFalse(bartAtHomer.reconnect());
            fail("Should not be able to connect. Identity is lisas!");
        } catch (InvalidIdentityException e) {
            // OK!
        }
    }
    
    public void xtestPublicInfrastructureConnect() {
        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        for (int i = 0; i < 500; i++) {
            try {
                getContollerBart().connect("server.powerfolder.com");
                final Member battlestar = getContollerBart().getNodeManager()
                    .getNode("INFRASTRUCTURE01");
                TestHelper.waitForCondition(10, new ConditionWithMessage() {
                    public String message() {
                        return "Unable to connect to battlestar";
                    }

                    public boolean reached() {
                        return battlestar.isCompleteyConnected();
                    }
                });
                assertTrue(battlestar.isCompleteyConnected());
                battlestar.shutdown();
            } catch (ConnectionException e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }

    }
}
