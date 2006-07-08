package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SwingWorker;

public class RestoreFileAction extends SelectionBaseAction {

    public RestoreFileAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("restorefile", controller, selectionModel);
        setEnabled(false);
    }

    public void selectionChanged(SelectionChangeEvent event) {
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length != 0) {
            // check if all are deleted files and file exists in recycle bin and
            // not is a Directory (cannot restore Dirs, well they are not in the
            // recycle bin anyway)
            setEnabled(true);

            for (int i = 0; i < selections.length; i++) {
                if (selections[i] instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) selections[i];

                    if (fileInfo.isDeleted()) {
                        if (!getController().getRecycleBin().isInRecycleBin(
                            fileInfo))
                        {
                            setEnabled(false);
                        }
                    } else {
                        setEnabled(false);
                        break;
                    }
                } else {
                    setEnabled(false);
                    break;
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                boolean succes = true;
                Object[] selections = getSelectionModel().getSelections();
                for (int i = 0; i < selections.length; i++) {
                    if (selections[i] instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selections[i];

                        if (fileInfo.isDeleted()) {
                            RecycleBin recycleBin = getController()
                                .getRecycleBin();
                            if (recycleBin.isInRecycleBin(fileInfo)) {
                                if (!recycleBin.restoreFromRecycleBin(fileInfo))
                                {
                                    succes = false;
                                }
                            }
                        }
                    }
                }
                return Boolean.valueOf(succes);
            }
        };
        // do in different thread
        worker.start();
    }
}
