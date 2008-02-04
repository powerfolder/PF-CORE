/* $Id: StartFileAction.java,v 1.12 2005/10/27 19:00:01 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * Action to start a file, currently only available on windows systems.
 * Only add to toolbar or menu if windows or MacOS.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class StartFileAction extends SelectionBaseAction {

    public StartFileAction(Controller controller, SelectionModel selectionModel)
    {
        super("openfile", controller, selectionModel);
        setEnabled(false);
    }

    public void selectionChanged(SelectionChangeEvent event) {
        setEnabled(true);
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length != 0) {
            // check if all are files (cannot open Dirs)
            for (int i = 0; i < selections.length; i++) {
                if (!(selections[i] instanceof FileInfo)) {
                    setEnabled(false);
                    break;
                }                
                //it is a FileInfo
                FileInfo fileInfo = (FileInfo) selections[i];   
                //check if file is local available
                if (!fileInfo.diskFileExists(getController())) {                 
                    setEnabled(false);
                    break;
                }
            }            
        } else {
            setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length > 0) {
            if (selections.length >= 10) {
                // TODO warn for opening more than 10 files?
            }
            for (int i = 0; i < selections.length; i++) {
                Object selection = selections[i];
                if (selection instanceof FileInfo && OSUtil.isWindowsSystem()) {
                    FileInfo fInfo = (FileInfo) selection;

                    if (fInfo.diskFileExists(getController())) {
                        File file = fInfo.getDiskFile(getController()
                            .getFolderRepository());
                        log().debug("Starting " + file.getAbsolutePath());
                        try {
                            FileUtils.executeFile(file);
                        } catch (IOException ex) {
                            unableToStart(fInfo, ex);
                        }
                    } else {
                        unableToStart(fInfo, "File not found");
                    }
                }
            }
        }
    }

    /**
     * Displays problem starting the file
     * 
     * @param fInfo
     * @param reason
     */
    private void unableToStart(FileInfo fInfo, Object reason) {
        // @todo add translation
        String text = "Unable to start\n" + fInfo.getName() + "\nReason: "
            + reason;
        DialogFactory.genericDialog(
                            getUIController().getMainFrame().getUIComponent(),
                "Unable to start", text, GenericDialogType.ERROR);
    }
}