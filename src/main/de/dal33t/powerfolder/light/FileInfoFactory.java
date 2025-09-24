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

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            return new DirectoryInfo(folder, name, null, null);
        }
        return new FileInfo(folder, name, null, null);
    }

    public static FileInfo lookupInstance(Folder folder, Path file) {
        String fn = buildFileName(folder.getLocalBase(), file);
        return lookupInstance(folder.getInfo(), fn, Files.isDirectory(file));
    }

    public static FileInfo lookupInstanceForTest(FolderInfo folder, String name, Date modificationDate) {
        return new FileInfo(folder, name, modificationDate, null);
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
                    original.getOID(), original.getSize(),
                    original.getModifiedBy(), original.getModifiedByAccount(),
                    original.getModifiedDate(), original.getVersion(),
                    original.getHashes(), original.isDeleted(),
                    original.getTags(), fi.intern());
            } else if (original.isDiretory()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Corrected DirectoryInfo on "
                        + original.toDetailString());
                }
                return new DirectoryInfo(original.getRelativeName(),
                    original.getOID(),
                    original.getModifiedBy(), original.getModifiedByAccount(),
                    original.getModifiedDate(), original.getVersion(),
                    original.getHashes(), original.isDeleted(),
                    original.getTags(), fi.intern());
            } else {
                throw new IllegalArgumentException(
                    "Illegal original FileInfo: " + original.getClass() + ": "
                        + original.toDetailString());
            }
        }
    }

    /**
     * Returns a FileInfo with changed AccountInfo. No version update etc.
     * whatsoever happens.
     *
     * @param original
     * @param accountInfo
     * @return the new (or existing) instance.
     */
    public static FileInfo changeModifiedAccount(FileInfo original, AccountInfo accountInfo) {
        Reject.ifNull(original, "Original FileInfo is null");
        if (original.getModifiedByAccount() != null && accountInfo != null) {
            if (Util.equals(accountInfo.getUsername(), original.getModifiedByAccount().getUsername())
                    && Util.equals(accountInfo.getDisplayName(), original.getModifiedByAccount().getDisplayName())) {
                // Same display name + username. No update required
                return original;
            }
        }
        if (original.isFile()) {
            if (original.isLookupInstance()) {
                return new FileInfo(original.getFolderInfo(),
                        original.getRelativeName(), original.getModifiedDate(), accountInfo);
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Corrected AccountInfo on "
                        + original.toDetailString());
            }
            return new FileInfo(original.getRelativeName(),
                    original.getOID(), original.getSize(),
                    original.getModifiedBy(), accountInfo,
                    original.getModifiedDate(), original.getVersion(),
                    original.getHashes(), original.isDeleted(),
                    original.getTags(), original.getFolderInfo());
        } else if (original.isDiretory()) {
            if (original.isLookupInstance()) {
                return new DirectoryInfo(original.getFolderInfo(),
                        original.getRelativeName(), original.getModifiedDate(), accountInfo);
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Corrected AccountInfo on "
                        + original.toDetailString());
            }
            return new DirectoryInfo(original.getRelativeName(),
                    original.getOID(),
                    original.getModifiedBy(), accountInfo,
                    original.getModifiedDate(), original.getVersion(),
                    original.getHashes(), original.isDeleted(),
                    original.getTags(), original.getFolderInfo());
        } else {
            throw new IllegalArgumentException(
                    "Illegal original FileInfo: " + original.getClass() + ": "
                            + original.toDetailString());
        }
    }
    
    /**
     * PFC-2352
     * @param fInfo
     * @param oid
     * @return a new instance with the given OID.
     */
    public static FileInfo setOID(FileInfo fInfo, String oid) {
        Reject.ifNull(fInfo, "FileInfo");
        if (StringUtils.isNotBlank(fInfo.getOID())) {
            LOG.warning("Overwriting existing OID: " + fInfo.getOID() + " of "
                + fInfo.toDetailString());
        }
        if (fInfo instanceof DirectoryInfo) {
            return new DirectoryInfo(fInfo.getRelativeName(), oid, fInfo.getModifiedBy(),
                fInfo.getModifiedByAccount(), fInfo.getModifiedDate(),
                fInfo.getVersion(), fInfo.getHashes(), fInfo.isDeleted(),
                fInfo.getTags(), fInfo.getFolderInfo());
        }
        return new FileInfo(fInfo.getRelativeName(), oid, fInfo.getSize(),
            fInfo.getModifiedBy(), fInfo.getModifiedByAccount(),
            fInfo.getModifiedDate(), fInfo.getVersion(), fInfo.getHashes(),
            fInfo.isDeleted(), fInfo.getTags(), fInfo.getFolderInfo());
    }

    public static FileInfo unmarshallExistingFile(FolderInfo fi,
        String fileName, String oid, long size, MemberInfo modByDevice,
        AccountInfo modByAccount, Date modDate, int version, String hashes,
        boolean dir, String tags)
    {
        if (dir) {
            return new DirectoryInfo(fileName, oid, modByDevice,
                modByAccount, modDate, version, hashes, false, tags, fi);
        }
        return new FileInfo(fileName, oid, size, modByDevice, modByAccount,
            modDate, version, hashes, false, tags, fi);
    }

    public static FileInfo unmarshallDeletedFile(FolderInfo fi,
        String fileName, String oid, MemberInfo modByDevice,
        AccountInfo modByAccount, Date modDate, int version, String hashes,
        boolean dir, String tags)
    {
        if (dir) {
            return new DirectoryInfo(fileName, oid, modByDevice,
                modByAccount, modDate, version, hashes, true, tags, fi);
        }
        return new FileInfo(fileName, oid, 0L, modByDevice, modByAccount,
            modDate, version, hashes, true, tags, fi);
    }

    /**
     * Initialize within a folder
     * 
     * @param folder
     * @param localFile
     * @param creatorDevice
     * @param directory
     *            if the given file is a directory.
     * @return the new file
     */
    public static FileInfo newFile(Folder folder, Path localFile, String oid,
        MemberInfo creatorDevice, AccountInfo creatorAccount, String hashes,
        boolean directory, String tags)
    {
        long date = new Date().getTime();
        long size = 0;

        try {
            date = Files.getLastModifiedTime(localFile).toMillis();
        } catch (IOException ioe) {
            LOG.fine(ioe.toString());
        }

        if (directory) {
            return new DirectoryInfo(buildFileName(folder.getLocalBase(),
                localFile), oid, creatorDevice, creatorAccount, new Date(date),
                0, hashes, false, tags, folder.getInfo());
        } else {
            try {
                size = Files.size(localFile);
            } catch (IOException ioe) {
                LOG.fine(ioe.toString());
            }
            return new FileInfo(
                buildFileName(folder.getLocalBase(), localFile), oid, size,
                creatorDevice, creatorAccount, new Date(date), 0, hashes,
                false, tags, folder.getInfo());
        }
    }

    public static FileInfo modifiedFile(FileInfo original, Folder folder,
        Path localFile, MemberInfo modByDevice, AccountInfo modByAccount,
        String newHashes)
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
                    return new DirectoryInfo(fn, original.getOID(), modByDevice, modByAccount,
                        new Date(Files.getLastModifiedTime(localFile)
                            .toMillis()), original.getVersion() + 1, newHashes,
                        false, original.getTags(), original.getFolderInfo());
                }
                return new FileInfo(fn, original.getOID(),
                    Files.size(localFile), modByDevice, modByAccount, new Date(
                        Files.getLastModifiedTime(localFile).toMillis()),
                    original.getVersion() + 1, newHashes, false,
                    original.getTags(), original.getFolderInfo());
            } else if (original.isDiretory()) {
                if (!isDir) {
                    return new FileInfo(fn, original.getOID(),
                        Files.size(localFile), modByDevice, modByAccount,
                        new Date(Files.getLastModifiedTime(localFile)
                            .toMillis()), original.getVersion() + 1, newHashes,
                        false, original.getTags(), original.getFolderInfo());
                }
                return new DirectoryInfo(fn, original.getOID(), modByDevice, modByAccount,
                        new Date(Files.getLastModifiedTime(localFile).toMillis()),
                    original.getVersion() + 1, newHashes, false,
                    original.getTags(), original.getFolderInfo());
            } else {
                throw new IllegalArgumentException(
                    "Illegal original FileInfo: " + original.getClass() + ": "
                        + original.toDetailString());
            }
        } catch (IOException ioe) {
            LOG.warning(ioe.toString());
            return null;
        }
    }

    public static FileInfo deletedFile(FileInfo original,
        MemberInfo delbyDevice, AccountInfo delByAccount, Date delDate)
    {
        Reject.ifNull(original, "Original FileInfo is null");
        Reject.ifTrue(original.isLookupInstance(),
            "Cannot delete lookup FileInfo!");
        // PFC-2352: TODO Think about hashes!
        if (original.isFile()) {
            return new FileInfo(original.getRelativeName(), original.getOID(),
                original.getSize(), delbyDevice, delByAccount, delDate,
                original.getVersion() + 1, original.getHashes(), true,
                original.getTags(), original.getFolderInfo());
        } else if (original.isDiretory()) {
            return new DirectoryInfo(original.getRelativeName(),
                original.getOID(), delbyDevice, delByAccount, delDate,
                original.getVersion() + 1, original.getHashes(), true,
                original.getTags(), original.getFolderInfo());
        } else {
            throw new IllegalArgumentException("Illegal original FileInfo: "
                + original.getClass() + ": " + original.toDetailString());
        }
    }

    public static FileInfo archivedFile(FolderInfo foInfo, String name,
        String oid, long size, MemberInfo modby, AccountInfo modByAccount,
        Date modDate, int version, String hashes, String tags)
    {
        return new FileInfo(name, oid, size, modby, modByAccount, modDate,
            version, hashes, false, tags, foInfo);
    }

    public static DirectoryInfo createBaseDirectoryInfo(FolderInfo foInfo) {
        return new DirectoryInfo(foInfo, "", null, null);
    }

    // PF-1790: Subfolder sharing

    public static FileInfo mapToSubFolder(FileInfo topFileInfo, FolderInfo subFolderInfo) {
        Reject.ifNull(topFileInfo, "FileInfo");
        Reject.ifNull(subFolderInfo, "SubFolderInfo");
        Reject.ifFalse(subFolderInfo.isSubFolder(), "Not a subfolder");

        String topFileInfoRelativeName = topFileInfo.getRelativeName();
        String locationName = subFolderInfo.getLocation().getRelativeName();

        Reject.ifFalse(topFileInfoRelativeName.startsWith(locationName), "FileInfo not in subfolder");

        int stripFrom = locationName.length();
        if (locationName.length() < topFileInfoRelativeName.length()) {
            stripFrom++;
        }
        String strippedRelative = topFileInfoRelativeName.substring(stripFrom);

        if (topFileInfo.isLookupInstance()) {
            if (topFileInfo instanceof DirectoryInfo) {
                return new DirectoryInfo(
                        subFolderInfo,
                        strippedRelative,
                        topFileInfo.getModifiedDate(),
                        topFileInfo.getModifiedByAccount());
            } else {
                return new FileInfo(
                        subFolderInfo,
                        strippedRelative,
                        topFileInfo.getModifiedDate(),
                        topFileInfo.getModifiedByAccount());
            }
        }

        if (topFileInfo instanceof DirectoryInfo) {
            if (strippedRelative.isEmpty()) {
                return new DirectoryInfo(
                        subFolderInfo,
                        "",
                        topFileInfo.getModifiedDate(),
                        topFileInfo.getModifiedByAccount());
            }
            return new DirectoryInfo(
                    strippedRelative,
                    topFileInfo.getOID(),
                    topFileInfo.getModifiedBy(),
                    topFileInfo.getModifiedByAccount(),
                    topFileInfo.getModifiedDate(),
                    topFileInfo.getVersion(),
                    topFileInfo.getHashes(),
                    topFileInfo.isDeleted(),
                    topFileInfo.getTags(),
                    subFolderInfo);
        } else {
            return new FileInfo(
                    strippedRelative,
                    topFileInfo.getOID(),
                    topFileInfo.getSize(),
                    topFileInfo.getModifiedBy(),
                    topFileInfo.getModifiedByAccount(),
                    topFileInfo.getModifiedDate(),
                    topFileInfo.getVersion(),
                    topFileInfo.getHashes(),
                    topFileInfo.isDeleted(),
                    topFileInfo.getTags(),
                    subFolderInfo);
        }
    }

    public static FileInfo mapToTopFolder(FileInfo subFileInfo) {
        Reject.ifNull(subFileInfo, "FileInfo");
        Reject.ifNull(subFileInfo.getFolderInfo().getParent(), "FileInfo not from subfolder: " + subFileInfo.toDetailString());

        FolderInfo parentFolderInfo = subFileInfo.getFolderInfo().getParent().getFolderInfo();

        String subPath = subFileInfo.getFolderInfo().getParent().getRelativeName();
        String relativeName = "";
        if (!subPath.isEmpty()) {
            relativeName += subPath + '/';
        }
        relativeName += subFileInfo.getFolderInfo().getName();
        if (!subFileInfo.getRelativeName().isEmpty()) {
            relativeName += '/' + subFileInfo.getRelativeName();
        }

        if (subFileInfo.isLookupInstance()) {
            if (subFileInfo instanceof DirectoryInfo) {
                return new DirectoryInfo(
                        parentFolderInfo,
                        relativeName,
                        subFileInfo.getModifiedDate(),
                        subFileInfo.getModifiedByAccount());
            } else {
                return new FileInfo(
                        parentFolderInfo,
                        relativeName,
                        subFileInfo.getModifiedDate(),
                        subFileInfo.getModifiedByAccount());
            }
        }

        if (subFileInfo instanceof DirectoryInfo) {
            return new DirectoryInfo(
                    relativeName,
                    subFileInfo.getOID(),
                    subFileInfo.getModifiedBy(),
                    subFileInfo.getModifiedByAccount(),
                    subFileInfo.getModifiedDate(),
                    subFileInfo.getVersion(),
                    subFileInfo.getHashes(),
                    subFileInfo.isDeleted(),
                    subFileInfo.getTags(),
                    parentFolderInfo);
        } else {
            return new FileInfo(
                    relativeName,
                    subFileInfo.getOID(),
                    subFileInfo.getSize(),
                    subFileInfo.getModifiedBy(),
                    subFileInfo.getModifiedByAccount(),
                    subFileInfo.getModifiedDate(),
                    subFileInfo.getVersion(),
                    subFileInfo.getHashes(),
                    subFileInfo.isDeleted(),
                    subFileInfo.getTags(),
                    parentFolderInfo);
        }
    }

    private static final String[] ILLEGAL_WINDOWS_CHARS = {"|", "?", "\"", "*",
        "<", ":", ">", "\r"};

    /**
     * #2480: Encodes illegal characters in filenames, e.g. for windows such as: |, :,
     * <, >,
     *
     * @param relativeFilename
     *            containing the illegal chars, e.g. "My|File.txt"
     * @return
     */
    public static String encodeIllegalChars(String relativeFilename) {
        String output = relativeFilename;

        if (OSUtil.isWindowsSystem()) {
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
        }

        return output;
    }

    /**
     * #2480: Decodes illegal characters in filenames, e.g. for windows such as: |, :,
     * <, >,
     *
     * @param relativeFilename
     *            containing the legal chars, e.g. "My$%fA%$File.txt"
     * @return
     */
    public static String decodeIllegalChars(String relativeFilename) {
        String output = relativeFilename;

        if (OSUtil.isWindowsSystem()) {
            int start = 0;
            while ((start = output.indexOf("$%", start)) >= 0) {
                int end = output.indexOf("%$", start);
                if (end < 0 || end < start + 2) {
                    break;
                }
                String encoded = output.substring(start + 2, end);
                try {
                    String decoded = Base64.decodeString(encoded + "==");
                    output = output.substring(0, start) + decoded + output.substring(end + 2);
                } catch (IllegalArgumentException e) {
                    break;
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "Exception while decoding filename: " + relativeFilename + ". " + e, e);
                    break;
                }
            }
        }
        return output;
    }

    public static String buildFileName(Path baseDirectory, Path file) {
        Reject.ifNull(baseDirectory, "Base directory is null");
        Reject.ifNull(file, "File is null");
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
            if (parent.getFileName() == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy. Cannot access parent. Basedir: "
                    +baseDirectory + ", file: " + file + ", parent: " + parent);
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
