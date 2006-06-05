/* $Id: RequestNodeList.java,v 1.1 2005/05/07 19:56:51 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * A request to send the nodelist
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class RequestNodeList extends Message {
    private static final long serialVersionUID = 100L;

    public String toString() {
        return "Request for NodeList";
    }
}