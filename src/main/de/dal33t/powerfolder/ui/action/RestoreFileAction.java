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
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SwingWorker;

public class RestoreFileAction extends SelectionBaseAction {

    public RestoreFileAction(Controller controller,
        SelectionModel selectionModel) {
        super("restorefile", controller, selectionModel);
        setEnabled(false);
    }

    /**
     * Disenables restore file action if nothing is selected.
     * @param event
     */
    public void selectionChanged(SelectionChangeEvent event) {
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length > 0) {
            for (Object object : getSelectionModel().getSelections()) {
                if (object instanceof FileInfo) {
                    setEnabled(true);
                    return;
                }
            }
        }
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        SwingWorker worker = new ActivityVisualizationWorker(
                getController().getUIController().getMainFrame().getUIComponent()) {

            @Override
            protected String getTitle() {
                return Translation.getTranslation("restore.busy.title");
            }

            @Override
            protected String getWorkingText() {
                return Translation.getTranslation("restore.busy.description");
            }

            public Object construct() {
                boolean succes = true;
                Object[] selections = getSelectionModel().getSelections();
                for (Object selection : selections) {
                    if (selection instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selection;

                        //if (fileInfo.isDeleted()) {
                        RecycleBin recycleBin = getController()
                                .getRecycleBin();
                        if (recycleBin.isInRecycleBin(fileInfo)) {
                            if (!recycleBin.restoreFromRecycleBin(fileInfo)) {
                                succes = false;
                            }
                        }
                    }
                }
                return succes;
            }
        };

        // do in different thread
        worker.start();
    }
}
