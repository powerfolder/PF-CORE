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
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.util.logging.Level;

/**
 * Test the reconnection behaviour.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ConnectNodesTest extends FiveControllerTestCase {

    public void testConnectedNodes() {
        int nTries = 10;
        for (int i = 0; i < nTries; i++) {
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

        connectSimpsons();
        assertEquals(4, getContollerBart().getNodeManager().getConnectedNodes()
            .size());
        assertEquals(4, getContollerLisa().getNodeManager().getConnectedNodes()
            .size());

        // Start Reconnector. Currently empty.
        TestHelper.waitMilliSeconds(500);
        getContollerHomer().getReconnectManager().start();

        final Member lisaAtHomer = getContollerHomer().getNodeManager()
            .getNode(getContollerLisa().getMySelf().getInfo());
        final Member homerAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerHomer().getMySelf().getInfo());
        assertTrue("Lisa not completely connected at Homer", lisaAtHomer
            .isCompletelyConnected());
        lisaAtHomer.shutdown();

        // No RECONNECT should happen!
        // Both are not friends so no connect!
        TestHelper.waitMilliSeconds(5000);
        assertFalse("Lisa still connected at Homer", lisaAtHomer
            .isCompletelyConnected());
        assertFalse("Homer still connected at Lisa", homerAtLisa
            .isCompletelyConnected());

        // Make friend
        lisaAtHomer.setFriend(true, "");

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Lisa has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue();
            }

            public boolean reached() {
                return lisaAtHomer.isCompletelyConnected();
            }
        });
        // W8 until reconnecting has stopped.
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return !lisaAtHomer.isConnecting();
            }
        });

        // Again shutdown
        TestHelper.waitForCondition(10, new ConditionWithMessage() {

            public boolean reached() {
                lisaAtHomer.shutdown();
                return !lisaAtHomer.isCompletelyConnected()
                    && !homerAtLisa.isCompletelyConnected();
            }

            public String message() {
                return "Lisa at homer is still connected? "
                    + lisaAtHomer.isCompletelyConnected()
                    + ". Homer at lisa is still connected? "
                    + homerAtLisa.isCompletelyConnected();
            }
        });

        System.out.println("Waiting for reconnect...");

        // RECONNECT should happen!
        // Both are friends so connect!
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public String message() {
                return "Lisa has not beed reconnected. Nodes in recon queue at Homer: "
                    + getContollerHomer().getReconnectManager()
                        .getReconnectionQueue();
            }

            public boolean reached() {
                return lisaAtHomer.isCompletelyConnected();
            }
        });
    }

    public void xtestFolderConnectInternetMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testFolderConnectInternet();
            tearDown();
            setUp();
        }
    }

    public void testFolderConnectInternet() throws InvalidIdentityException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);

        // All connections should be detected as on internet.
        Feature.CORRECT_LAN_DETECTION.enable();
        Feature.CORRECT_INTERNET_DETECTION.disable();

        // Reconnect manager has to be started therefore!
        getContollerLisa().getReconnectManager().start();

        final Member margeAtLisa = getContollerMarge().getMySelf().getInfo()
            .getNode(getContollerLisa(), true);
        assertFalse(margeAtLisa.isCompletelyConnected());

        // Join testfolder.
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION, false);

        ConnectResult conRes = margeAtLisa.reconnect();
        assertTrue(conRes.toString(), conRes.isSuccess());

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public String message() {
                return "Marge has not beed reconnected. Nodes in recon queue at Lisa: "
                    + getContollerLisa().getReconnectManager()
                        .getReconnectionQueue().size();
            }

            public boolean reached() {
                return margeAtLisa.isCompletelyConnected();
            }
        });

        // // Again shutdown
        // margeAtLisa.shutdown();
        // // getContollerLisa().getReconnectManager().buildReconnectionQueue();
        //
        // // RECONNECT should happen!
        // // Both are friends so connect!
        // TestHelper.waitForCondition(100, new ConditionWithMessage() {
        // public String message() {
        // return "Marge has not beed reconnected. Nodes in recon queue at Lisa:
        // "
        // + getContollerLisa().getReconnectManager()
        // .getReconnectionQueue().size();
        // }
        //
        // public boolean reached() {
        // return margeAtLisa.isCompletelyConnected();
        // }
        // });
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
            assertFalse(bartAtHomer.reconnect().isSuccess());
            fail("Should not be able to connect. Identity is lisas!");
        } catch (InvalidIdentityException e) {
            // OK!
        }
    }

    /**
     * PFS-3616
     */
    public void testLoopbackConnection() {
        // Prepare folder
        FolderInfo foInfo = joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION, false);
        Folder folder = foInfo.getFolder(getContollerLisa());
        TestHelper.createRandomFile(folder.getLocalBase());
        TestHelper.scanFolder(folder);
        assertFalse(folder.getKnownFiles().isEmpty());

        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        // Reconnect manager has to be started therefore!
        getContollerLisa().getReconnectManager().start();
        try {
            LoggingManager.setConsoleLogging(Level.FINE);
            assertFalse(getContollerLisa().getMySelf().reconnect().isSuccess());
            assertFalse(getContollerLisa().getMySelf().isConnecting());
            assertFalse(getContollerLisa().getMySelf().isConnected());
        } catch (InvalidIdentityException e) {
            // Expected, but not mandatory.
            // Could be thrown at other side of the connection. We might only get an EOF here
        }

        // File DB intact:
        assertFalse(folder.getKnownFiles().isEmpty());
    }

    public void noTestPublicInfrastructureConnect() {
        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerBart(), "");
        for (int i = 0; i < 10; i++) {
            try {
                final Member pegasus = getContollerBart().connect(
                    TestHelper.DEV_SYSTEM_CONNECT_STRING);
                TestHelper.waitForCondition(10, new ConditionWithMessage() {
                    public String message() {
                        return "Unable to connect to pegasus";
                    }

                    public boolean reached() {
                        return pegasus.isCompletelyConnected();
                    }
                });
                assertTrue(pegasus.isCompletelyConnected());
                pegasus.shutdown();

                final Member server = getContollerBart().connect(
                    TestHelper.ONLINE_STORAGE_ADDRESS);
                TestHelper.waitForCondition(10, new ConditionWithMessage() {
                    public String message() {
                        return "Unable to connect to OnlineStorage";
                    }

                    public boolean reached() {
                        return server.isCompletelyConnected();
                    }
                });
                assertTrue(server.isCompletelyConnected());

                ServerClient client = new ServerClient(getContollerBart());
                client.setServer(server, false);
                assertTrue(client.isConnected());
                Account a = client.login("junit@powerfolder.com", Util
                    .toCharArray("asdfgh12"));
                a = client.login("junit@powerfolder.com", Util
                    .toCharArray("asdfgh12"));
                a = client.login("junit@powerfolder.com", Util
                    .toCharArray("asdfgh12"));
                assertNotNull(a);
                assertTrue(a.isValid());
                client.getSecurityService().getFolderPermissions(
                        FolderInfoFactory.newTopFolderForTest("xx", "xx43kljkfjdffewlkjk345j4kj5öjöj"));
                assertTrue(server.isCompletelyConnected());
                server.shutdown();
            } catch (ConnectionException e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }

    }
}
