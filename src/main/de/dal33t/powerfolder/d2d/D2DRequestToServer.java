package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;

public interface D2DRequestToServer extends D2DObject {

    @Override
    default void handle(Member node) {
        node.getController().getNodeManager().messageReceived(node, (Message)this);
    }
}
