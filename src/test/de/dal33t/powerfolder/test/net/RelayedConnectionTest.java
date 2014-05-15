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
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.net.RelayFinder;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for relayed connections.
 * <p>
 * TRAC #597.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RelayedConnectionTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Don't check limited connectivity.
        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
    }

    public void testRelayedConnection() throws ConnectionException {
        connectOrFail(getContollerBart(), getContollerLisa());
        connectOrFail(getContollerBart(), getContollerMarge());

        // Use bart as relay
        getContollerMarge().getIOProvider().getRelayedConnectionManager()
            .setRelayFiner(new RelayFinder() {
                public Member findRelay(NodeManager nodeManager) {
                    return nodeManager.getNode(getContollerBart().getMySelf()
                        .getId());
                }
            });

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        Member lisaAtMarge = getContollerMarge().getNodeManager()
            .acceptConnection(conHan);

        assertTrue("Marge not supernode", getContollerMarge().getMySelf()
            .isSupernode());
        assertTrue("Lisa is not interesting at marge", lisaAtMarge
            .isInteresting());
        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());

        joinTestFolder(SyncProfile.HOST_FILES, false);
        TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(),
            10 * 1024 * 1024);
        scanFolder(getFolderAtMarge());

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(120, new ConditionWithMessage() {
            public String message() {
                return "Files at lisa: "
                    + getFolderAtLisa().getKnownFiles().size();
            }

            public boolean reached() {
                return getFolderAtLisa().getKnownFiles().size() == 1;
            }
        });
    }

    public void noTestPublicRelayedConnection() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerMarge(), "");
        assertTrue(getContollerLisa().connect(
            TestHelper.DEV_SYSTEM_CONNECT_STRING).isCompletelyConnected());
        assertTrue(getContollerMarge().connect(
            TestHelper.DEV_SYSTEM_CONNECT_STRING).isCompletelyConnected());

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        Member lisaAtMarge = getContollerMarge().getNodeManager()
            .acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());

        joinTestFolder(SyncProfile.HOST_FILES);
        TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(),
            200 * 1024);

        scanFolder(getFolderAtMarge());

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        TestHelper.waitForCondition(120, new ConditionWithMessage() {
            public String message() {
                return "Files at lisa: "
                    + getFolderAtLisa().getKnownFiles().size();
            }

            public boolean reached() {
                return getFolderAtLisa().getKnownFiles().size() == 1;
            }
        });

        // Disconnect and reconnect
        lisaAtMarge.shutdown();

        conHan = getContollerMarge().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        lisaAtMarge = getContollerMarge().getNodeManager().acceptConnection(
            conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());
    }

    public void noTestRelayConnectionToOS() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        assertTrue(getContollerLisa().connect(
            TestHelper.DEV_SYSTEM_CONNECT_STRING).isCompletelyConnected());

        Member os = getContollerLisa().connect(
            TestHelper.ONLINE_STORAGE_ADDRESS);
        os.shutdown();

        ConnectionHandler conHan = getContollerLisa().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());

        os.shutdown();
        conHan.shutdownWithMember();

        conHan = getContollerLisa().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());
    }
}
