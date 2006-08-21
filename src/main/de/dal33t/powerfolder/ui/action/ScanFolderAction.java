/* $Id: ScanFolderAction.java,v 1.11 2006/02/16 13:58:27 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;

/**
 * Action to manually scan a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class ScanFolderAction extends BaseAction {

    public ScanFolderAction(Controller controller) {
        super("scanfolder", controller);
        // Override icon
        putValue(Action.SMALL_ICON, null);
    }

    public void actionPerformed(ActionEvent e) {
        Object selectedItem = getUIController().getControlQuarter()
            .getSelectedItem();
        if (selectedItem instanceof Folder) {
            Folder folder = (Folder) selectedItem;

            // Ask for more sync options on that folder if on project sync
            if (folder.getSyncProfile() == SyncProfile.PROJECT_WORK) {
                askAndPerfomsSync(folder);
            } else if (folder.getSyncProfile().isAutoDetectLocalChanges()) {
                // Force scan on this
                folder.forceScanOnNextMaintenance();
            }
        }

        log().warn("Disable silent mode");
        getController().setSilentMode(false);

        // Now trigger the scan
        getController().getFolderRepository().triggerMaintenance();
    }

    /**
     * Asks and performs the choosen sync action on that folder
     * 
     * @param folder
     */
    private void askAndPerfomsSync(Folder folder) {
        // Open panel
        new SyncFolderPanel(getController(), folder).open();
    }

}