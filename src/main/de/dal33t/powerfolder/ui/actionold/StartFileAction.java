/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.actionold;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Action to start a file, currently only available on windows systems.
 * Only add to toolbar or menu if windows or MacOS.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class StartFileAction extends SelectionBaseAction {

    private static final Logger log = Logger.getLogger(StartFileAction.class.getName());

    public StartFileAction(Controller controller, SelectionModel selectionModel)
    {
        super("open_file", controller, selectionModel);
        setEnabled(false);
    }

    public void selectionChanged(SelectionChangeEvent event) {
        setEnabled(true);
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length != 0) {
            // check if all are files (cannot open Dirs)
            for (Object selection : selections) {
                if (!(selection instanceof FileInfo)) {
                    setEnabled(false);
                    break;
                }
                //it is a FileInfo
                FileInfo fileInfo = (FileInfo) selection;
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
            for (Object selection : selections) {
                if (selection instanceof FileInfo) {
                    FileInfo fInfo = (FileInfo) selection;

                    if (fInfo.diskFileExists(getController())) {
                        File file = fInfo.getDiskFile(getController()
                            .getFolderRepository());
                        log.fine("Starting " + file.getAbsolutePath());
                        try {
                            FileUtils.openFile(file);
                        } catch (IOException ex) {
                            unableToStart(fInfo, ex);
                        }
                    } else {
                        unableToStart(fInfo, new IOException("File not found"));
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
    private void unableToStart(FileInfo fInfo, Throwable reason) {
        DialogFactory.genericDialog(
                            getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation("start.file.unable.title"),
                Translation.getTranslation("start.file.unable.text",
                        fInfo.getName()),
                getController().isVerbose(), reason);
    }
}