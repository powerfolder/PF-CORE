package de.dal33t.powerfolder.event;

/**
 * Implement this class to receive events from the NodeManager.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom </A>
 * @version $Revision: 1.2 $
 */
public interface NodeManagerListener {
    public void nodeRemoved(NodeManagerEvent e);

    public void nodeAdded(NodeManagerEvent e);

    public void nodeConnected(NodeManagerEvent e);

    public void nodeDisconnected(NodeManagerEvent e);

    public void friendAdded(NodeManagerEvent e);

    public void friendRemoved(NodeManagerEvent e);
    
    public void settingsChanged(NodeManagerEvent e);
}