package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

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
        if (folder != null)
        {
            PreviewToJoinPanel p = new PreviewToJoinPanel(getController(),
                    folder);
            p.open();
        }
    }
}