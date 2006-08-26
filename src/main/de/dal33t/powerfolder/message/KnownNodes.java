/* $Id: KnownNodes.java,v 1.11 2006/04/23 16:00:30 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains information about nodes. This message is a answer message for
 * <code>RequestNodeList</code> and <code>SearchNodeRequest</code> messages.
 * 
 * @see de.dal33t.powerfolder.message.RequestNodeList
 * @see de.dal33t.powerfolder.message.SearchNodeRequest
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class KnownNodes extends Message {
    private static final long serialVersionUID = 101L;
    private static final Logger LOG = Logger.getLogger(KnownNodes.class);

    public MemberInfo[] nodes;

    public KnownNodes() {
        // Serialisation constructor
    }

    /**
     * Creates a nodelist with only one node
     * 
     * @param node
     */
    public KnownNodes(MemberInfo node) {
        if (node == null) {
            throw new NullPointerException("Node is null");
        }
        nodes = new MemberInfo[]{node};
    }

    /**
     * Creates a nodelist with nodes
     * 
     * @param nodes
     */
    public KnownNodes(MemberInfo[] nodes) {
        Reject.ifNull(nodes, "Nodes is null");
        this.nodes = nodes;

        if (nodes.length > Constants.NODES_LIST_MAX_NODES_PER_MESSAGE) {
            LOG.warn("Nodelist longer than max size: " + this);
        }
    }

    /**
     * Creates the message for the nodemanager. List gets splitted into smaller
     * ones if required
     * 
     * @deprecated Not longer use this. Switch to request/response based way
     * @param nm
     *            the nodemanager
     * @return the splitted lists
     */
    public static Message[] createKnownNodesList(NodeManager nm) {
        Reject.ifNull(nm, "NodeManager ist null");

        boolean iamSupernode = nm.getMySelf().isSupernode();

        // Filter nodes
        Member[] validNodes = nm.getValidNodes();
        List<MemberInfo> nodesList = new ArrayList<MemberInfo>(
            validNodes.length);

        // Offline limit time, all nodes before this time are not getting send
        // to remote
        Date offlineLimitTime = new Date(System.currentTimeMillis()
            - Constants.MAX_NODE_OFFLINE_TIME);

        for (int i = 0; i < validNodes.length; i++) {
            Member node = validNodes[i];
            // Check if node was offline too long
            Date lastConnectTime = node.getLastNetworkConnectTime();
            boolean offlineTooLong = true;

            offlineTooLong = lastConnectTime != null ? lastConnectTime
                .before(offlineLimitTime) : true;
            if (iamSupernode || node.isSupernode() || !offlineTooLong
                || node.isConnected() || node.isMySelf())
            {
                nodesList.add(node.getInfo());
            }
        }

        return createKnownNodesList(nodesList);
    }

    /**
     * Creats mutliple known nodes messages from the nodelist
     * 
     * @param nodesList
     * @return the array of the messages
     */
    public static Message[] createKnownNodesList(List<MemberInfo> nodesList) {
        if (nodesList.size() < Constants.NODES_LIST_MAX_NODES_PER_MESSAGE) {
            // One list only
            MemberInfo[] nodes = getArray(nodesList, 0, nodesList.size());
            KnownNodes message = new KnownNodes(nodes);
            return new KnownNodes[]{message};
        }

        // Split list
        int nLists = nodesList.size()
            / Constants.NODES_LIST_MAX_NODES_PER_MESSAGE;
        int lastListSize = nodesList.size()
            - Constants.NODES_LIST_MAX_NODES_PER_MESSAGE * nLists;
        int arrSize = nLists;
        if (lastListSize > 0) {
            arrSize++;
        }

        KnownNodes[] messages = new KnownNodes[arrSize];
        for (int i = 0; i < nLists; i++) {
            MemberInfo[] slice = getArray(nodesList, i
                * Constants.NODES_LIST_MAX_NODES_PER_MESSAGE,
                Constants.NODES_LIST_MAX_NODES_PER_MESSAGE);
            messages[i] = new KnownNodes(slice);
        }

        // Add last list
        if (lastListSize > 0) {
            MemberInfo[] slice = getArray(nodesList, nLists
                * Constants.NODES_LIST_MAX_NODES_PER_MESSAGE, lastListSize);
            messages[arrSize - 1] = new KnownNodes(slice);
        }

        LOG.verbose("Built " + messages.length + " nodelists");

        return messages;
    }

    // Helper ***************************************************************

    /**
     * Gets a slice from the nodelist in a array
     * 
     * @param nodeslist
     * @param offset
     * @param lenght
     * @return
     */
    private static MemberInfo[] getArray(List<MemberInfo> nodeslist,
        int offset, int lenght)
    {
        Reject.ifNull(nodeslist, "Nodelist is null");
        Reject.ifTrue(offset > nodeslist.size(), "Offset (" + offset
            + ") greater than nodelist size (" + nodeslist.size() + ")");
        Reject.ifTrue(offset + lenght > nodeslist.size(), "Lenght (" + lenght
            + ") exceeds nodelist size (" + nodeslist.size() + "), offset ("
            + offset + ")");
        Reject.ifTrue(offset < 0, "Offset is below zero");

        int endOffset = offset + lenght;
        MemberInfo[] arr = new MemberInfo[lenght];
        int arrOff = 0;
        // Use System.arrayCopy here?
        for (int i = offset; i < endOffset; i++) {
            arr[arrOff] = nodeslist.get(i);
            arrOff++;
        }

        return arr;
    }

    // General **************************************************************

    public String toString() {
        return "NodeList, " + (nodes != null ? nodes.length : 0) + " nodes(s)";
    }
}