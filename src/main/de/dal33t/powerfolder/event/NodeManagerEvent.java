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