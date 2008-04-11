package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderPreviewHelper;
import de.dal33t.powerfolder.ui.dialog.FolderLeavePanel;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import java.awt.event.ActionEvent;

/**
 * Action which acts on selected folder. Leaves selected folder
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderLeaveAction extends BaseAction {
    // new selection model
    private SelectionModel actionSelectionModel;

    public FolderLeaveAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("folder_leave", controller);
        actionSelectionModel = selectionModel;
        setEnabled(actionSelectionModel.getSelection() != null);

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

    // Called if leave button clicked
    public void actionPerformed(ActionEvent e) {
        // selected folder
        Folder folder = (Folder) actionSelectionModel.getSelection();
        if (folder != null) {
            // show a confirm dialog
            FolderLeavePanel flp = new FolderLeavePanel(this, getController(),
                folder);
            flp.open();
        }
    }

    /**
     * Called from FolderLeave Panel if the folder leave is confirmed.
     * 
     * @param deleteSystemSubFolder
     *            whether to delete hte .PowerFolder directory
     * @param convertToPreview
     *            Change back to a preview
     * @param removeFromOS
     *            if the folder and files should be removed from the Online
     *            Storage
     */
    public void confirmedFolderLeave(boolean deleteSystemSubFolder,
        boolean convertToPreview, boolean removeFromOS)
    {
        Folder folder = (Folder) actionSelectionModel.getSelection();
        if (convertToPreview) {
            FolderPreviewHelper.convertFolderToPreview(getController(), folder,
                deleteSystemSubFolder);
        } else {
            getController().getFolderRepository().removeFolder(folder,
                deleteSystemSubFolder);
        }
        if (removeFromOS) {
            getController().getOSClient().getFolderService().removeFolder(
                folder.getInfo(), true);
        }
    }
}