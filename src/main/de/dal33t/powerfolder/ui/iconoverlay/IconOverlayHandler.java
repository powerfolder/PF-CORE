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
package de.dal33t.powerfolder.ui.iconoverlay;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.liferay.nativity.modules.fileicon.FileIconControlCallback;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Determine the correct overlay to be displayed for a file.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IconOverlayHandler extends PFComponent implements
    FileIconControlCallback
{
    public IconOverlayHandler(Controller controller) {
        super(controller);
    }

    @Override
    public int getIconForFile(String pathName) {
        if (StringUtils.isBlank(pathName)) {
            logFine("No path name passed");
            return IconOverlayIndex.NO_OVERLAY.getIndex();
        }
        return getIconForFile(Paths.get(pathName));
    }


    public int getIconForFile(Path path) {
        if (path == null) {
            logFine("No path name passed");
            return IconOverlayIndex.NO_OVERLAY.getIndex();
        }
        
        try {
            // First check, if the path is associated with any folder ...
            FolderRepository fr = getController().getFolderRepository();

            Path basepath = fr.getFoldersBasedir();
            boolean isLnkFile = false;
            if (path.getFileName() != null) {
                isLnkFile = path.getFileName().toString()
                    .endsWith(Constants.LINK_EXTENSION);
            }

            if (basepath.equals(path.getParent()) && Files.isRegularFile(path)
                && !isLnkFile)
            {
                if (Constants.GETTING_STARTED_GUIDE_FILENAME.equals(path
                    .getFileName().toString())
                    || PathUtils.DESKTOP_INI_FILENAME.equals(path.getFileName()
                        .toString()))
                {
                    return IconOverlayIndex.NO_OVERLAY.getIndex();
                }
                return IconOverlayIndex.WARNING_OVERLAY.getIndex();
            }

            Folder folder;
            if (isLnkFile) {
                String name = path.getFileName().toString();
                String fName = name.split("\\.")[0];
                folder = fr.findExistingFolder(fName);
                if (folder != null) {
                    path = folder.getLocalBase();
                }
            } else {
                folder = fr.findContainingFolder(path);
            }
            if (folder == null) {
                return IconOverlayIndex.NO_OVERLAY.getIndex();
            }

            // ... then see, if it is part of a meta-folder.
            if (path.toString().contains(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                return IconOverlayIndex.NO_OVERLAY.getIndex();
            }

            // We know, it is a file in a Folder, so create a lookup instance
            // ...
            FileInfo lookup = FileInfoFactory.lookupInstance(folder, path);
            SyncStatus status = SyncStatus.of(getController(), lookup, folder);

            // Pick the apropriate icon overlay
            switch (status) {
                case SYNC_OK :
                    return IconOverlayIndex.OK_OVERLAY.getIndex();
                case SYNCING :
                    return IconOverlayIndex.SYNCING_OVERLAY.getIndex();
                case IGNORED :
                    return IconOverlayIndex.IGNORED_OVERLAY.getIndex();
                case LOCKED :
                    return IconOverlayIndex.LOCKED_OVERLAY.getIndex();
                case WARNING :
                    return IconOverlayIndex.WARNING_OVERLAY.getIndex();
                case NONE :
                default :
                    return IconOverlayIndex.NO_OVERLAY.getIndex();
            }
        } catch (RuntimeException re) {
            logSevere("An error occured while determining the icon overlay for file '"
                + path.toString() + "'. " + re);
            re.printStackTrace();
            return IconOverlayIndex.NO_OVERLAY.getIndex();
        }
    }
}
