package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.dialog.FolderLeavePanel;
import de.dal33t.powerfolder.util.ui.*;

import java.awt.event.ActionEvent;

/**
 * Action which acts on selected folder. Changes a folder from preview to join.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.5 $
 */
public class PreviewJoinAction extends BaseAction {

    public PreviewJoinAction(Controller controller, SelectionModel selectionModel) {
        super("previewjoin", controller);
    }

    public void actionPerformed(ActionEvent e) {
        DialogFactory.genericDialog(getController().getUIController().getMainFrame().getUIComponent(),
                "Preview Join Action",
                "Not yet implemented :-)",
                GenericDialogType.INFO);
    }
}