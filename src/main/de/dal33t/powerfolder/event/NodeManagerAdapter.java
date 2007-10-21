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
