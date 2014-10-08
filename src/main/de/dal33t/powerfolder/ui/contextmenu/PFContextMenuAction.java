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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;

/**
 * Base class for actions executed when a context menu item is clicked adding
 * some convenience methods. {@link ContextMenuAction}
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
abstract class PFContextMenuAction extends ContextMenuAction {

    private static final Logger log = Logger
        .getLogger(PFContextMenuAction.class.getName());

    private Controller controller;

    PFContextMenuAction(Controller controller) {
        this.controller = controller;
    }

    @Override
    public abstract void onSelection(String[] paths);

    /**
     * Retrieve FileInfos for the passed paths, if they exist.<br />
     * <br />
     * If a path is not found as FileInfo, it is ignored.
     * 
     * @param paths
     *            A list of Strings representing paths.
     * @return A List of FileInfos that represent the paths passed.
     */
    protected List<FileInfo> getFileInfos(String[] paths) {
        List<FileInfo> fileInfos = new ArrayList<>();

        for (String path : paths) {
            try {
                Path targetDir = Paths.get(path).toRealPath();

                for (Folder fo : controller.getFolderRepository().getFolders())
                {
                    if (targetDir.equals(fo.getLocalBase())) {
                        fileInfos.add(fo.getBaseDirectoryInfo());
                    } else if (targetDir.startsWith(fo.getLocalBase())) {
                        FileInfo lookup = FileInfoFactory.lookupInstance(fo,
                            targetDir);
                        FileInfo found = fo.getDAO().find(lookup, null);

                        if (found != null) {
                            fileInfos.add(found);
                        } else {
                            log.fine("No info found for " + targetDir);
                        }
                    }
                }
            } catch (IOException ioe) {
                log.fine("Could not check for file at: " + path + ". " + ioe);
            }
        }

        return fileInfos;
    }

    /**
     * Retrieve FolderInfos for the passed paths, if they exist.<br />
     * <br />
     * If a path is not found as FolderInfo, it is ignored.
     * 
     * @param paths
     *            A list of Strings representing paths.
     * @return A List of FolderInfos that represent the paths passed.
     */
    protected List<Folder> getFolders(String[] paths) {
        List<Folder> folders = new ArrayList<>();

        for (String path : paths) {
            try {
                Path targetDir = Paths.get(path).toRealPath();
                Folder folder = null;
                if ((folder = controller.getFolderRepository()
                    .findExistingFolder(targetDir)) != null)
                {
                    folders.add(folder);
                }
            } catch (IOException ioe) {
                log.fine("Could not check for folder at: " + path + ". " + ioe);
            }
        }

        return folders;
    }

    protected Controller getController() {
        return controller;
    }
}
