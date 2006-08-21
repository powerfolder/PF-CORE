package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;

/**
 * Action to scan all folders. 
 * 
 * @author Dante
 *
 */
public class ScanAllFoldersAction extends BaseAction {

    public ScanAllFoldersAction(Controller controller) {
        super("scanallfolders", controller);
    }

    public void actionPerformed(ActionEvent arg0) {
        FolderRepository repo = getController().getFolderRepository();
        // Force scan on all folders, of repository was selected
        FolderInfo[] folders = repo.getJoinedFolderInfos();
        for (int i = 0; i < folders.length; i++) {
            Folder folder = repo.getFolder(folders[i]);
            if (folder != null) {
                // Ask for more sync options on that folder if on project sync
                if (folder.getSyncProfile() == SyncProfile.PROJECT_WORK) {
                    askAndPerfomsSync(folder);
                } else if (folder.getSyncProfile().isAutoDetectLocalChanges()) {
                    // Force scan on this
                    folder.forceScanOnNextMaintenance();
                }
            }
        }
        log().warn("Disable silent mode");
        getController().setSilentMode(false);

        // Now trigger the scan
        getController().getFolderRepository().triggerMaintenance();
    }
    
    /**
     * (Copied from ScanFolderAction)
     * Asks and performs the choosen sync action on that folder
     * 
     * @param folder
     */
    private void askAndPerfomsSync(Folder folder) {
        // Open panel
        new SyncFolderPanel(getController(), folder).open();
    }
}
