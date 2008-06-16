/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
        for (int i = 0; i < 250; i++) {
            boolean connectOk = tryToConnectSimpsons();

            assertEquals("Connected nodes @Homer: "
                + getContollerHomer().getNodeManager().getConnectedNodes(), 4,
                getContollerHomer().getNodeManager().getConnectedNodes().size());
            assertEquals("Connected nodes @Bart: "
                + getContollerBart().getNodeManager().getConnectedNodes(), 4,
                getContollerBart().getNodeManager().getConnectedNodes().size());
            assertEquals("Connected nodes @Marge: "
                + getContollerMarge().getNodeManager().getConnectedNodes(), 4,
                getContollerMarge().getNodeManager().getConnectedNodes().size());
            assertEquals("Connected nodes @Lisa: "
                + getContollerLisa().getNodeManager().getConnectedNodes(), 4,
                getContollerLisa().getNodeManager().getConnectedNodes().size());
            assertEquals("Connected nodes @Maggie: "
                + getContollerMaggie().getNodeManager().getConnectedNodes(), 4,
                getContollerMaggie().getNodeManager().getConnectedNodes()
                    .size());
            assertTrue("Connection of Simpsons failed", connectOk);

            getContollerBart().getNodeManager().shutdown();
            getContollerLisa().getNodeManager().shutdown();

            // Wait for disconnects
            TestHelper.waitForCondition(10, new ConditionWithMessage() {
                public boolean reached() {
                    return getContollerHomer().getNodeManager()
                        .getConnectedNodes().size() == 2;
                }

                public String message() {
                    return "Connected nodes @Homer: "
                        + getContollerHomer().getNodeManager()
                            .getConnectedNodes();
                }
            });
            TestHelper.waitForCondition(10, new ConditionWithMessage() {
                public boolean reached() {
                    return getContollerBart().getNodeManager()
                        .getConnectedNodes().size() == 0;
                }

                public String message() {
                    return "Connected nodes @Bart: "
                        + getContollerBart().getNodeManager()
                            .getConnectedNodes();
                }
            });
            TestHelper.waitForCondition(10, new ConditionWithMessage() {
                public boolean reached() {
                    return getContollerMarge().getNodeManager()
                        .getConnectedNodes().size() == 2;
                }

                public String message() {
                    return "Connected nodes @Marge: "
                        + getContollerMarge().getNodeManager()
                            .getConnectedNodes();
                }
            });
            TestHelper.waitForCondition(10, new ConditionWithMessage() {
                public boolean reached() {
                    return getContollerLisa().getNodeManager()
                        .getConnectedNodes().size() == 0;
                }

                public String message() {
                    return "Connected nodes @Lisa: "
                        + getContollerLisa().getNodeManager()
                            .getConnectedNodes();
                }
            });
            TestHelper.waitForCondition(10, new ConditionWithMessage() {
                public boolean reached() {
                    return getContollerMaggie().getNodeManager()
                        .getConnectedNodes().size() == 2;
                }

                public String message() {
                    return "Connected nodes @Maggie: "
                        + getContollerMaggie().getNodeManager()
                            .getConnectedNodes();
                }
            });

            getContollerBart().getNodeManager().start();
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
