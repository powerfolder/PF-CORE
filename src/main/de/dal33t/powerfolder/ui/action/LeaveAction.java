package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action which acts on selected folder. Leaves selected folder
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LeaveAction extends BaseAction {
    // new selection model
    private SelectionModel actionSelectionModel;

    public LeaveAction(Controller controller, SelectionModel selectionModel) {
        super("folderleave", controller);
        this.actionSelectionModel = selectionModel;
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

    // called if leave button clicked
    public void actionPerformed(ActionEvent e) {
        // selected folder
        Folder folder = (Folder) actionSelectionModel.getSelection();
        if (folder != null) {
            // create folderleave dialog message
            boolean syncFlag = folder.isSynchronizing();
            String folerLeaveText = null;
            if (syncFlag) {
                folerLeaveText = Translation.getTranslation(
                    "folderleave.dialog.text", folder.getInfo().name)
                    + "\n"
                    + Translation
                        .getTranslation("folderleave.dialog.sync_warning");
            } else {
                folerLeaveText = Translation.getTranslation(
                    "folderleave.dialog.text", folder.getInfo().name);
            }

            // show a confirm dialog
            int choice = JOptionPane.showConfirmDialog(getUIController()
                .getMainFrame().getUIComponent(), folerLeaveText, Translation
                .getTranslation("folderleave.dialog.title",
                    folder.getInfo().name), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.OK_OPTION) {
                getController().getPreferences().put(
                    "folder." + folder.getName() + ".last-localbase",
                    folder.getLocalBase().getAbsolutePath());
                // confirmed! remove folder!
                getController().getFolderRepository().removeFolder(folder);
            }
        }
    }
}