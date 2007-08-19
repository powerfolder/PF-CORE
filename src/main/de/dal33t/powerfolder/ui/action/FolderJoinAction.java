package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action which acts on selected folder. Joins selected folder
 * 
 * @version $Revision: 1.3 $
 */
public class FolderJoinAction extends BaseAction {
    // new selection model
    private SelectionModel selectionModel;

    public FolderJoinAction(Controller controller, SelectionModel selectionModel) {
        super("folderjoin", controller);
        this.selectionModel = selectionModel;
        setEnabled(false);

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

    // called if join button clicked
    public void actionPerformed(ActionEvent e) {
        Object selection = selectionModel.getSelection();
        FolderInfo folderInfo = null;
        if (selection instanceof FolderInfo) {
            // selected folder
            folderInfo = (FolderInfo) selection;
        } else if (selection instanceof FolderDetails) {
            folderInfo = ((FolderDetails) selection).getFolderInfo();
        } else if (selection instanceof FileInfo) {
            // selected File
            FileInfo fileInfo = (FileInfo) selection;
            // selected folder
            folderInfo = fileInfo.getFolderInfo();
        }
        
        if (folderInfo != null) {
            FolderJoinPanel panel = new FolderJoinPanel(getController(),
                folderInfo);
            panel.open();
        }
    }
}