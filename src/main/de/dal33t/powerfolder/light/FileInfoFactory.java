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

import java.io.IOException;
import java.io.ObjectInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Factory to create {@link FileInfo} and {@link DirectoryInfo} objects.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 */
public final class FileInfoFactory {
    private static final Logger LOG = Logger.getLogger(FileInfoFactory.class
        .getName());

    private FileInfoFactory() {
        // No instance allowed
    }

    public static FileInfo readExt(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        int type = in.readInt();
        FileInfo fileInfo = type == 0 ? new FileInfo() : new DirectoryInfo();
        fileInfo.readExternal(in);
        return fileInfo;
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
     * @return a DirectoryInfo object use to lookup.
     */
    public static DirectoryInfo lookupDirectory(FolderInfo folder, String name)
    {
        return (DirectoryInfo) lookupInstance(folder, name, true);
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

    public static FileInfo lookupInstance(Folder folder, Path file) {
        String fn = buildFileName(folder.getLocalBase(), file);
        return lookupInstance(folder.getInfo(), fn, Files.isDirectory(file));
    }

    /**
     * Returns a FileInfo with changed FolderInfo. No version update etc.
     * whatsoever happens.
     * 
     * @param original
     * @param fi
     * @return the new (or existing) instance.
     */
    public static FileInfo changedFolderInfo(FileInfo original, FolderInfo fi) {
        Reject.ifNull(original, "Original FileInfo is null");
        if (original.isLookupInstance()) {
            // TODO Check if this causes problems with DirectoryInfo
            return lookupInstance(fi, original.getRelativeName());
        } else {
            if (original.getFolderInfo().equals(fi)) {
                return original;
            }
            if (original.isFile()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Corrected FolderInfo on "
                        + original.toDetailString());
                }
                return new FileInfo(original.getRelativeName(),
                    original.getSize(), original.getModifiedBy(),
                    original.getModifiedDate(), original.getVersion(),
                    original.isDeleted(), fi.intern());
            } else if (original.isDiretory()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Corrected DirectoryInfo on "
                        + original.toDetailString());
                }
                return new DirectoryInfo(original.getRelativeName(),
                    original.getSize(), original.getModifiedBy(),
                    original.getModifiedDate(), original.getVersion(),
                    original.isDeleted(), fi.intern());
            } else {
                throw new IllegalArgumentException(
                    "Illegal original FileInfo: " + original.getClass() + ": "
                        + original.toDetailString());
            }
        }
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
     * @param directory
     *            if the given file is a directory.
     * @return the new file
     */
    public static FileInfo newFile(Folder folder, Path localFile,
        MemberInfo creator, boolean directory)
    {
        long date = new Date().getTime();
        long size = 0;

        try {
            date = Files.getLastModifiedTime(localFile).toMillis();
        } catch (IOException ioe) {
            LOG.fine(ioe.getMessage());
        }

        if (directory) {
            return new DirectoryInfo(buildFileName(folder.getLocalBase(),
                localFile), creator, new Date(date), 0, false, folder.getInfo());
        } else {
            try {
                size = Files.size(localFile);
            } catch (IOException ioe) {
                LOG.fine(ioe.getMessage());
            }

            return new FileInfo(
                buildFileName(folder.getLocalBase(), localFile),
                size, creator, new Date(date), 0, false,
                folder.getInfo());
        }
    }

    public static FileInfo modifiedFile(FileInfo original, Folder folder,
        Path localFile, MemberInfo modby)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject.ifTrue(original.isLookupInstance(),
            "Cannot modify template FileInfo!");
        Reject.ifNull(folder, "Folder is null");
        String fn = buildFileName(folder.getLocalBase(), localFile);
        if (original.getRelativeName().equals(fn)) {
            fn = original.getRelativeName();
        }

        boolean isDir = Files.isDirectory(localFile);
        try {
            if (original.isFile()) {
                if (isDir) {
                    return new DirectoryInfo(fn, Files.size(localFile), modby,
                        new Date(Files.getLastModifiedTime(localFile).toMillis()),
                        original.getVersion() + 1, false, original.getFolderInfo());
                }
                return new FileInfo(fn, Files.size(localFile), modby, new Date(
                    Files.getLastModifiedTime(localFile).toMillis()),
                    original.getVersion() + 1, false, original.getFolderInfo());
            } else if (original.isDiretory()) {
                if (!isDir) {
                    return new FileInfo(fn, Files.size(localFile), modby, new Date(
                        Files.getLastModifiedTime(localFile).toMillis()),
                        original.getVersion() + 1, false, original.getFolderInfo());
                }
                return new DirectoryInfo(fn, Files.size(localFile), modby,
                    new Date(Files.getLastModifiedTime(localFile).toMillis()),
                    original.getVersion() + 1, false, original.getFolderInfo());
            } else {
                throw new IllegalArgumentException("Illegal original FileInfo: "
                    + original.getClass() + ": " + original.toDetailString());
            }
        } catch (IOException ioe) {
            LOG.warning(ioe.getMessage());
            return null;
        }
    }

    public static FileInfo deletedFile(FileInfo original, MemberInfo delby,
        Date delDate)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject.ifTrue(original.isLookupInstance(),
            "Cannot delete template FileInfo!");
        if (original.isFile()) {
            return new FileInfo(original.getRelativeName(), original.getSize(),
                delby, delDate, original.getVersion() + 1, true,
                original.getFolderInfo());
        } else if (original.isDiretory()) {
            return new DirectoryInfo(original.getRelativeName(), 0L, delby,
                delDate, original.getVersion() + 1, true,
                original.getFolderInfo());
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

    public static DirectoryInfo createBaseDirectoryInfo(FolderInfo foInfo) {
        return new DirectoryInfo(foInfo, "");
    }

    private static final String[] ILLEGAL_WINDOWS_CHARS = {"|", "?", "\"", "*",
        "<", ":", ">", "\r"};

    /**
     * #2480: Encodes illegal characters in filenames for windows such as: |, :,
     * <, >,
     * 
     * @param relativeFilename
     *            containing the illegal chars, e.g. "My|File.txt"
     * @return
     */
    public static String encodeIllegalChars(String relativeFilename) {
        if (!OSUtil.isWindowsSystem()) {
            return relativeFilename;
        }
        String output = relativeFilename;
        for (String illChar : ILLEGAL_WINDOWS_CHARS) {
            if (output.contains(illChar)) {
                String replacement = Base64.encodeString(illChar);
                replacement = replacement.replace("=", "");
                replacement = "$%" + replacement + "%$";
                output = output.replace(illChar, replacement);
            }
        }
        if (output.length() > 1) {
            char lastChar = output.charAt(output.length() - 1);
            if (lastChar == ' ' || lastChar == '.') {
                String replacement = Base64.encodeString(String.valueOf(output
                    .charAt(output.length() - 1)));
                replacement = replacement.replace("=", "");
                replacement = "$%" + replacement + "%$";
                output = output.substring(0, output.length() - 1);
                output += replacement;
            }
        }
        if (output.contains(" /")) {
            String replacement = Base64.encodeString(" ");
            replacement = replacement.replace("=", "");
            replacement = "$%" + replacement + "%$";
            output = output.replace(" /", replacement + "/");
        }
        if (output.contains("./")) {
            String replacement = Base64.encodeString(".");
            replacement = replacement.replace("=", "");
            replacement = "$%" + replacement + "%$";
            output = output.replace("./", replacement + "/");
        }
        // Spaces at start and end
        return output;
    }

    /**
     * #2480: Decodes illegal characters in filenames for windows such as: |, :,
     * <, >,
     * 
     * @param relativeFilename
     *            containing the legal chars, e.g. "My$%fA%$File.txt"
     * @return
     */
    public static String decodeIllegalChars(String relativeFilename) {
        if (!OSUtil.isWindowsSystem()) {
            return relativeFilename;
        }
        String output = relativeFilename;
        int start = 0;
        while ((start = output.indexOf("$%", start)) >= 0) {
            int end = output.indexOf("%$", start);
            if (end < 0) {
                break;
            }
            String encoded = output.substring(start + 2, end);
            String decoded = Base64.decodeString(encoded + "==");
            output = output.substring(0, start) + decoded
                + output.substring(end + 2);
        }
        return output;
    }

    protected static String buildFileName(Path baseDirectory, Path file) {
        if (file.equals(baseDirectory)) {
            return "";
        }
        String fn = decodeIllegalChars(file.getFileName().toString());
        if (fn.endsWith("/")) {
            fn = fn.substring(0, fn.length() - 1);
        }
        Path parent = file.getParent();

        while (!baseDirectory.equals(parent)) {
            if (parent == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy. Basedir: "
                        + baseDirectory + ", file: " + file);
            }
            fn = decodeIllegalChars(parent.getFileName().toString()) + '/' + fn;
            parent = parent.getParent();
        }
        if (fn.endsWith("/")) {
            // Crop off last /
            fn = fn.substring(0, fn.length() - 1);
        }
        while (fn.startsWith("/")) {
            // Crop off first /s
            fn = fn.substring(1);
        }
        return fn;
    }
}
