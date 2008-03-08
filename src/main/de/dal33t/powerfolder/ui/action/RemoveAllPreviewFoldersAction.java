package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.ui.*;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionEvent;

/**
 * Action which acts on removes all preview folders
 *
 * @author <a href="mailto:hglasgow@powerfolder.comm">Harry Glasgow</a>
 * @version $Revision: 2.3 $
 */
public class RemoveAllPreviewFoldersAction extends BaseAction {

    private FolderRepository folderRepository;

    public RemoveAllPreviewFoldersAction(Controller controller) {
        super("remove_all_preview_folders", controller);
        folderRepository = getController().getFolderRepository();

        // Enable if prevew foldres exists
        getController().getFolderRepository().addFolderRepositoryListener(
                new RepositoryListener());
        enableOnPreviewFolders();
    }

    // Called if remove button clicked
    public void actionPerformed(ActionEvent e) {
        int result = DialogFactory.genericDialog(getController().getUIController()
                .getMainFrame().getUIComponent(),
                Translation.getTranslation("remove_previews_action.title"),
                Translation.getTranslation("remove_previews_action.text"),
                new String[]{
                        Translation.getTranslation("remove_previews_action.remove"),
                        Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.QUESTION);
        if (result == 0) { // Remove
            getController().getFolderRepository().removeAllPreviewFolders();
        }
    }

    /**
     * Enable only if previewFolders exist.
     */
    private void enableOnPreviewFolders() {
        setEnabled(!folderRepository.getPreviewFoldersAsSortedList().isEmpty());
    }

    /**
     * Listen for changes to preview folders.
     */
    private class RepositoryListener implements FolderRepositoryListener {
        public boolean fireInEventDispathThread() {
            return true;
        }

        public void folderCreated(FolderRepositoryEvent e) {
            enableOnPreviewFolders();
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            enableOnPreviewFolders();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }
    }

}