/*
 * Copyright 2004 - 2020 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.util.IdGenerator;

import java.util.logging.Logger;

/**
 * Factory to create {@link FolderInfo} objects.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 */
public class FolderInfoFactory {
    private static final Logger LOG = Logger.getLogger(FileInfoFactory.class
            .getName());

    private FolderInfoFactory() {
        // No instance allowed
    }

    /**
     * @param folderID
     * @return a FolderInfo object use to lookup other FolderInfo instances by ID
     */
    public static FolderInfo lookupInstance(String folderID) {
        return new FolderInfo("", folderID);
    }

    /**
     * @param folderID
     * @param name
     * @return a FolderInfo object use to lookup other FolderInfo instances by ID
     */
    public static FolderInfo lookupInstance(String folderID, String name) {
        return new FolderInfo(name, folderID);
    }

    public static FolderInfo newFolder(String name, DirectoryInfo parent) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, parent);
    }

    public static FolderInfo rename(FolderInfo originalFolderInfo) {
        return new FolderInfo(
                originalFolderInfo.getName(),
                originalFolderInfo.getId(),
                originalFolderInfo.getVersion() + 1,
                originalFolderInfo.getParent()
        );
    }
}
