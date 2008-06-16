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
import de.dal33t.powerfolder.ui.dialog.PreviewFolderRemovePanel;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import java.awt.event.ActionEvent;

/**
 * Action which acts on selected preview folder. Removes selected folder from PF
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class PreviewFolderRemoveAction extends BaseAction {
    // new selection model
    private SelectionModel actionSelectionModel;

    public PreviewFolderRemoveAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("preview_folder_remove", controller);
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
            PreviewFolderRemovePanel flp = new PreviewFolderRemovePanel(this,
                getController(), folder);
            flp.open();
        }
    }

    /**
     * Called from FolderLeave Panel if the folder leave is confirmed.
     * 
     * @param deleteSystemSubFolder
     *            whether to delete hte .PowerFolder directory *
     * @param removeFromOS
     *            if the folder and files should be removed from the Online
     *            Storage
     */
    public void confirmedFolderLeave(boolean deleteSystemSubFolder,
        boolean removeFromOS)
    {
        Folder folder = (Folder) actionSelectionModel.getSelection();
        getController().getFolderRepository().removeFolder(folder,
            deleteSystemSubFolder);
        if (removeFromOS) {
            if (getController().getOSClient().hasJoined(folder)) {
                getController().getOSClient().getFolderService().removeFolder(
                    folder.getInfo(), true);
            } else {
                getController().getOSClient().getFolderService().revokeAdmin(
                    folder.getInfo());
            }
        }
    }
}