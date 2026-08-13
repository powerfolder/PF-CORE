/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.StackDump;

import java.nio.file.Path;
import java.util.Objects;
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

    // TODO Really needed?
    public static FolderInfo copyFrom(Folder folder) {
        return new FolderInfo(folder.getInfo().getName(),
                folder.getInfo().getId(),
                folder.getInfo().getVersion(),
                folder.getInfo().getParent());
    }

    public static FolderInfo newTopFolder(String name) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, null).intern();
    }

    // TODO Check if better use unmarshallExistingTopFolder, retrieve from database or FolderRepository
    public static FolderInfo newTopFolder(String id, String name) {
        return new FolderInfo(name, id, 0, null).intern();
    }

    public static FolderInfo newFolder(DirectoryInfo subdir) {
        return new FolderInfo(subdir.getFilenameOnly(), IdGenerator.makeFolderId(), 0, subdir.getParent()).intern();
    }

    public static FolderInfo newFolder(String id, String name, DirectoryInfo parent) {
        return new FolderInfo(name, id, 0, parent).intern();
    }

    // TODO Really needed?
    public static FolderInfo proxyFolder(String id, String name) {
        return new FolderInfo(name, id, 0, null).intern();
    }

    public static FolderInfo backupFolderOfAccount(String name, AccountInfo aInfo) {
        return new FolderInfo(name, "PB-" + aInfo.getOID() + "-" + name, 0, null).intern();
    }

    public static FolderInfo unmarshallExistingTopFolder(String id, String name, int version) {
        return new FolderInfo(name, id, version, null).intern();
    }

    public static FolderInfo unmarshallExistingFolder(String id, String name, int version, DirectoryInfo parent) {
        return new FolderInfo(name, id, version, parent).intern();
    }

    public static FolderInfo resolveConflict(FolderInfo originalFolderInfo) {
        int version;
        if (originalFolderInfo.isLookupInstance()) {
            version = 0;
            LOG.log(Level.WARNING, originalFolderInfo + ": Renaming from lookup instance is discouraged, but used.", new StackDump());
        } else {
            version = originalFolderInfo.getVersion() + 1;
        }
        // Tags and inheritance flag travel via the constructor (immutable FolderInfo), otherwise
        // the conflict bump would silently drop them. PFS-5306
        FolderInfo result = new FolderInfo(
                originalFolderInfo.getName(),
                originalFolderInfo.getId(),
                version,
                originalFolderInfo.getParent(),
                originalFolderInfo.storedTags(),
                originalFolderInfo.storedInheritsPermissions()
        );
        return result.intern(true);
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
        // Tags and inheritance flag travel via the constructor (immutable FolderInfo), otherwise
        // a rename would silently drop them. PFS-5306
        FolderInfo result = new FolderInfo(
                newName,
                originalFolderInfo.getId(),
                version,
                originalFolderInfo.getParent(),
                originalFolderInfo.storedTags(),
                originalFolderInfo.storedInheritsPermissions()
        );
        return result.intern(true);
    }

    public static FolderInfo changeParent(FolderInfo originalFolderInfo, DirectoryInfo newParent) {
        if (Objects.equals(originalFolderInfo.getParent(), newParent)) {
            return originalFolderInfo;
        }
        int version;
        if (originalFolderInfo.isLookupInstance()) {
            version = 0;
            LOG.log(Level.WARNING, originalFolderInfo + ": Change parent from lookup instance is discouraged, but used.", new StackDump());
        } else {
            version = originalFolderInfo.getVersion() + 1;
        }
        // Tags and inheritance flag travel via the constructor (immutable FolderInfo), otherwise
        // a move would silently drop them. PFS-5306
        FolderInfo result = new FolderInfo(
                originalFolderInfo.getName(),
                originalFolderInfo.getId(),
                version,
                newParent,
                originalFolderInfo.storedTags(),
                originalFolderInfo.storedInheritsPermissions()
        );
        return result.intern(true);
    }

    /**
     * PFC-3543: Changes the "inherits permissions" flag of a (sub)folder. Like a
     * rename this does not mutate the given instance but produces a new, version-
     * bumped {@link FolderInfo} so the change propagates through the network
     * (folder version is part of the sync protocol). This is the only supported
     * way to change the flag.
     *
     * @param originalFolderInfo  the folder to change
     * @param inheritsPermissions {@code false} to interrupt inheritance
     * @return the original instance if the flag is unchanged, otherwise a new
     *         version-bumped instance with the flag applied
     */
    public static FolderInfo changeInheritsPermissions(FolderInfo originalFolderInfo,
        boolean inheritsPermissions)
    {
        if (originalFolderInfo.storedInheritsPermissions() == inheritsPermissions) {
            return originalFolderInfo;
        }
        int version;
        if (originalFolderInfo.isLookupInstance()) {
            version = 0;
            LOG.log(Level.WARNING, originalFolderInfo
                + ": Changing inheritsPermissions from lookup instance is discouraged, but used.",
                new StackDump());
        } else {
            version = originalFolderInfo.getVersion() + 1;
        }
        // Tags travel via the constructor as well (immutable FolderInfo). PFS-5306
        FolderInfo result = new FolderInfo(
                originalFolderInfo.getName(),
                originalFolderInfo.getId(),
                version,
                originalFolderInfo.getParent(),
                originalFolderInfo.storedTags(),
                inheritsPermissions
        );
        return result.intern(true);
    }

    /**
     * PFS-5306: Changes the tags of a folder. Like a rename this does
     * not mutate the given instance but produces a new, version-bumped
     * {@link FolderInfo} so the change propagates through the network (folder
     * version is part of the sync protocol). This is the only supported way to
     * change the tags.
     *
     * @param foInfo   the folder to change
     * @param tagsJson the tags as JSON array string, {@code null} to untag
     * @return the original instance if the tags are unchanged, otherwise a new
     *         version-bumped instance with the tags applied
     */
    public static FolderInfo changeTags(FolderInfo foInfo, String tagsJson) {
        if (Objects.equals(foInfo.storedTags(), tagsJson)) {
            return foInfo;
        }
        int version;
        if (foInfo.isLookupInstance()) {
            version = 0;
            LOG.log(Level.WARNING, foInfo + ": Changing tags from lookup instance is discouraged, but used.",
                new StackDump());
        } else {
            version = foInfo.getVersion() + 1;
        }
        // Inheritance flag travels via the constructor as well (immutable FolderInfo). PFC-3543
        FolderInfo result = new FolderInfo(
                foInfo.getName(),
                foInfo.getId(),
                version,
                foInfo.getParent(),
                tagsJson,
                foInfo.storedInheritsPermissions()
        );
        return result.intern(true);
    }

    // Persistence ------------------------------------------------------------

    public static FolderInfo readFrom(Path folderBasePath) {
        Path file = folderBasePath.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR).resolve("FolderInfo");
        return FolderInfo.load(file);
    }

    public static FolderInfo readFrom(Folder folder) {
        Path file = folder.getSystemSubDir().resolve("FolderInfo");
        return FolderInfo.load(file);
    }

    public static boolean writeFolderInfo(Folder folder) {
        Path file = folder.getSystemSubDir().resolve("FolderInfo");
        return folder.getInfo().save(file);
    }

    // For tests --------------------------------------------------------------

    public static FolderInfo newRandomTopFolderForTest() {
        return unmarshallExistingTopFolder(
                IdGenerator.makeFolderId(),
                "TestFolder / " + UUID.randomUUID(),
                (int) (1000000L * Math.random()));
    }

    public static FolderInfo newTopFolderForTest(String name) {
        return new FolderInfo(name, IdGenerator.makeFolderId(), 0, null);
    }

    public static FolderInfo newTopFolderForTest(String name, String id) {
        return new FolderInfo(name, id, 0, null);
    }

    public static FolderInfo backupFolderOfAccountForTest(String name, AccountInfo aInfo) {
        return backupFolderOfAccount(name, aInfo);
    }
}
