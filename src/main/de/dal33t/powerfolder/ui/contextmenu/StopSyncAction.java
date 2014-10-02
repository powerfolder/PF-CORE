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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.disk.Folder;

/**
 * Leaving a folder
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class StopSyncAction extends ContextMenuAction {

    private static final Logger log = Logger.getLogger(StopSyncAction.class
        .getName());
    private Folder folder;

    StopSyncAction(Folder folder) {
        this.folder = folder;
    }

    @Override
    public void onSelection(String[] paths) {
        if (paths.length != 1) {
            log.info("More than one path for Folder");
            return;
        }

        String pathName = paths[0];
        Path path = Paths.get(pathName);

        if (!folder.getLocalBase().equals(path)) {
            log.info("Path is not equal to the Folder's local base");
        }

        folder.getController().getFolderRepository()
            .removeFolder(folder, true);
    }
}
