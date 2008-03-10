package de.dal33t.powerfolder.test.net;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for UDT connections.
 * Mostly copied from RelayConnectionTest.
 * TRAC #591
 *  
 * @author Dennis "Bytekeeper" Waldherr
 * @version $Revision: 1.5 $
 */
public class UDTConnectionTest extends FiveControllerTestCase {
//	static {
//        Logger.removeExcludeConsoleLogLevel(Logger.VERBOSE);
//	}
	
	public void testUDTConnection() throws ConnectionException {

        connect(getContollerBart(), getContollerLisa());
        connect(getContollerBart(), getContollerMarge());

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getUDTSocketConnectionManager()
            .initUDTConnectionHandler(getContollerLisa().getMySelf().getInfo());
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
/**	The following tests will always fail unless your relay is within the same subnet. The reason for this is that
 *  a relay behind a router can't possibly tell you your local IP unless you're directly connected to the internet.

    public void testPublicUDTConnection() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        getContollerMarge().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerMarge(), "");
        assertTrue(getContollerLisa().connect("server.powerfolder.com")
            .isCompleteyConnected());
        assertTrue(getContollerMarge().connect("server.powerfolder.com")
            .isCompleteyConnected());

        ConnectionHandler conHan = getContollerMarge().getIOProvider()
            .getUDTSocketConnectionManager().initUDTConnectionHandler(
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
            .getUDTSocketConnectionManager().initUDTConnectionHandler(
                getContollerLisa().getMySelf().getInfo());
        assertTrue(conHan.isConnected());
        lisaAtMarge = getContollerMarge().getNodeManager().acceptConnection(
            conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());
    }

    public void testUDTConnectionToOS() throws ConnectionException {
        getContollerLisa().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        ConfigurationEntry.NET_BIND_ADDRESS.setValue(getContollerLisa(), "");
        assertTrue(getContollerLisa().connect("server.powerfolder.com")
            .isCompleteyConnected());

        Member os = getContollerLisa()
            .connect(Constants.ONLINE_STORAGE_ADDRESS);
        os.shutdown();

        ConnectionHandler conHan = getContollerLisa().getIOProvider()
            .getUDTSocketConnectionManager().initUDTConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());

        os.shutdown();
        conHan.shutdownWithMember();

        conHan = getContollerLisa().getIOProvider()
            .getUDTSocketConnectionManager().initUDTConnectionHandler(
                os.getInfo());
        assertTrue(conHan.isConnected());
        getContollerLisa().getNodeManager().acceptConnection(conHan);

        assertTrue(conHan.isConnected());
        assertNotNull(conHan.getMember());
        assertTrue(conHan.getMember().isCompleteyConnected());
    }
    */
}
