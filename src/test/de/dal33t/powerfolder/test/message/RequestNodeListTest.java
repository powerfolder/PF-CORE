package de.dal33t.powerfolder.test.message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.test.Condition;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests the requesting and answer building of the nodelist from remote peers.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RequestNodeListTest extends TwoControllerTestCase {
    private static final int N_CON_SUPERNODES = 10;
    private static final int N_OFFLINE_SUPERNODES = 80;
    private static final int N_CON_NORMAL_NODES = 200;
    private static final int N_OFFLINE_NORMAL_NODES = 2000;

    private static final int N_TOTAL_NODES = N_CON_SUPERNODES
        + N_OFFLINE_SUPERNODES + N_CON_NORMAL_NODES + N_OFFLINE_NORMAL_NODES;

    private Member bartAtLisa;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();

        // Add nodes to bart
        for (int i = 0; i < N_CON_SUPERNODES; i++) {
            String nick = IdGenerator.makeId();
            MemberInfo randomNodeInfo = createSampleMemberInfo(nick,
                new Date(), true);
            randomNodeInfo.setConnectAddress(new InetSocketAddress("localhost",
                2000));
            Member randomNode = getContollerBart().getNodeManager().addNode(
                randomNodeInfo);
            randomNode.setConnectedToNetwork(true);
        }

        for (int i = 0; i < N_OFFLINE_SUPERNODES; i++) {
            String nick = IdGenerator.makeId();
            MemberInfo randomNodeInfo = createSampleMemberInfo(nick, new Date(
                System.currentTimeMillis() - 60000), true);
            randomNodeInfo.setConnectAddress(new InetSocketAddress("localhost",
                2000));
            Member randomNode = getContollerBart().getNodeManager().addNode(
                randomNodeInfo);
            randomNode.setConnectedToNetwork(false);
        }

        for (int i = 0; i < N_CON_NORMAL_NODES; i++) {
            String nick = IdGenerator.makeId();
            MemberInfo randomNodeInfo = createSampleMemberInfo(nick,
                new Date(), false);
            randomNodeInfo.setConnectAddress(new InetSocketAddress("localhost",
                2000));
            Member randomNode = getContollerBart().getNodeManager().addNode(
                randomNodeInfo);
            randomNode.setConnectedToNetwork(true);
        }

        for (int i = 0; i < N_OFFLINE_NORMAL_NODES; i++) {
            String nick = IdGenerator.makeId();
            MemberInfo randomNodeInfo = createSampleMemberInfo(nick, new Date(
                System.currentTimeMillis() - 60000), false);
            randomNodeInfo.setConnectAddress(new InetSocketAddress("localhost",
                2000));
            Member randomNode = getContollerBart().getNodeManager().addNode(
                randomNodeInfo);
            randomNode.setConnectedToNetwork(false);
        }

        // +1 = lisa
        assertEquals(N_TOTAL_NODES + 1, getContollerBart().getNodeManager()
            .countNodes());
        assertEquals(1, getContollerLisa().getNodeManager().countNodes());

        // Convenience var
        bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());
    }

    /**
     * Tests the default request for nodelist from a normal peer (lisa)
     * 
     * @throws ConnectionException
     */
    public void testRequestDefaultNodesList() throws ConnectionException {
        // Request default list
        bartAtLisa.sendMessage(getContollerLisa().getNodeManager()
            .createDefaultNodeListRequestMessage());

        // Wait for answer
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getContollerLisa().getNodeManager().countNodes() >= N_CON_NORMAL_NODES
                    + N_CON_SUPERNODES;
            }
        });

        // Should have all online supernodes
        assertEquals(N_CON_SUPERNODES, getContollerLisa().getNodeManager()
            .countSupernodes());

        // And all other online nodes
        assertEquals(N_CON_NORMAL_NODES + N_CON_SUPERNODES + 1,
            getContollerLisa().getNodeManager().countNodes());
    }

    /**
     * Test request by nodelist. Usual for requesting new information about own
     * friendlist.
     * 
     * @throws ConnectionException
     */
    public void testRequestNodesListByNodeIdList() throws ConnectionException {
        final int nNodes = 25;
        List<Member> testNodes = new ArrayList<Member>();
        for (int i = 0; i < nNodes; i++) {
            testNodes.add(getContollerBart().getNodeManager().getNodes()[i]);
        }

        // Request default list
        bartAtLisa.sendMessage(RequestNodeList.createRequest(testNodes,
            RequestNodeList.NodesCriteria.NONE,
            RequestNodeList.NodesCriteria.NONE));

        // Wait for answer
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getContollerLisa().getNodeManager().countNodes() >= nNodes;
            }
        });

        // Should have 10 +1 (bart)
        assertEquals(nNodes + 1, getContollerLisa().getNodeManager()
            .countNodes());

        Member[] nodesAtLisa = getContollerLisa().getNodeManager().getNodes();
        for (Member nodeAtLisa : nodesAtLisa) {
            if (!testNodes.contains(nodeAtLisa)
                && !nodeAtLisa.getNick().equals("Bart"))
            {
                fail("Not requested: " + nodeAtLisa);
            }
        }
    }

    public void testRequestAllNodes() throws ConnectionException {
        // Request all nodes
        bartAtLisa.sendMessage(RequestNodeList.createRequestAllNodes());

        // Wait for answer
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getContollerLisa().getNodeManager().countNodes() >= N_TOTAL_NODES;
            }
        });

        // Should have all supernodes
        assertEquals(N_CON_SUPERNODES + N_OFFLINE_SUPERNODES,
            getContollerLisa().getNodeManager().countSupernodes());

        // And all nodes
        assertEquals(N_TOTAL_NODES + 1, getContollerLisa().getNodeManager()
            .countNodes());

        // This one fails, because the memberinfos received are all marked as
        // isConnected = false
        // assertEquals(N_CON_NORMAL_NODES + N_CON_SUPERNODES + 1,
        // getContollerLisa().getNodeManager().countOnlineNodes());
    }

    private MemberInfo createSampleMemberInfo(String nick,
        Date lastConnectTime, boolean supernode)
    {
        MemberInfo sample = new MemberInfo(nick, IdGenerator.makeId());
        sample.lastConnectTime = lastConnectTime;
        sample.isSupernode = supernode;
        return sample;
    }
}
