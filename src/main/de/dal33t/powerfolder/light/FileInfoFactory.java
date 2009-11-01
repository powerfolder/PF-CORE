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

    /**
     * @param folder
     * @param name
     * @return a ACTUAL FileInfo object use to lookup other FileInfo instances.
     */
    public static FileInfo lookupInstance(FolderInfo folder, String name) {
        return lookupInstance(folder, name, false);
    }

    /**
     * @param folder
     * @param name
     * @param dir
     * @return a FileInfo or DirectoryInfo object use to lookup other File or
     *         DirectoryInfo instances.
     */
    public static FileInfo lookupInstance(FolderInfo folder, String name,
        boolean dir)
    {
        if (dir) {
            return new DirectoryInfo(folder, name);
        }
        return new FileInfo(folder, name);
    }

    public static FileInfo lookupInstance(Folder folder, File file) {
        String fn = buildFileName(folder.getLocalBase(), file);
        return lookupInstance(folder.getInfo(), fn, file.isDirectory());
    }

    public static FileInfo unmarshallExistingFile(FolderInfo fi,
        String fileName, long size, MemberInfo modby, Date modDate,
        int version, boolean dir)
    {
        if (dir) {
            return new DirectoryInfo(fileName, size, modby, modDate, version,
                false, fi);
        }
        return new FileInfo(fileName, size, modby, modDate, version, false, fi);
    }

    public static FileInfo unmarshallDeletedFile(FolderInfo fi,
        String fileName, MemberInfo modby, Date modDate, int version,
        boolean dir)
    {
        if (dir) {
            return new DirectoryInfo(fileName, 0, modby, modDate, version,
                true, fi);
        }
        return new FileInfo(fileName, 0, modby, modDate, version, true, fi);
    }

    /**
     * Initalize within a folder
     * 
     * @param folder
     * @param localFile
     * @param creator
     * @param directory if the given file is a directory.
     * @return the new file
     */
    public static FileInfo newFile(Folder folder, File localFile,
        MemberInfo creator, boolean directory)
    {
        if (directory) {
            return new DirectoryInfo(buildFileName(folder.getLocalBase(),
                localFile), creator, new Date(localFile.lastModified()), 0,
                false, folder.getInfo());
        } else {
            return new FileInfo(
                buildFileName(folder.getLocalBase(), localFile), localFile
                    .length(), creator, new Date(localFile.lastModified()), 0,
                false, folder.getInfo());
        }
    }

    public static FileInfo modifiedFile(FileInfo original,
        FolderRepository rep, File localFile, MemberInfo modby)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject
            .ifTrue(original.isTemplate(), "Cannot modify template FileInfo!");
        String fn = buildFileName(original.getFolder(rep).getLocalBase(),
            localFile);
        if (original.getRelativeName().equals(fn)) {
            fn = original.getRelativeName();
        }

        if (original.isFile()) {
            return new FileInfo(fn, localFile.length(), modby, new Date(
                localFile.lastModified()), original.getVersion() + 1, false,
                original.getFolderInfo());
        } else if (original.isDiretory()) {
            return new DirectoryInfo(fn, localFile.length(), modby, new Date(
                localFile.lastModified()), original.getVersion() + 1, false,
                original.getFolderInfo());
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
            return new FileInfo(original.getRelativeName(), 0L, delby, delDate,
                original.getVersion() + 1, true, original.getFolderInfo());
        } else if (original.isDiretory()) {
            return new DirectoryInfo(original.getRelativeName(), 0L, delby,
                delDate, original.getVersion() + 1, true, original
                    .getFolderInfo());
        } else {
            throw new IllegalArgumentException("Illegal original FileInfo: "
                + original.getClass() + ": " + original.toDetailString());
        }
    }

    public static FileInfo archivedFile(FolderInfo foInfo, String name,
        long size, MemberInfo modby, Date modDate, int version)
    {
        return new FileInfo(name, size, modby, modDate, version, false, foInfo);
    }

    @Deprecated
    public static FileInfo updatedVersion(FileInfo original, int newVersion) {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject
            .ifTrue(original.isTemplate(), "Cannot update template FileInfo!");
        Reject.ifTrue(original instanceof DirectoryInfo,
            "Possible problem. Unable to perform on dirInfo:"
                + original.toDetailString());
        return new FileInfo(original.getRelativeName(), original.getSize(),
            original.getModifiedBy(), original.getModifiedDate(), newVersion,
            original.isDeleted(), original.getFolderInfo());
    }

    protected static String buildFileName(File baseDirectory, File file) {
        String fn = file.getName();
        File parent = file.getParentFile();

        while (!baseDirectory.equals(parent)) {
            if (parent == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fn = parent.getName() + '/' + fn;
            parent = parent.getParentFile();
        }
        return fn;
    }
}
