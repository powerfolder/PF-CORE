package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
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
public class ConnectNodesTest extends FiveControllerTestCase {

    public void testConnectedNodes() {
        for (int i = 0; i < 50; i++) {
            assertTrue("Connected nodes @Bart: "
                + getContollerBart().getNodeManager().getConnectedNodes(),
                getContollerBart().getNodeManager().getConnectedNodes()
                    .isEmpty());
            assertTrue("Connected nodes @Lisa: "
                + getContollerLisa().getNodeManager().getConnectedNodes(),
                getContollerLisa().getNodeManager().getConnectedNodes()
                    .isEmpty());
            boolean connectOk = tryToConnectSimpsons();
            assertEquals("Connected nodes at bart: "
                + getContollerBart().getNodeManager().getConnectedNodes(), 4,
                getContollerBart().getNodeManager().getConnectedNodes().size());
            assertEquals("Connected nodes at lisa: "
                + getContollerLisa().getNodeManager().getConnectedNodes(), 4,
                getContollerLisa().getNodeManager().getConnectedNodes().size());
            assertTrue("Connection of Simpsons failed", connectOk);
            getContollerBart().getNodeManager().shutdown();
            getContollerBart().getNodeManager().start();
            getContollerLisa().getNodeManager().shutdown();
            getContollerLisa().getNodeManager().start();
        }
    }

    public void testAutoReconnectAfterDisconnect() {
        // Reconnect manager has to be started therefore!
        getContollerHomer().getReconnectManager().start();

        connectSimpsons();
        assertEquals(4, getContollerBart().getNodeManager().getConnectedNodes()
            .size());
        assertEquals(4, getContollerLisa().getNodeManager().getConnectedNodes()
            .size());

        final Member lisaAtHomer = getContollerHomer().getNodeManager()
            .getNode(getContollerLisa().getMySelf().getInfo());
        assertTrue(lisaAtHomer.isCompleteyConnected());
        lisaAtHomer.shutdown();

        // No RECONNECT should happen!
        // Both are not friends so no connect!
        TestHelper.waitMilliSeconds(10000);
        assertFalse(lisaAtHomer.isCompleteyConnected());

        // Make friend
        lisaAtHomer.setFriend(true, "");

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Lisa has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue();
            }

            public boolean reached() {
                return lisaAtHomer.isCompleteyConnected();
            }
        });

        // Again shutdown
        lisaAtHomer.shutdown();

        // RECONNECT should happen!
        // Both are friends so connect!
        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Lisa has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue();
            }

            public boolean reached() {
                return lisaAtHomer.isCompleteyConnected();
            }
        });
    }

    public void testFriendAutoConnect() {
        // Reconnect manager has to be started therefore!
        getContollerHomer().getReconnectManager().start();

        final Member margeAtHomer = getContollerMarge().getMySelf().getInfo()
            .getNode(getContollerHomer(), true);
        assertFalse(margeAtHomer.isCompleteyConnected());

        // Make friend
        margeAtHomer.setFriend(true, "");

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Marge has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return margeAtHomer.isCompleteyConnected();
            }
        });

        // Again shutdown
        margeAtHomer.shutdown();

        // RECONNECT should happen!
        // Both are friends so connect!
        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Marge has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return margeAtHomer.isCompleteyConnected();
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

    public void noTestPublicInfrastructureConnect() {
        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerBart(), "");
        for (int i = 0; i < 50; i++) {
            try {
                final Member battlestar = getContollerBart().connect(
                    TestHelper.INFRASTRUCTURE_CONNECT_STRING);
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

                final Member onlineStorage = getContollerBart().connect(
                    Constants.ONLINE_STORAGE_ADDRESS);
                TestHelper.waitForCondition(10, new ConditionWithMessage() {
                    public String message() {
                        return "Unable to connect to OnlineStorage";
                    }

                    public boolean reached() {
                        return onlineStorage.isCompleteyConnected();
                    }
                });
                assertTrue(onlineStorage.isCompleteyConnected());
                onlineStorage.shutdown();
            } catch (ConnectionException e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }

    }

}
