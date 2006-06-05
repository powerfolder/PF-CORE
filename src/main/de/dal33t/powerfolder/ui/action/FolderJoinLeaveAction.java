/* $Id: FolderJoinLeaveAction.java,v 1.26 2006/03/10 19:18:23 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Action which acts on selected folder. Joins/Leaves selected folder of navtree
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.26 $
 */
public class FolderJoinLeaveAction extends SelectionBaseAction {
    public static final String FOLDER_JOIN_LEAVE = "folderjoinleave";

    /**
     * Create a folder join/leave/invitation action.
     * 
     * @param controller
     * @param selectionModel
     * @param toolbarIcons
     */
    public FolderJoinLeaveAction(Controller controller,
        SelectionModel selectionModel)
    {
        super(FOLDER_JOIN_LEAVE, controller, selectionModel);        
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();        
        // Set name depending on selection
        if (selection instanceof Folder) {
            putValue(Action.NAME, Translation.getTranslation(FOLDER_JOIN_LEAVE
                + ".leave"));            
        } else if (selection instanceof FolderDetails) {
            putValue(Action.SHORT_DESCRIPTION, Translation
                .getTranslation(FOLDER_JOIN_LEAVE + ".join"));            
        }
    }

    
    public void actionPerformed(ActionEvent e) {
        // check selected item on tree
        Object target = getUIController().getControlQuarter().getSelectedItem();
        if (target instanceof FolderDetails) {
            //join
            FolderDetails foDetails = (FolderDetails) target;
            FolderJoinPanel panel = new FolderJoinPanel(getController(),
                foDetails.getFolderInfo());
            panel.open();
        } else if (target instanceof Folder) {
            //leave
            Folder folder = (Folder) target;
            int choice = JOptionPane.showConfirmDialog(getUIController()
                .getMainFrame().getUIComponent(), Translation.getTranslation(
                "folderleave.dialog.text", folder.getInfo().name), Translation
                .getTranslation("folderleave.dialog.title",
                    folder.getInfo().name), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, Icons.LEAVE_FOLDER);
            if (choice == JOptionPane.OK_OPTION) {
                getController().getPreferences().put(
                    "folder." + folder.getName() + ".last-localbase",
                    folder.getLocalBase().getAbsolutePath());
                getController().getFolderRepository().removeFolder(folder);
            }
        }
    }
}