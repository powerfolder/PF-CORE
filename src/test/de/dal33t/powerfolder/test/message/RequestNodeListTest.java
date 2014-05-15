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
package de.dal33t.powerfolder.test.message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.NodeFilter;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the requesting and answer building of the nodelist from remote peers.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
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
    protected void setUp() throws Exception {
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
            randomNode.getInfo().isConnected = true;
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
            randomNode.getInfo().isConnected = true;
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

        // +1 = lisa and Online Storage
        for (Member member : getContollerBart().getNodeManager()
            .getNodesAsCollection())
        {
        }
        assertEquals(N_TOTAL_NODES + 2, getContollerBart().getNodeManager()
            .getNodesAsCollection().size());
        assertEquals(2, getContollerLisa().getNodeManager()
            .getNodesAsCollection().size());

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
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getNodeManager()
                    .getNodesAsCollection().size() >= // N_CON_NORMAL_NODES+
                N_CON_SUPERNODES;
            }

            public String message() {
                return "Lisas known nodes "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection().size()
                    + ". content: "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection();
            }
        });

        // Let supernode state sync/broadcast
        TestHelper.waitMilliSeconds(3000);

        // Should have all online supernodes
        assertEquals(N_CON_SUPERNODES, getContollerLisa().getNodeManager()
            .countSupernodes());

        // And all other online nodes (with bart and online storage)
        assertEquals(N_CON_SUPERNODES + 2, getContollerLisa().getNodeManager()
            .getNodesAsCollection().size());
    }

    /**
     * Test request by nodelist. Usual for requesting new information about own
     * friendlist.
     *
     * @throws ConnectionException
     */
    public void testRequestNodesListByNodeIdList() throws ConnectionException {
        final int nNodes = 25;
        final List<Member> testNodes = new ArrayList<Member>();
        int i = 0;
        for (Member node : getContollerBart().getNodeManager()
            .getNodesAsCollection())
        {
            testNodes.add(node);
            i++;
            if (i >= nNodes) {
                break;
            }
        }

        getContollerLisa().getNodeManager().addNodeFilter(new NodeFilter() {
            public boolean shouldAddNode(MemberInfo nodeInfo) {
                return Convert.asMemberInfos(testNodes).contains(nodeInfo);
            }
        });
        // Request default list
        bartAtLisa.sendMessage(RequestNodeList.createRequest(testNodes,
            RequestNodeList.NodesCriteria.NONE,
            RequestNodeList.NodesCriteria.NONE));

        // Wait for answer
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getNodeManager()
                    .getNodesAsCollection().size() >= nNodes;
            }

            public String message() {
                return "Lisas known nodes "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection().size()
                    + ". content: "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection();
            }
        });

        // Should have 10 +2 (bart and Online Storage)
        assertEquals(nNodes + 2, getContollerLisa().getNodeManager()
            .getNodesAsCollection().size());

        for (Member nodeAtLisa : getContollerLisa().getNodeManager()
            .getNodesAsCollection())
        {
            if (!testNodes.contains(nodeAtLisa)
                && !nodeAtLisa.getNick().equals("Bart")
                && !nodeAtLisa.getNick().equals(
                    getContollerBart().getOSClient().getServer().getNick()))
            {
                fail("Not requested: " + nodeAtLisa);
            }
        }
    }

    public void testRequestAllNodes() throws ConnectionException {
        getContollerLisa().getNodeManager().addNodeFilter(new NodeFilter() {
            public boolean shouldAddNode(MemberInfo nodeInfo) {
                return true;
            }
        });
        // Request all nodes
        bartAtLisa.sendMessage(RequestNodeList.createRequestAllNodes());

        // Wait for answer
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getNodeManager()
                    .getNodesAsCollection().size() >= N_TOTAL_NODES;
            }

            public String message() {
                return "Lisas known nodes "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection().size()
                    + ". content: "
                    + getContollerLisa().getNodeManager()
                        .getNodesAsCollection();
            }
        });

        // Should have all supernodes
        assertEquals(N_CON_SUPERNODES + N_OFFLINE_SUPERNODES,
            getContollerLisa().getNodeManager().countSupernodes());

        // And all nodes (with bart and online storage)
        assertEquals(N_TOTAL_NODES + 2, getContollerLisa().getNodeManager()
            .getNodesAsCollection().size());

        // This one fails, because the memberinfos received are all marked as
        // isConnected = false
        // assertEquals(N_CON_NORMAL_NODES + N_CON_SUPERNODES + 1,
        // getContollerLisa().getNodeManager().countOnlineNodes());
    }

    private MemberInfo createSampleMemberInfo(String nick,
        Date lastConnectTime, boolean supernode)
    {
        MemberInfo sample = new MemberInfo(nick, IdGenerator.makeId(), null);
        sample.setLastConnectTime(lastConnectTime);
        sample.isSupernode = supernode;
        return sample;
    }
}
