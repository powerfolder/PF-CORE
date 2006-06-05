/* $Id: FolderTreeNode.java,v 1.2 2005/04/27 15:54:13 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;

/**
 * A Tree nodes which acts upon a folder.
 * <p>
 * Registers in folder as event listener an fires node events to update ui
 * <p>
 * TODO: Complete this
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class FolderTreeNode extends TreeNodeList {
//    private Controller controller;
//    private Folder folder;

    /**
     * @param userObject
     * @param parent
     */
    public FolderTreeNode(Controller controller, Folder folder) {
        // initalize with folder repo as root
        super(folder, controller.getFolderRepository().getJoinedFoldersTreeNode());
//        this.controller = controller;
//        this.folder = folder;
    }
}