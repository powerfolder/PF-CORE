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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

import java.io.Externalizable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger log = Logger.getLogger(KnownNodes.class
        .getName());
    private static final long serialVersionUID = 101L;

    public MemberInfo[] nodes;

    protected KnownNodes() {
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
            log.warning("Nodelist longer than max size: " + this);
        }
    }

    /**
     * Creats mutliple known nodes messages from the nodelist
     *
     * @param nodesList
     * @param useExt
     *            #2072: if use {@link Externalizable} versions of the messages.
     * @return the array of the messages
     */
    public static Message[] createKnownNodesList(List<MemberInfo> nodesList,
        boolean useExt)
    {
        if (nodesList.size() < Constants.NODES_LIST_MAX_NODES_PER_MESSAGE) {
            // One list only
            MemberInfo[] nodes = getArray(nodesList, 0, nodesList.size());
            KnownNodes message = useExt
                ? new KnownNodesExt(nodes)
                : new KnownNodes(nodes);
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
            messages[i] = useExt ? new KnownNodesExt(slice) : new KnownNodes(
                slice);
        }

        // Add last list
        if (lastListSize > 0) {
            MemberInfo[] slice = getArray(nodesList, nLists
                * Constants.NODES_LIST_MAX_NODES_PER_MESSAGE, lastListSize);
            messages[arrSize - 1] = useExt
                ? new KnownNodesExt(slice)
                : new KnownNodes(slice);
        }

        if (log.isLoggable(Level.FINER)) {
            log.finer("Built " + messages.length + " nodelists");
        }

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