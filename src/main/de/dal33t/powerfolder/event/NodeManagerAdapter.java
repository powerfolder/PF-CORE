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


/**
 *  Adapter that implement NodeManagerListener, for the convenience of handling NodeManagerEvent.
 */
public class NodeManagerAdapter implements
		NodeManagerListener {

	public void friendAdded(NodeManagerEvent e) {
	}

	public void friendRemoved(NodeManagerEvent e) {
	}

	public void nodeAdded(NodeManagerEvent e) {
	}

	public void nodeConnected(NodeManagerEvent e) {
	}

	public void nodeDisconnected(NodeManagerEvent e) {
	}

	public void nodeRemoved(NodeManagerEvent e) {
	}

	public void settingsChanged(NodeManagerEvent e) {
	}

	public void startStop(NodeManagerEvent e) {
	}

	public boolean fireInEventDispathThread() {
		return false;
	}
}
