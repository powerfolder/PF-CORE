/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.disk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * A file archiver that tries to move a file to an archive first, and falls back
 * to copying otherwise, or if forced to. <i>Note:</i> No support for removal of
 * old files (yet) - special care of directories might be required Archives are
 * stored in an archives directory, with suffix '_K_nnn', where 'nnn' is the
 * version number. So 'data/info.txt' archive version 6 would be
 * 'archive/data/info.txt_K_6'.
 *
 * @author dante
 */
public class FileArchiver {

    private static final Logger log = Logger.getLogger(FileArchiver.class
        .getName());
    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();
    private static final Pattern BASE_NAME_PATTERN = Pattern
        .compile("(.*)_K_\\d+(.*)");
    private static final String SIZE_INFO_FILE = "Size";

    private final Path archiveDirectory;
    private volatile int versionsPerFile;
    private MemberInfo mySelf;

    /**
     * Cached size of this file archive.
     */
    private Long size;

    /**
     * Constructs a new FileArchiver which stores backups in the given
     * directory.
     *
     * @param archiveDirectory
     * @param mySelf
     *            myself
     */
    public FileArchiver(Path archiveDirectory, MemberInfo mySelf) {
        Reject.notNull(archiveDirectory, "archiveDirectory");
        Reject.ifNull(mySelf, "Myself");
        this.archiveDirectory = archiveDirectory;
        // Default: Store unlimited # of files
        versionsPerFile = -1;
        this.mySelf = mySelf;
        this.size = loadSize();
    }

    private Long loadSize() {
        Path sizeFile = archiveDirectory.resolve(SIZE_INFO_FILE);
        if (Files.notExists(sizeFile)) {
            return null;
        }
        try (InputStream fin = Files.newInputStream(sizeFile)) {
            byte[] buf = StreamUtils.readIntoByteArray(fin);
            return Long.valueOf(new String(buf));
        } catch (Exception e) {
            log.fine("Unable to read size of archive to " + sizeFile + ". " + e);
            return null;
        }
    }

    /**
     * @see FileArchiver#archive(FileInfo, Path, boolean)
     */
    public void archive(FileInfo fileInfo, Path source, boolean forceKeepSource)
        throws IOException
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(source, "source");

        if (versionsPerFile == 0) {
            // Optimization for zero-archive
            if (!forceKeepSource) {
                if (!Files.deleteIfExists(source)) {
                    log.warning("Unable to remove old file " + source);
                }
                return;
            }
        }

        Path target = getArchiveTarget(fileInfo);

        if (Files.exists(target)) {
            log.warning("File " + fileInfo.toDetailString()
                + " seems to be archived already, doing nothing.");
            // Maybe throw Exception instead?
            return;
        }

        long oldSize = getSize();

        try {
            Files.createDirectories(target.getParent());
        } catch (FileAlreadyExistsException faee) {
            // Ignore.
        } catch (IOException ioe) {

        }

        if (Files.exists(target.getParent())) {
            // Reset cache
            // size = null;
            boolean tryCopy = forceKeepSource;
            if (!tryCopy) {
                try {
                    Files.move(source, target);
                    if (size != null && Files.exists(target)) {
                        size += Files.size(target);
                    }
                } catch (IOException ioe) {
                    log.warning("Failed to rename " + source
                        + ", falling back to copying: " + ioe);
                    tryCopy = true;
                }
            }
            if (tryCopy) {
                long lastModified = Files.getLastModifiedTime(source)
                    .toMillis();
                PathUtils.copyFile(source, target);
                // Preserve last modification date.
                Files.setLastModifiedTime(target,
                    FileTime.fromMillis(lastModified));
                if (size != null && Files.exists(target)) {
                    size += Files.size(target);
                }
            }

            if (log.isLoggable(Level.FINE)) {
                log.fine("Archived " + fileInfo.toDetailString() + " from "
                    + source + " to " + target);
            }

            // Success, now check if we have to remove a file
            List<Path> list = getArchivedFiles(target.getParent(),
                fileInfo.getFilenameOnly());
            checkArchivedFile(list);

            if (oldSize != size) {
                saveSize();
            }
        } else {
            throw new IOException("Failed to create directory: "
                + target.getParent());
        }
    }

    public final Path getArchiveDir() {
        return archiveDirectory;
    }

    private void checkArchivedFile(Collection<Path> versions)
        throws IOException
    {
        assert versions != null;
        if (versionsPerFile < 0) {
            // Unlimited. Don't check
            return;
        }

        if (versions.size() <= versionsPerFile) {
            return;
        }

        Path[] versionArray = versions.toArray(new Path[versions.size()]);
        Arrays.sort(versionArray, VERSION_COMPARATOR);
        int toDelete = versionArray.length - versionsPerFile;
        long oldSize = size;
        for (Path f : versionArray) {
            if (toDelete <= 0) {
                break;
            }
            toDelete--;

            long len = Files.size(f);
            try {
                Files.delete(f);
                if (size != null) {
                    size -= len;
                }
                if (log.isLoggable(Level.FINE)) {
                    log.fine("checkArchivedFile: Deleted archived file " + f);
                }
            } catch (IOException ioe) {
                throw new IOException("Could not delete old version: " + f);
            }
        }
        if (oldSize != size) {
            saveSize();
        }
    }

    /**
     * Tries to ensure that only the allowed amount of versions per file is in
     * the archive.
     *
     * @return true the maintenance worked successfully for all files, false if
     *         it failed for at least one file
     */
    public synchronized boolean maintain() {
        if (Files.notExists(archiveDirectory)) {
            return true;
        }
        boolean check = checkRecursive(archiveDirectory, new HashSet<Path>());
        size = null;
        return check;
    }

    private boolean checkRecursive(Path dir, Set<Path> checked) {
        assert dir != null && Files.isDirectory(dir);
        assert checked != null;

        if (dir == null || Files.notExists(dir) || !Files.isDirectory(dir)) {
            // Is empty or not existent.
            return true;
        }

        boolean allSuccessful = true;

        Set<Path> flist = new HashSet<Path>();
        try (DirectoryStream<Path> file = Files.newDirectoryStream(dir)) {
            for (Path p : file) {
                flist.add(p);
            }
        } catch (IOException ioe) {
            log.warning(ioe.getMessage());
            return false;
        }

        Map<String, Collection<Path>> fileMap = new HashMap<String, Collection<Path>>();
        for (Path f : flist) {
            if (f.getFileName().toString().equals(SIZE_INFO_FILE)) {
                continue;
            }
            if (Files.isDirectory(f)) {
                boolean thisSuccessfuly = checkRecursive(f, checked);
                if (thisSuccessfuly) {
                    try {
                        Files.delete(f);
                    } catch (IOException ioe) {
                        log.warning(ioe.getMessage());
                    }
                }
                allSuccessful &= thisSuccessfuly;
            } else {
                String baseName = getBaseName(f);
                Path vf = dir.resolve(baseName);
                if (!checked.contains(vf)) {
                    checked.add(vf);
                }
                Collection<Path> files = fileMap.get(baseName);
                if (files == null) {
                    files = new LinkedList<Path>();
                    fileMap.put(baseName, files);
                }
                files.add(f);
            }
        }
        for (Collection<Path> files : fileMap.values()) {
            try {
                checkArchivedFile(files);
            } catch (IOException e) {
                allSuccessful = false;
                log.log(Level.WARNING, "Failed to check " + files, e);
            }
        }
        return allSuccessful;
    }

    private static String getBaseName(Path file) {
        Matcher m = BASE_NAME_PATTERN.matcher(file.getFileName().toString());
        if (m.matches()) {
            if (m.groupCount() == 1) {
                // Ends with _K_n, so return the first group.
                return m.group(1);
            }
            if (m.groupCount() == 2) {
                // Contained _K_n, so return the first group + second group.
                return m.group(1) + m.group(2);
            }
        }
        throw new IllegalArgumentException("File not in archive: " + file);
    }

    private Path getArchiveTarget(FileInfo fileInfo) {
        String relativeName = fileInfo.getRelativeName();

        // Split something like 'file.txt' into 'file' and '.txt', so we can
        // insert the '_K_nnn' stuff.
        String[] parts = new String[2];
        if (relativeName.contains(".")) {
            int pos = relativeName.lastIndexOf(".");
            parts[0] = relativeName.substring(0, pos);
            parts[1] = relativeName.substring(pos); // Includes the '.';
        } else {
            parts[0] = relativeName;
            parts[1] = "";
        }
        return archiveDirectory.resolve(FileInfoFactory
            .encodeIllegalChars(parts[0])
            + "_K_"
            + fileInfo.getVersion()
            + FileInfoFactory.encodeIllegalChars(parts[1]));
    }

    /**
     * Convert a file name and version into archive file name, something like
     * /bob/file.txt_K_4 . This is the old way of doing it, kept for
     * compatibility.
     *
     * @param fileInfo
     * @return
     */
    private Path getOldArchiveTarget(FileInfo fileInfo) {
        return archiveDirectory.resolve(FileInfoFactory
            .encodeIllegalChars(fileInfo.getRelativeName()) + "_K_"
                + fileInfo.getVersion());
    }

    private String getFileInfoName(Path fileInArchive) {
        return buildFileName(archiveDirectory, fileInArchive);
    }

    private static String buildFileName(Path baseDirectory, Path file) {
        String fn = FileInfoFactory.decodeIllegalChars(file.getFileName()
            .toString());
        int i = fn.lastIndexOf("_K_");
        int ext = fn.lastIndexOf(".");
        if (i >= 0 && ext >= 0) {
            fn = fn.substring(0, i) + fn.substring(ext);
        } else if (i >= 0 && ext < 0) {
            fn = fn.substring(0, i);
        }
        Path parent = file.getParent();

        while (!baseDirectory.equals(parent)) {
            if (parent == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fn = FileInfoFactory.decodeIllegalChars(parent.getFileName()
                .toString()) + '/' + fn;
            parent = parent.getParent();
        }
        return fn;
    }

    /**
     * Parse the file name for the last "_K_" and extract the following version
     * number. Like 'file_K_45.txt' returns 45.
     *
     * @param file
     *            file to parse name.
     * @return the version.
     */
    private static int getVersionNumber(Path file) {
        String fileName = file.getFileName().toString();
        String lastPart = fileName.substring(fileName.lastIndexOf("_K_") + 3);
        if (lastPart.contains(".")) {
            // Strip the extension.
            lastPart = lastPart.substring(0, lastPart.lastIndexOf("."));
        }
        return Integer.parseInt(lastPart);
    }

    private static List<Path> getArchivedFiles(Path directory,
        final String baseName)
    {
        List<Path> ret = new ArrayList<Path>();

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (belongsTo(file.getFileName().toString(), baseName)) {
                    ret.add(file);
                }
            }
        } catch (IOException ioe) {
            log.warning(ioe.getMessage());
        }

        return ret;
    }

    private static boolean belongsTo(String name, String baseName) {
        Matcher m = BASE_NAME_PATTERN.matcher(name);
        if (m.matches()) {
            return Util.equalsRelativeName(m.group(1) + m.group(2), baseName);
        }
        return false;
    }

    public boolean hasArchivedFileInfo(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        // Find archive subdirectory.
        Path subdirectory = archiveDirectory.resolve(
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName()))
            .getParent();
        if (Files.notExists(subdirectory)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files
            .newDirectoryStream(subdirectory)) {
            String fn = FileInfoFactory.encodeIllegalChars(fileInfo
                .getFilenameOnly());

            // get rid of the extension, if present
            int ind = fn.lastIndexOf('.');
            if (ind > -1) {
                fn = fn.substring(0, ind);
            }

            for (Path file : files) {
                if (file.getFileName().toString().startsWith(fn)) {
                    return true;
                }
            }
        } catch (IOException ioe) {
            log.warning(ioe.getMessage());
        }

        return false;
    }

    public List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        // Find archive subdirectory.
        Path subdirectory = PathUtils.buildFileFromRelativeName(
            archiveDirectory,
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName()))
            .getParent();
        if (Files.notExists(subdirectory)) {
            return Collections.emptyList();
        }

        Path target = getArchiveTarget(fileInfo);
        List<Path> archivedFiles = getArchivedFiles(target.getParent(),
            FileInfoFactory.encodeIllegalChars(fileInfo.getFilenameOnly()));
        if (archivedFiles == null || archivedFiles.size() == 0) {
            return Collections.emptyList();
        }
        List<FileInfo> list = new ArrayList<FileInfo>();
        FolderInfo foInfo = fileInfo.getFolderInfo();
        for (Path file : archivedFiles) {
            try {
                int version = getVersionNumber(file);
                Date modDate = new Date(Files.getLastModifiedTime(file)
                    .toMillis());
                String name = getFileInfoName(file);
                // PFC-2352: TODO: Support ID, hashes and tags
                String oid = null;
                String hashes = null;
                String tags = null;
                // PFC-2571: TODO: Add/Read modifier from meta-db
                AccountInfo modAccount = null;
                FileInfo archiveFile = FileInfoFactory.archivedFile(foInfo,
                    name, oid, Files.size(file), mySelf, modAccount, modDate,
                    version, hashes, tags);
                list.add(archiveFile);
            } catch (IOException ioe) {
                log.warning(ioe.getMessage());
            }
        }
        // Read-only, so others don't trash this.
        return Collections.unmodifiableList(list);
    }

    public Path getArchivedFile(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        Path subdirectory = archiveDirectory.resolve(
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName()))
            .getParent();
        if (Files.notExists(subdirectory)) {
            return null;
        }
        return getArchiveTarget(fileInfo);
    }

    /**
     * Comparator for comparing file versions.
     */
    private static class VersionComparator implements Comparator<Path> {
        public int compare(Path o1, Path o2) {
            return getVersionNumber(o1) - getVersionNumber(o2);
        }
    }

    /**
     * Restore a file version.
     *
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param target
     */
    public boolean restore(FileInfo versionInfo, Path target)
        throws IOException
    {
        Path archiveFile = getArchiveTarget(versionInfo);
        if (Files.notExists(archiveFile)) {
            // Try with the old format, adding _K_nnn to end of file name, after
            // extension.
            archiveFile = getOldArchiveTarget(versionInfo);
        }
        if (Files.exists(archiveFile)) {
            log.fine("Restoring " + versionInfo.getRelativeName() + " to "
                + target.toAbsolutePath());
            if (target.getParent() != null
                && Files.notExists(target.getParent()))
            {
                Files.createDirectories(target.getParent());
            }

            // Files.copy(archiveFile, target,
            // StandardCopyOption.REPLACE_EXISTING);
            PathUtils.copyFile(archiveFile, target);
            // FileUtils.copyFile(archiveFile, target);
            // #2256: New modification date. Otherwise conflict detection
            // triggers
            // target.setLastModified(versionInfo.getModifiedDate().getTime());
            return true;
        } else {
            return false;
        }
    }

    public int getVersionsPerFile() {
        return versionsPerFile;
    }

    public void setVersionsPerFile(int versionsPerFile) {
        this.versionsPerFile = versionsPerFile;
    }

    public synchronized long getSize() {
        if (size == null) {
            long s = PathUtils.calculateDirectorySizeAndCount(archiveDirectory)[0];
            Path sizeFile = archiveDirectory.resolve(SIZE_INFO_FILE);
            if (Files.exists(sizeFile)) {
                try {
                    s -= Files.size(sizeFile);
                } catch (IOException ioe) {
                    log.warning(ioe.getMessage());
                }
            }
            size = s;
            saveSize();
        }
        return size;
    }

    public void purge() throws IOException {
        PathUtils.recursiveDelete(archiveDirectory);
        size = 0L;
        saveSize();
    }

    /**
     * Delete archives older that a specified number of days.
     *
     * @param cleanupDate
     *            Age in days of archive files to delete.
     */
    public void cleanupOldArchiveFiles(Date cleanupDate) {
        log.info("Cleaning up " + archiveDirectory + " for files older than "
            + cleanupDate);

        if (Files.exists(archiveDirectory)) {
            cleanupOldArchiveFiles(archiveDirectory, cleanupDate);
        }
    }

    private static void cleanupOldArchiveFiles(Path file, Date cleanupDate) {
        if (Files.isDirectory(file)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(file)) {
                for (Path path : files) {
                    cleanupOldArchiveFiles(path, cleanupDate);
                }
            } catch (IOException ioe) {
                log.warning(ioe.getMessage());
            }
        } else {
            try {
                Date age = new Date(Files.getLastModifiedTime(file).toMillis());
                if (age.before(cleanupDate)) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Deleting old archive file " + file + " ("
                            + age + ')');
                    }
                    try {
                        Files.delete(file);
                    } catch (SecurityException e) {
                        log.severe("Could not delete archive file " + file);
                    }
                }
            } catch (IOException ioe) {
                log.warning("Could not read modification time of " + file);
            }
        }
    }

    private void saveSize() {
        Path sizeFile = archiveDirectory.resolve(SIZE_INFO_FILE);
        if (size == 0) {
            try {
                Files.deleteIfExists(sizeFile);
                return;
            } catch (IOException e) {
                log.fine("Unable to delete meta data file: " + sizeFile + ". "
                    + e);
            }
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(String
            .valueOf(size).getBytes());
        try {
            PathUtils.copyFromStreamToFile(bin, sizeFile);
            PathUtils.setAttributesOnWindows(sizeFile, true, true);
        } catch (IOException e) {
            log.fine("Unable to store size of archive to " + sizeFile);
        }
    }
}
