package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for relayed connections.
 * <p>
 * TRAC #597.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RelayedConnectionTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        getContollerBart().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
    }

    public void testRelayedConnection() throws ConnectionException {
        connect(getContollerBart(), getContollerLisa());
        connect(getContollerBart(), getContollerMarge());

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        getContollerMarge().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());

        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(),
            10 * 1024 * 1024);
        scanFolder(getFolderAtMarge());

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

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

    public void testPublicRelayedConnection() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerMarge(), "");
        assertTrue(getContollerLisa().connect(TestHelper.INFRASTRUCTURE_CONNECT_STRING)
            .isCompleteyConnected());
        assertTrue(getContollerMarge().connect(TestHelper.INFRASTRUCTURE_CONNECT_STRING)
            .isCompleteyConnected());

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        Member lisaAtMarge = getContollerMarge().getNodeManager()
            .acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());

        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.createRandomFile(getFolderAtMarge().getLocalBase(),
            200 * 1024);

        scanFolder(getFolderAtMarge());

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        TestHelper.waitForCondition(40, new ConditionWithMessage() {
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
        assertTrue(conHan.getMember().isCompleteyConnected());
    }

    public void testRelayConnectionToOS() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        assertTrue(getContollerLisa().connect(TestHelper.INFRASTRUCTURE_CONNECT_STRING)
            .isCompleteyConnected());

        Member os = getContollerLisa()
            .connect(Constants.ONLINE_STORAGE_ADDRESS);
        os.shutdown();

        ConnectionHandler conHan = getContollerLisa().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());

        os.shutdown();
        conHan.shutdownWithMember();

        conHan = getContollerLisa().getIOProvider()
            .getRelayedConnectionManager().initRelayedConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());
    }
}
