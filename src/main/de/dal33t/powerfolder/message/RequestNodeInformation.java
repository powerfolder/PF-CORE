/* $Id: RequestNodeInformation.java,v 1.3 2004/10/04 00:41:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * Node information request
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class RequestNodeInformation extends Message {
    private static final long serialVersionUID = 100L;

    public String toString() {
        return "Request for node information";
    }
}