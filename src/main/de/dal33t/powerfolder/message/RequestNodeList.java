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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.protocol.NodesCriteriaProto;
import de.dal33t.powerfolder.protocol.RequestNodeListProto;
import de.dal33t.powerfolder.util.Reject;

/**
 * A request to send the nodelist. Expected answer is a <code>KnownNodes</code>
 * message.
 * <p>
 * TODO Add filter criteria for folders.
 *
 * @see de.dal33t.powerfolder.message.KnownNodes
 * @see de.dal33t.powerfolder.message.SearchNodeRequest
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class RequestNodeList extends Message
  implements D2DObject
{
    private static final long serialVersionUID = 101L;

    /**
     * The criteria for getting the supernodes infos.
     */
    public static enum NodesCriteria {
        ALL, ONLINE, NONE
    }

    /**
     * What supernodes information should be returned.
     */
    private NodesCriteria supernodesCriteria;

    /**
     * What other nodes information should be returned, additionally match
     * through nodeId list.
     */
    private NodesCriteria nodesCriteria;

    /**
     * The list of node ids for which we want to request fresh connection
     * information. May be null. Usually the ids of the friends.
     */
    private Collection<String> nodeIds;

    /** RequestNodeList
     * Init class
     * @author Christoph Kappel <kappel@powerfolder.com>
     **/

    public RequestNodeList() {
        /* Empty constructor for D2D */
    }

    /**
     * Constructs a new request.
     */
    private RequestNodeList(Collection<String> mIds,
        NodesCriteria supernodesCrit, NodesCriteria normalNodesCrit)
    {
        Reject.ifNull(normalNodesCrit, "Nodes criteria is null");
        Reject.ifNull(supernodesCrit, "Supernodes criteria is null");
        supernodesCriteria = supernodesCrit;
        nodesCriteria = normalNodesCrit;
        nodeIds = mIds;
    }

    // Creation methods *******************************************************

    /**
     * Creates a request nodes message, which requests all node infomations.
     *
     * @return the request message
     */
    public static RequestNodeList createRequestAllNodes() {
        return new RequestNodeList(null, NodesCriteria.ALL, NodesCriteria.ALL);
    }

    /**
     * Creates a request nodes message, which requests nodes information of the
     * given nodes. Can additionally obtain information about supernodes.
     *
     * @param nodes
     *            the nodes to request new information for
     * @param supernodesCriteria
     *            the supernodes criteria.
     * @param normalNodesCriteria
     *            the criteria for normal nodes in not in id list.
     * @return the message to sent
     */
    public static RequestNodeList createRequest(Collection<Member> nodes,
        NodesCriteria supernodesCriteria, NodesCriteria normalNodesCriteria)
    {
        Reject.ifNull(nodes, "Nodes is null");
        Collection<String> mIds = new HashSet<String>(nodes.size());
        for (Member node : nodes) {
            mIds.add(node.getId());
        }
        return new RequestNodeList(mIds, supernodesCriteria,
            normalNodesCriteria);
    }

    // API ********************************************************************

    /**
     * Filter the collection of nodes with the criterias of this request.
     *
     * @param source
     *            the list of nodes
     * @return the filtered list of node infos. never contains temporary sever
     *         nodes.
     */
    public List<MemberInfo> filter(Collection<Member> source) {
        List<MemberInfo> nodes = null;
        // reduce abnormal container growth
        if (NodesCriteria.ALL.equals(nodesCriteria)) {
            nodes = new ArrayList<MemberInfo>(source.size());
        } else {
            nodes = new ArrayList<MemberInfo>();
        }

        for (Member node : source) {
            if (matches(node)) {
                nodes.add(node.getInfo());
            }
        }
        return nodes;
    }

    // Helper *****************************************************************

    private boolean matches(Member node) {
        if (ServerClient.isTempServerNode(node.getInfo())) {
            return false;
        }
        // "general" nodes criteria match
        if (NodesCriteria.ALL.equals(nodesCriteria)) {
            return true;
        }
        if (NodesCriteria.ONLINE.equals(nodesCriteria)
            && node.isConnectedToNetwork())
        {
            return true;
        }
        // Supernode match
        if (NodesCriteria.ALL.equals(supernodesCriteria) && node.isSupernode())
        {
            return true;
        }
        if (NodesCriteria.ONLINE.equals(supernodesCriteria)
            && node.isSupernode() && node.isConnectedToNetwork())
        {
            return true;
        }
        // Node id list match
        if (nodeIds != null && nodeIds.contains(node.getId())) {
            return true;
        }
        return false;
    }

    // General ****************************************************************

    @Override
    public String toString() {
        return "Request for NodeList (supernodes: " + supernodesCriteria
            + ", normal-nodes: " + nodesCriteria + ", "
            + (nodeIds == null ? "all" : Integer.valueOf(nodeIds.size()))
            + " nodes)";
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof RequestNodeListProto.RequestNodeList)
        {
          RequestNodeListProto.RequestNodeList proto =
            (RequestNodeListProto.RequestNodeList)mesg;

          /* Fill in enum values */
          for(NodesCriteria crit : NodesCriteria.values())
            {
              if(crit.ordinal() == proto.getSupernodesCriteria().getValueValue())
                this.supernodesCriteria = crit;
              if(crit.ordinal() == proto.getNodesCriteria().getValueValue())
                this.nodesCriteria = crit;
            }

          /* Convert list back to collection */
          if(null != this.nodeIds)
              this.nodeIds.clear();

          for(String str : proto.getNodeIDsList())
            {
              this.nodeIds.add(str);
            }
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      RequestNodeListProto.RequestNodeList.Builder builder =
        RequestNodeListProto.RequestNodeList.newBuilder();

      builder.setClazzName("RequestNodeList");

      /* Handle enum stuff */
      NodesCriteriaProto.NodesCriteria.Builder nodeBuilder =
        NodesCriteriaProto.NodesCriteria.newBuilder();

      nodeBuilder.setValueValue(this.supernodesCriteria.ordinal());

      builder.setSupernodesCriteria(nodeBuilder.build());

      nodeBuilder.setValueValue(this.nodesCriteria.ordinal());

      builder.setNodesCriteria(nodeBuilder.build());

      /* Just append collection here */
      if(null != this.nodeIds) {
          builder.addAllNodeIDs(this.nodeIds);
      } else builder.addAllNodeIDs(Collections.emptyList());

      return builder.build();
    }
}