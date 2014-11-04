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

import de.dal33t.powerfolder.net.NodeManager;

/**
 * Implement this class to receive events from the NodeManager.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom </A>
 * @version $Revision: 1.2 $
 */
public interface NodeManagerListener extends CoreListener {

    /**
     * Called if a node was added to the internal member node storage of
     * {@link NodeManager}
     *
     * @param e
     */
    public void nodeAdded(NodeManagerEvent e);

    /**
     * Called if a node was removef from the internal member node storage of
     * {@link NodeManager}
     *
     * @param e
     */
    public void nodeRemoved(NodeManagerEvent e);

    /**
     * When a node started connecting. Will be followed by
     * {@link #nodeConnected(NodeManagerEvent)} or
     * {@link #nodeDisconnected(NodeManagerEvent)}
     *
     * @param e
     */
    public void nodeConnecting(NodeManagerEvent e);

    /**
     * When the node actually connected to this computer.
     *
     * @param e
     */
    public void nodeConnected(NodeManagerEvent e);

    /**
     * If physically disconnects
     *
     * @param e
     */
    public void nodeDisconnected(NodeManagerEvent e);

    /**
     * Event when this node gets ONLINE in the network. This does NOT mean that
     * this node is directly connected!!
     *
     * @param e
     */
    public void nodeOnline(NodeManagerEvent e);

    /**
     * When the node leaves/shuts down from the online network.
     *
     * @param e
     */
    public void nodeOffline(NodeManagerEvent e);

    public void friendAdded(NodeManagerEvent e);

    public void friendRemoved(NodeManagerEvent e);

    public void settingsChanged(NodeManagerEvent e);

    public void startStop(NodeManagerEvent e);
}