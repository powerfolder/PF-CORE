/* $Id: NodeInformation.java,v 1.4 2005/04/05 13:21:41 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Node information
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class NodeInformation extends Message {
    private static final long serialVersionUID = 101L;

    public MemberInfo node;
    public String programVersion;
    public String debugReport;

    /**
     * Constructs a node information
     * 
     * @param c
     */
    public NodeInformation(Controller c) {
        debugReport = c.getDebugReport();
        node = c.getMySelf().getInfo();
        programVersion = Controller.PROGRAM_VERSION;
    }

    public String toString() {
        return "Node information";
    }
}