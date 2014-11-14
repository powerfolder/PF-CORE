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
import de.dal33t.powerfolder.ui.model.NodeManagerModel;

/**
 * Event fired from the NodeManagerModel
 */
public class NodeManagerModelEvent extends EventObject {

    private Member node;

    /**
     * Constructor
     *
     * @param nodeManagerModel
     * @param node
     */
    public NodeManagerModelEvent(NodeManagerModel nodeManagerModel, Member node) {
        super(nodeManagerModel);
        this.node = node;
    }

    /**
     * The node that this event is about
     */
    /**
     *
     * @return
     */
    public Member getNode() {
        return node;
    }

    /**
     * The node manager model source
     *
     * @return
     */
    public NodeManagerModel getNodeManagerModel() {
       return (NodeManagerModel) getSource();
    }
}