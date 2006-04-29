/* $Id: RequestNetworkFolderList.java,v 1.2 2006/01/28 16:33:25 totmacherr Exp $
 * 
 * Copyright (c) DAKOSY AG and Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Message for requesting the folderlist from a supernode
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class RequestNetworkFolderList extends Message {
    private static final long serialVersionUID = 100L;

    /**
     * Instance to request complete network folder list
     */
    public static final RequestNetworkFolderList COMPLETE_LIST = new RequestNetworkFolderList();

    /**
     * Filered folders, if null all folders are requested, otherwise only those
     * in list
     */
    public FolderInfo[] folders;

    /**
     * Answers if the complete list should be transferred
     * 
     * @return
     */
    public boolean completeList() {
        return folders == null || folders.length == 0;
    }

    public String toString() {
        return "Request for Network Folderlist. "
            + (completeList()
                ? "Complete list"
                : ("For " + folders.length + " Folders"));
    }
}
