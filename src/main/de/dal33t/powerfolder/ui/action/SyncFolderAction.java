/* $Id: ScanFolderAction.java,v 1.11 2006/02/16 13:58:27 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import de.dal33t.powerfolder.Controller;

import javax.swing.*;

/**
 * Action to manually sync a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class SyncFolderAction extends BaseAction {

    public SyncFolderAction(Controller controller) {
        super("scanfolder", controller);
        // Override icon
        putValue(SMALL_ICON, null);

        // Note: the accelerator is not actually used here;
        // It just puts the Alt-1 text on the Nav tree pop-up item.
        // See MainFrame.MySyncFolderAction.
        putValue(ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
        getController().getUIController().getFolderRepositoryModel().scanSelectedFolder();
    }
}