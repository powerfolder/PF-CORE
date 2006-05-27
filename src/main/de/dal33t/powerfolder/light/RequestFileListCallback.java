/* $Id: RequestFileListCallback.java,v 1.1 2005/06/29 04:24:01 totmacherr Exp $
 */
package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.FileList;

/**
 * Callback, used to inform requestor of about received filelists from nodes
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public interface RequestFileListCallback {
    /**
     * Informs that a filelist has been received from a node
     * 
     * @param from
     * @param filelist
     */
    public void fileListReceived(Member from, FileList filelist);

    /**
     * Informs that the request is over. No more filelist can be expected
     */
    public void fileListRequestOver();
}