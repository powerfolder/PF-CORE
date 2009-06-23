/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: FileInfo.java 8176 2009-06-10 13:21:06Z bytekeeper $
 */
package de.dal33t.powerfolder.light;

import java.io.File;
import java.util.Date;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.Reject;

/**
 * Factory to create {@link FileInfo} and {@link DirectoryInfo} objects.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 */
public final class FileInfoFactory {
    private FileInfoFactory() {
        // No instance allowed
    }

    public static FileInfo lookupInstance(FolderInfo folder, String name) {
        // TODO DIRECTORY
        return new FileInfo(folder, name);
    }

    public static FileInfo lookupInstance(Folder folder, File file) {
        String fn = buildFileName(folder, file);
        return lookupInstance(folder.getInfo(), fn);
    }

    public static FileInfo unmarshallExistingFile(FolderInfo fi,
        String fileName, long size, MemberInfo modby, Date modDate, int version)
    {
        // TODO DIRECOTRY
        return new FileInfo(fileName, size, modby, modDate, version, false, fi);
    }

    public static FileInfo unmarshallDelectedFile(FolderInfo fi,
        String fileName, MemberInfo modby, Date modDate, int version)
    {
        // TODO DIRECTORY
        return new FileInfo(fileName, 0, modby, modDate, version, true, fi);
    }

    /**
     * Initalize within a folder
     * 
     * @param folder
     * @param localFile
     * @param creator
     * @return the new file
     */
    public static FileInfo newFile(Folder folder, File localFile,
        MemberInfo creator)
    {
        if (localFile.isFile()) {
            return new FileInfo(buildFileName(folder, localFile), localFile
                .length(), creator, new Date(localFile.lastModified()), 0,
                false, folder.getInfo());
        } else if (localFile.isDirectory()) {
            return new DirectoryInfo(buildFileName(folder, localFile), creator,
                new Date(localFile.lastModified()), 0, false, folder.getInfo());
        } else {
            throw new IllegalArgumentException("File not Directory nor File: "
                + localFile);
        }
    }

    /**
     * Returns a FileInfo with changed FolderInfo. No version update etc.
     * whatsoever happens.
     * 
     * @param original
     * @param fi
     * @return the new (or existing) instance.
     */
    @Deprecated
    public static FileInfo changedFolderInfo(FileInfo original, FolderInfo fi) {
        Reject.ifNull(original, "Original FileInfo is null");
        if (original.isTemplate()) {
            // TODO Check if this causes problems with DirectoryInfo
            return FileInfo.getTemplate(fi, original.fileName);
        } else {
            if (original.folderInfo.equals(fi)) {
                return original;
            }
            if (original.isFile()) {
                return new FileInfo(original.fileName, original.size,
                    original.modifiedBy, original.lastModifiedDate,
                    original.version, original.deleted, fi);
            } else if (original.isDiretory()) {
                return new DirectoryInfo(original.fileName, original.size,
                    original.modifiedBy, original.lastModifiedDate,
                    original.version, original.deleted, fi);
            } else {
                throw new IllegalArgumentException(
                    "Illegal original FileInfo: " + original.getClass() + ": "
                        + original.toDetailString());
            }
        }
    }

    public static FileInfo modifiedFile(FileInfo original,
        FolderRepository rep, File localFile, MemberInfo modby)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject
            .ifTrue(original.isTemplate(), "Cannot modify template FileInfo!");
        String fn = buildFileName(original.getFolder(rep), localFile);
        if (original.fileName.equals(fn)) {
            fn = original.fileName;
        }

        if (original.isFile()) {
            return new FileInfo(fn, localFile.length(), modby, new Date(
                localFile.lastModified()), original.version + 1, false,
                original.folderInfo);
        } else if (original.isDiretory()) {
            return new DirectoryInfo(fn, localFile.length(), modby, new Date(
                localFile.lastModified()), original.version + 1, false,
                original.folderInfo);
        } else {
            throw new IllegalArgumentException("Illegal original FileInfo: "
                + original.getClass() + ": " + original.toDetailString());
        }
    }

    public static FileInfo deletedFile(FileInfo original, MemberInfo delby,
        Date delDate)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject
            .ifTrue(original.isTemplate(), "Cannot delete template FileInfo!");
        if (original.isFile()) {
            return new FileInfo(original.fileName, 0L, delby, delDate,
                original.version + 1, true, original.folderInfo);
        } else if (original.isDiretory()) {
            return new DirectoryInfo(original.fileName, 0L, delby, delDate,
                original.version + 1, true, original.folderInfo);
        } else {
            throw new IllegalArgumentException("Illegal original FileInfo: "
                + original.getClass() + ": " + original.toDetailString());
        }
    }

    @Deprecated
    public static FileInfo updatedVersion(FileInfo original, int newVersion) {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject
            .ifTrue(original.isTemplate(), "Cannot update template FileInfo!");
        Reject.ifTrue(original instanceof DirectoryInfo,
            "Possible problem. Unable to perform on dirInfo:"
                + original.toDetailString());
        return new FileInfo(original.fileName, original.size,
            original.modifiedBy, original.lastModifiedDate, newVersion,
            original.deleted, original.folderInfo);
    }

    protected static String buildFileName(Folder folder, File file) {
        String fn = file.getName();
        File parent = file.getParentFile();
        File folderBase = folder.getLocalBase();

        while (!folderBase.equals(parent)) {
            if (parent == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fn = parent.getName() + "/" + fn;
            parent = parent.getParentFile();
        }
        return fn;
    }
}
