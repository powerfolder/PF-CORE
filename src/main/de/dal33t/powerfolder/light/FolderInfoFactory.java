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

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.StackDump;

import java.util.UUID;
import java.util.logging.Level;
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
        return new FolderInfo("", folderID, -1, null);
    }

    /**
     * @param folderID
     * @param name
     * @return a FolderInfo object use to lookup other FolderInfo instances by ID
     */
    public static FolderInfo lookupInstance(String folderID, String name) {
        return new FolderInfo(name, folderID, -1, null);
    }

    public static FolderInfo copyFrom(Folder folder) {
        return new FolderInfo(folder.getInfo().getName(),
                folder.getInfo().getId(),
                folder.getInfo().getVersion(),
                folder.getInfo().getParent());
    }

    public static FolderInfo newTopFolder(String name) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, null).intern();
    }

    // TODO Check if better use unmarshallExistingTopFolder
    public static FolderInfo newTopFolder(String id, String name) {
        return new FolderInfo(name, id, 0, null).intern();
    }

    public static FolderInfo newFolder(String name, DirectoryInfo parent) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, parent).intern();
    }

    public static FolderInfo newFolder(String id, String name, DirectoryInfo parent) {
        return new FolderInfo(name, id, 0, parent).intern();
    }

    public static FolderInfo backupFolderOfAccount(String name, AccountInfo aInfo)
    {
        return new FolderInfo(name, "PB-" + aInfo.getOID() + "-" + name, 0, null).intern();
    }

    public static FolderInfo unmarshallExistingTopFolder(String id, String name, int version) {
        return unmarshallExistingFolder(name, id, version, null);
    }

    public static FolderInfo unmarshallExistingFolder(String id, String name, int version, DirectoryInfo parent) {
        return new FolderInfo(name, id, version, parent).intern();
    }

    public static FolderInfo rename(FolderInfo originalFolderInfo, String newName) {
        if (originalFolderInfo.getName().equals(newName)) {
            return originalFolderInfo;
        }
        int version;
        if (originalFolderInfo.isLookupInstance()) {
            version = 0;
            LOG.log(Level.WARNING, originalFolderInfo + ": Renaming from lookup instance is discouraged, but used.", new StackDump());
        } else {
            version = originalFolderInfo.getVersion() + 1;
        }
        return new FolderInfo(
                newName,
                originalFolderInfo.getId(),
                version,
                originalFolderInfo.getParent()
        ).intern(true);
    }

    // TODO: Read/Write as file

    public static FolderInfo newRandomTopFolderForTest() {
        return unmarshallExistingTopFolder(
                IdGenerator.makeFolderId(),
                "TestFolder / " + UUID.randomUUID(),
                (int) (1000000L * Math.random()));
    }

    public static FolderInfo newTopFolderForTest(String name) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, null);
    }

    public static FolderInfo backupFolderOfAccountForTest(String name, AccountInfo aInfo) {
        return backupFolderOfAccountForTest(name, aInfo);
    }
}
