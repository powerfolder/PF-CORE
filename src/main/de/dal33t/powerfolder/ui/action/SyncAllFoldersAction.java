package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.Reject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Action to sync all folders.
 * 
 * @author Dante
 */
public class SyncAllFoldersAction extends BaseAction {

    public SyncAllFoldersAction(Controller controller) {
        super("scanallfolders", controller);
        putValue(ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    }

    public void actionPerformed(ActionEvent e) {
        perfomSync(getController());
    }

    /**
     * Perfoms the sync on all folders.
     * 
     * @param controller
     *            the controller
     */
    public static void perfomSync(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        FolderRepository repo = controller.getFolderRepository();

        // Let other nodes scan now!
        repo.broadcastScanCommandOnAllFolders();

        // Force scan on all folders, of repository was selected
        Folder[] folders = repo.getFolders();
        for (Folder folder : folders) {
            if (folder != null) {
                // Ask for more sync options on that folder if on project sync
                if (folder.getSyncProfile().equals(SyncProfile.PROJECT_WORK)) {
                    askAndPerfomsSync(folder);
                } else {
                    // Recommend scan on this
                    folder.recommendScanOnNextMaintenance();
                }
            }
        }

        controller.setSilentMode(false);

        // Now trigger the scan
        controller.getFolderRepository().triggerMaintenance();

        // Trigger file requesting
        controller.getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Fresh reconnection try!
        controller.getReconnectManager().buildReconnectionQueue();
    }

    /**
     * (Copied from ScanFolderAction) Asks and performs the choosen sync action
     * on that folder
     * 
     * @param folder
     */
    private static void askAndPerfomsSync(Folder folder) {
        // Open panel
        new SyncFolderPanel(folder.getController(), folder).open();
    }
}
