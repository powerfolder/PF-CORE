package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.ui.*;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionEvent;

/**
 * Action which acts on selected folder. Changes a folder from preview to join.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.5 $
 */
public class PreviewJoinAction extends BaseAction {

    // new selection model
    private SelectionModel selectionModel;

    public PreviewJoinAction(Controller controller, SelectionModel selectionModel) {
        super("previewjoin", controller);
        this.selectionModel = selectionModel;
        setEnabled(selectionModel.getSelection() != null);

        // Add behavior on selection model
        selectionModel.addSelectionChangeListener(new SelectionChangeListener()
        {
            public void selectionChanged(SelectionChangeEvent event) {
                Object selection = event.getSelection();
                // enable button if there is something selected
                setEnabled(selection != null);
            }
        });
    }

    /**
     * Move frolder from preview to normal (my folders)
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        Folder folder = (Folder) selectionModel.getSelection();
        if (folder != null) {
            FolderInfo foInfo = folder.getInfo();
            FolderSettings settings = new FolderSettings(folder.getLocalBase(),
                    folder.getSyncProfile(),
                    false,
                    folder.isUseRecycleBin(),
                    false);
            FolderRepository folderRepository = getController().getFolderRepository();
            folderRepository.removeFolder(folder, false);
            try {
                folderRepository.createFolder(foInfo, settings);
            } catch (FolderException e1) {
                e1.show(getController(), Translation
                            .getTranslation("folderrepository.please_recreate"));
            }
        }
    }
}