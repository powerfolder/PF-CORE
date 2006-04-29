package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.NodeManager;

/** event fired from the NodeManager */
public class NodeManagerEvent extends EventObject {
    private Member node;

    public NodeManagerEvent(NodeManager nodeManager, Member node) {
        super(nodeManager);
        this.node = node;
    }

    /** the node that this event is about */
    public Member getNode() {
        return node;
    }
    
    public NodeManager getNodeManager() {
       return (NodeManager) getSource();
    }
}