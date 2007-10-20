package de.dal33t.powerfolder.event;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.NodeManager;

/**
 *  Adapter that implement NodeManagerListener, for the convenience of handling NodeManagerEvent.
 */
public class NodeManagerAdapter implements
		NodeManagerListener {

	public void friendAdded(NodeManagerEvent e) {
		
		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireFriendAdded(node);

	}

	public void friendRemoved(NodeManagerEvent e) {

		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireFriendRemoved(node);
		
	}

	public void nodeAdded(NodeManagerEvent e) {

		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireNodeAdded(node);
		
	}

	public void nodeConnected(NodeManagerEvent e) {

		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireNodeConnected(node);
		
	}

	public void nodeDisconnected(NodeManagerEvent e) {

		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireNodeDisconnected(node);
		
	}

	public void nodeRemoved(NodeManagerEvent e) {

		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireNodeRemoved(node);
		
	}

	public void settingsChanged(NodeManagerEvent e) {
		
		Member node = e.getNode();
		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireNodeSettingsChanged(node);

	}

	public void startStop(NodeManagerEvent e) {

		NodeManager nodeManager = e.getNodeManager();
		nodeManager.fireStartStop();
		
	}

	public boolean fireInEventDispathThread() {
		return false;
	}

}
