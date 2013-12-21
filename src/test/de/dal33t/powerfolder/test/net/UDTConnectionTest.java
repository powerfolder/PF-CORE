/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.net.InetSocketAddress;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.net.RelayFinder;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for UDT connections. Mostly copied from RelayConnectionTest. TRAC #591
 * 
 * @author Dennis "Bytekeeper" Waldherr
 * @version $Revision: 1.5 $
 */
public class UDTConnectionTest extends FiveControllerTestCase {
    // static {
    // Logger.removeExcludeConsoleLogLevel(Logger.VERBOSE);
    // }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
    }

    public void testUDTConnection() throws ConnectionException {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }

        connectOrFail(getContollerBart(), getContollerLisa());
        connectOrFail(getContollerBart(), getContollerMarge());
        getContollerLisa().getMySelf().getInfo().setConnectAddress(
            new InetSocketAddress("192.168.0.4", 10000));
        getContollerBart().getMySelf().getInfo().setConnectAddress(
            new InetSocketAddress("192.168.0.4", 10001));

        getContollerMarge().getIOProvider().getRelayedConnectionManager()
            .setRelayFiner(new RelayFinder() {
                public Member findRelay(NodeManager nodeManager) {
                    return nodeManager.getNode(getContollerBart().getMySelf()
                        .getInfo());
                }
            });

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getUDTSocketConnectionManager()
            .initRendezvousUDTConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        getContollerMarge().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompletelyConnected());

        joinTestFolder(SyncProfile.HOST_FILES, false);
        TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(),
            10 * 1024 * 1024);
        scanFolder(getFolderAtMarge());

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(40, new ConditionWithMessage() {
            public String message() {
                return "Files at lisa: "
                    + getFolderAtLisa().getKnownFiles().size();
            }

            public boolean reached() {
                return getFolderAtLisa().getKnownFiles().size() == 1;
            }
        });
    }
    /**
     * The following tests will always fail unless your relay is within the same
     * subnet. The reason for this is that a relay behind a router can't
     * possibly tell you your local IP unless you're directly connected to the
     * internet. public void testPublicUDTConnection() throws
     * ConnectionException {
     * getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
     * ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
     * getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
     * ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerMarge(), "");
     * assertTrue(getContollerLisa().connect("server.powerfolder.com")
     * .isCompleteyConnected());
     * assertTrue(getContollerMarge().connect("server.powerfolder.com")
     * .isCompleteyConnected()); ConnectionHandler conHan =
     * getContollerMarge().getIOProvider()
     * .getUDTSocketConnectionManager().initUDTConnectionHandler(
     * getContollerLisa().getMySelf().getInfo());
     * assertTrue(conHan.isConnected()); Member lisaAtMarge =
     * getContollerMarge().getNodeManager() .acceptConnection(conHan);
     * assertTrue(conHan.isConnected()); assertNotNull(conHan.getMember());
     * assertTrue(conHan.getMember().isCompleteyConnected());
     * joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
     * TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(), 200 *
     * 1024); scanFolder(getFolderAtMarge());
     * getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
     * TestHelper.waitForCondition(40, new ConditionWithMessage() { public
     * String message() { return "Files at lisa: " +
     * getFolderAtLisa().getKnownFiles().size(); } public boolean reached() {
     * return getFolderAtLisa().getKnownFiles().size() == 1; } }); // Disconnect
     * and reconnect lisaAtMarge.shutdown(); conHan =
     * getContollerMarge().getIOProvider()
     * .getUDTSocketConnectionManager().initUDTConnectionHandler(
     * getContollerLisa().getMySelf().getInfo());
     * assertTrue(conHan.isConnected()); lisaAtMarge =
     * getContollerMarge().getNodeManager().acceptConnection( conHan);
     * assertTrue(conHan.isConnected()); assertNotNull(conHan.getMember());
     * assertTrue(conHan.getMember().isCompleteyConnected()); } public void
     * testUDTConnectionToOS() throws ConnectionException {
     * getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
     * ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
     * assertTrue(getContollerLisa().connect("server.powerfolder.com")
     * .isCompleteyConnected()); Member os = getContollerLisa()
     * .connect(Constants.ONLINE_STORAGE_ADDRESS); os.shutdown();
     * ConnectionHandler conHan = getContollerLisa().getIOProvider()
     * .getUDTSocketConnectionManager().initUDTConnectionHandler( os.getInfo());
     * assertTrue(conHan.isConnected());
     * getContollerLisa().getNodeManager().acceptConnection(conHan);
     * assertTrue(conHan.isConnected()); assertNotNull(conHan.getMember());
     * assertTrue(conHan.getMember().isCompleteyConnected()); os.shutdown();
     * conHan.shutdownWithMember(); conHan = getContollerLisa().getIOProvider()
     * .getUDTSocketConnectionManager().initUDTConnectionHandler( os.getInfo());
     * assertTrue(conHan.isConnected());
     * getContollerLisa().getNodeManager().acceptConnection(conHan);
     * assertTrue(conHan.isConnected()); assertNotNull(conHan.getMember());
     * assertTrue(conHan.getMember().isCompleteyConnected()); }
     */
}
