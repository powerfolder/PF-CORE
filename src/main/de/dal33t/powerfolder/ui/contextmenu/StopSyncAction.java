/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.contextmenu;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.dialog.FolderRemoveDialog;

/**
 * Leaving a folder
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class StopSyncAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(StopSyncAction.class
        .getName());

    StopSyncAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        try {
            List<Folder> folders = getFolders(paths);

            for (Folder folder : folders) {
                log.fine("Stopping sync of local folder " + folder);
                FolderRemoveDialog panel = new FolderRemoveDialog(getController(),
                    folder.getInfo());
                panel.open();
            }
        } catch (RuntimeException re) {
            log.log(Level.WARNING, "Problem while trying to stop sync. " + re,
                re);
        }
    }
}
