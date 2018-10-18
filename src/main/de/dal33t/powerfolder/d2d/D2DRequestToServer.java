package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;

public interface D2DRequestToServer extends D2DObject {

    default void handle(Member node) {
        if (node.getController().getMySelf().isServer()) {
            node.getController().getNodeManager().messageReceived(node, (Message) this);
        }
    }

}
