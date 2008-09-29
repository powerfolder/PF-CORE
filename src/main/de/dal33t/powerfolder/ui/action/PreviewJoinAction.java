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
        super("preview_join", controller);
        this.selectionModel = selectionModel;
        setEnabled(selectionModel.getSelection() != null);

        // Add behavior on selection model
        selectionModel.addSelectionChangeListener(new SelectionChangeListener()
        {
            public void selectionChanged(SelectionChangeEvent event) {
                Object selection = event.getSelection();
                if (selection instanceof Folder) {
                    // enable button if the folder is a preview
                    Folder folder = (Folder) selection;
                    setEnabled(folder.isPreviewOnly());
                } else {
                    // enable button if there is something selected
                    setEnabled(selection != null);
                }
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