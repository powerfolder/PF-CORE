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

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Set file to be "NOT in use for edit" to display a message to the user.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class UnlockAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(UnlockAction.class.getName());

    UnlockAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        List<FileInfo> fileInfos = getFileInfos(paths);

        for (FileInfo fileInfo : fileInfos) {
            unlockFileInfos(fileInfo);
        }
    }

    private void unlockFileInfos(FileInfo fileInfo) {
        if (fileInfo.isDiretory()) {
            DirectoryInfo dInfo = (DirectoryInfo) fileInfo;

            FileInfoCriteria criteria = new FileInfoCriteria();
            criteria.addMySelf(fileInfo.getFolder(getController()
                .getFolderRepository()));
            criteria.setPath(dInfo);
            criteria.setRecursive(true);

            Collection<FileInfo> infos = dInfo
                .getFolder(getController().getFolderRepository()).getDAO()
                .findFiles(criteria);

            for (FileInfo info : infos) {
                unlock(info);
            }
        }

        unlock(fileInfo);
    }

    private void unlock(FileInfo fileInfo) {
        if (fileInfo.isLocked(getController())) {
            if (!fileInfo.unlock(getController())) {
                log.warning("File " + fileInfo + " could not be unlocked!");
            }
        }
    }
}
