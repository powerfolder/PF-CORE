/* $Id: RequestBackup.java,v 1.1 2005/10/06 02:32:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * Message to request backup of a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class RequestBackup extends FolderRelatedMessage {
    private static final long serialVersionUID = 100L;

    public String toString() {
        return "BackupRequest on " + folder;
    }
}