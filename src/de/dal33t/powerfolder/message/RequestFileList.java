/* $Id: RequestFileList.java,v 1.3 2004/10/04 00:41:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Requests all files of a folder TODO: request only those added since a date
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class RequestFileList extends FolderRelatedMessage
{
    private static final long serialVersionUID = 100L;

    public RequestFileList(FolderInfo fInfo) {
        this.folder = fInfo;
    }

    public String toString() {
        return "Request for filelist";
    }
}