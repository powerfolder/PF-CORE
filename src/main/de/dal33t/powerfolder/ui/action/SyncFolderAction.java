/* $Id: ScanFolderAction.java,v 1.11 2006/02/16 13:58:27 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action to manually sync a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class SyncFolderAction extends SelectionBaseAction {

    public SyncFolderAction(Controller controller, SelectionModel model) {
        super("scanfolder", controller, model);
        // Override icon
        putValue(Action.SMALL_ICON, null);
    }

    public void actionPerformed(ActionEvent e) {
        Object selectedItem = getSelectionModel().getSelection();
        if (!(selectedItem instanceof Folder)) {
            return;
        }
        Folder folder = (Folder) selectedItem;

        // Let other nodes scan now!
        folder.broadcastScanCommand();
        
        // Ask for more sync options on that folder if on project sync
        if (folder.getSyncProfile() == SyncProfile.PROJECT_WORK) {
            askAndPerfomsSync(folder);
        } else if (folder.getSyncProfile().isAutoDetectLocalChanges()) {
            // Force scan on this
            folder.forceScanOnNextMaintenance();
        }

        log().debug("Disable silent mode");
        getController().setSilentMode(false);

        // Now trigger the scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requesting (trigger all folders, doesn't matter)
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();
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

    public void selectionChanged(SelectionChangeEvent event) {
    }
}