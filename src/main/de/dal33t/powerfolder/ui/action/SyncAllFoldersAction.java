package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;

/**
 * Action to sync all folders.
 * 
 * @author Dante
 */
public class SyncAllFoldersAction extends BaseAction {

    public SyncAllFoldersAction(Controller controller) {
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
        if (getController().isSilentMode()) {
            log().warn("Disabling silent mode");
        }
        getController().setSilentMode(false);

        // Now trigger the scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requesting
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();
    }

    /**
     * (Copied from ScanFolderAction) Asks and performs the choosen sync action
     * on that folder
     * 
     * @param folder
     */
    private void askAndPerfomsSync(Folder folder) {
        // Open panel
        new SyncFolderPanel(getController(), folder).open();
    }
}
