package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Interface defines a callback to something that can decide if a node should be
 * added to the internal database.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface NodeFilter {
    /**
     * Answers if this node should be added to the internal node database.
     * 
     * @param nodeInfo
     *            the node to be added
     * @return true if this node should be added to the internal node database.
     */
    boolean shouldAddNode(MemberInfo nodeInfo);
}
