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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.problem.FileProblemHelper;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Util;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A file archiver that tries to move a file to an archive first, and falls back
 * to copying otherwise, or if forced to. Archives are stored in an archives
 * directory, with suffix '_K_nnn', where 'nnn' is the version number.
 * So 'data/info.txt' archive version 6 would be 'archive/data/info.txt_K_6'.
 * <p>
 * Nightly maintenance via {@link #maintainAndCleanup} enforces version limits,
 * removes old archives, and recovers lost DAO entries in a single walk.
 *
 * @author dante
 */
public class FileArchiverImpl implements FileArchiver {

    private static final Logger log = Logger.getLogger(FileArchiverImpl.class.getName());
    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();
    private static final Pattern BASE_NAME_PATTERN = Pattern.compile("(.*)_K_\\d+(.*)");
    private static final String SIZE_INFO_FILE = "Size";
    private static final String META_SUFFIX = ".meta";
    private static final String META_WRITING_SUFFIX = META_SUFFIX + ".writing";

    private final Path archiveDirectory;
    private volatile int versionsPerFile;
    private MemberInfo mySelf;

    /*
     * Cached size of this file archive.
     */
    private Long size;

    /**
     * Constructs a new FileArchiver which stores backups in the given
     * directory.
     *
     * @param archiveDirectory
     * @param mySelf           myself
     */
    public FileArchiverImpl(Path archiveDirectory, MemberInfo mySelf) {
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

    @Override
    public void archive(FileInfo fileInfo, Path source, boolean forceKeepSource)
            throws IOException {

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
            // PFS-1794: Happens 2136x
            if (log.isLoggable(Level.FINE)) {
                log.fine("File " + fileInfo.toDetailString()
                        + " seems to be archived already, doing nothing.");
            }
            return;
        }

        Long oldSize = size;

        try {
            if (Files.notExists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
        } catch (IOException faee) {
            // Ignore.
        }

        if (Files.exists(target.getParent())) {
            // Reset cache
            // size = null;
            boolean tryCopy = forceKeepSource;
            if (!tryCopy) {
                try {
                    // // PFS-1794: Replace existing target file atomically.
                    Files.move(source, target,
                            StandardCopyOption.REPLACE_EXISTING);
                    if (size != null && Files.exists(target)) {
                        size += Files.size(target);
                    }
                } catch (IOException ioe) {
                    if (ioe.getMessage().toLowerCase().contains("too long") || FileProblemHelper.isTooLong(target.getFileName().toString())) {
                        log.warning("Failed to archive " + source + ": " + ioe.getMessage());
                        return;
                    }
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

            writeMeta(target, fileInfo);

            // Success, now check if we have to remove a file
            List<Path> list = getArchivedFiles(target.getParent(),
                    fileInfo.getFilenameOnly());
            checkArchivedFile(list);

            if (oldSize != null && size != null && oldSize.longValue() != size.longValue()) {
                saveSize();
            }
        } else {
            throw new IOException("Failed to create directory: "
                    + target.getParent());
        }
    }

    Path getArchiveDir() {
        return archiveDirectory;
    }

    private void checkArchivedFile(Collection<Path> versions)
            throws IOException {

        assert versions != null;
        if (versionsPerFile < 0) {
            // Unlimited. Don't check
            return;
        }

        if (versions.size() <= versionsPerFile) {
            return;
        }

        Path[] versionArray = versions.toArray(new Path[0]);
        Arrays.sort(versionArray, VERSION_COMPARATOR);
        int toDelete = versionArray.length - versionsPerFile;
        Long oldSize = size;
        for (Path f : versionArray) {
            if (toDelete <= 0) {
                break;
            }
            toDelete--;

            long len = Files.size(f);
            try {
                Files.delete(f);
                deleteMetaIfExists(f);
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
        if (!Objects.equals(oldSize, size)) {
            saveSize();
        }
    }

    /**
     * Visitor for {@link #walkArchive}. Called once per unique base file name
     * in each directory with all its {@code _K_N} version paths.
     */
    interface ArchiveVisitor {
        /**
         * @param baseName the original file base name (without {@code _K_N})
         * @param dir      the directory containing the versions
         * @param versions all version paths for this base name
         * @return true if processing succeeded (used to track overall success)
         */
        boolean visit(String baseName, Path dir, Collection<Path> versions);
    }

    /**
     * Recursively walks the archive directory, groups files by base name
     * per directory, and calls the visitor for each group.
     * Empty subdirectories are deleted after visiting if
     * {@code deleteEmptyDirs} is true.
     *
     * @return true if all visitor calls and directory operations succeeded
     */
    private boolean walkArchive(Path dir, ArchiveVisitor visitor, boolean deleteEmptyDirs) {
        if (dir == null || Files.notExists(dir) || !Files.isDirectory(dir)) {
            return true;
        }

        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                entries.add(p);
            }
        } catch (IOException ioe) {
            log.warning(ioe.toString());
            return false;
        }

        boolean allSuccessful = true;
        Map<String, Collection<Path>> fileMap = new LinkedHashMap<>();

        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (name.equals(SIZE_INFO_FILE) || isMetaFileName(name)) {
                continue;
            }
            if (Files.isDirectory(entry)) {
                boolean subSuccess = walkArchive(entry, visitor, deleteEmptyDirs);
                if (deleteEmptyDirs && subSuccess) {
                    try {
                        Files.delete(entry);
                    } catch (DirectoryNotEmptyException dnee) {
                        // Expected: directory still holds archive versions within retention
                        log.finer(dnee.toString());
                    } catch (IOException ioe) {
                        log.warning(ioe.toString());
                    }
                }
                allSuccessful &= subSuccess;
            } else {
                String baseName;
                try {
                    baseName = getBaseName(entry);
                } catch (RuntimeException e) {
                    log.log(Level.WARNING, entry + ": Skipping: " + e.toString());
                    continue;
                }
                fileMap.computeIfAbsent(baseName, k -> new LinkedList<>()).add(entry);
            }
        }

        for (Map.Entry<String, Collection<Path>> e : fileMap.entrySet()) {
            allSuccessful &= visitor.visit(e.getKey(), dir, e.getValue());
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
     * @param file file to parse name.
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
                                               final String baseName) {
        List<Path> ret = new ArrayList<>();

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (belongsTo(FileInfoFactory.decodeIllegalChars(file.getFileName().toString()), baseName)) {
                    ret.add(file);
                }
            }
        } catch (IOException ioe) {
            log.warning(ioe.toString());
        }

        return ret;
    }

    /**
     * Use the {@link FileArchiverImpl#BASE_NAME_PATTERN} to match the name and
     * extension of the {@code baseName} to match the file's name and extension
     * of {@code name}.
     *
     * @param name     Name of a file in the history
     * @param baseName Name of the actual file in the folder
     * @return {@code True} if {@code name} has the same filename and extension
     * as {@code baseName}
     */
    private static boolean belongsTo(String name, String baseName) {
        Matcher m = BASE_NAME_PATTERN.matcher(name);
        if (m.matches()) {
            return Util.equalsRelativeName(m.group(1) + m.group(2), baseName);
        }
        return false;
    }

    /**
     * Search the history for an archived file of {@code fileInfo}
     *
     * @param fileInfo The file to check for in the history.
     * @return {@code True} if there is a file in the history, {@code false}
     * otherwise.
     */
    @Override
    public boolean hasArchivedFileInfo(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");

        Path subdirectory;
        try {
            // Find archive subdirectory.
            subdirectory = archiveDirectory.resolve(
                    FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName())).getParent();
        } catch (InvalidPathException e) {
            // PFS-2000:
            log.warning("Unable to resolve versions for file: "
                    + fileInfo.toDetailString() + ". " + e);
            return false;
        }
        if (Files.notExists(subdirectory)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files
                .newDirectoryStream(subdirectory)) {
            String fn = fileInfo.getFilenameOnly();

            // get rid of the extension, if present
            int ind = fn.lastIndexOf('.');
            if (ind > -1) {
                fn = fn.substring(0, ind);
            }
            fn = FileInfoFactory.encodeIllegalChars(fn);

            for (Path file : files) {
                if (file.getFileName().toString().startsWith(fn)) {
                    return true;
                }
            }
        } catch (IOException ioe) {
            log.warning(ioe.toString());
        }

        return false;
    }

    /**
     * Inspect the file in the file history (in meta folder). Files in the
     * history are named like this:
     * <code>&lt;filename&gt;_K_&lt;number&gt;.&lt;extension&gt;</code>.<br />
     * Take those into account that are in the same relative directory and start
     * with the same <code>filename</code>.<br />
     * <br />
     * <b>Attention:</b> The returned list may not be orderd!
     *
     * @param fileInfo Get all files in the history that are older versions of
     *                 {@code fileInfo}.
     * @return A list of all older versions of the passed file in the history.
     */
    @Override
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
        List<Path> archivedFiles = getArchivedFiles(target.getParent(), fileInfo.getFilenameOnly());
        if (archivedFiles == null || archivedFiles.size() == 0) {
            return Collections.emptyList();
        }
        List<FileInfo> list = new ArrayList<>();
        FolderInfo foInfo = fileInfo.getFolderInfo();
        for (Path file : archivedFiles) {
            try {
                FileInfo archiveFile = readMeta(file);
                if (archiveFile == null) {
                    int version = getVersionNumber(file);
                    Date modDate = new Date(Files.getLastModifiedTime(file)
                            .toMillis());
                    String name = getFileInfoName(file);
                    archiveFile = FileInfoFactory.archivedFile(foInfo,
                            name, null, Files.size(file), mySelf,
                            FileInfo.UNKNOWN_FROM_ARCHIVE, modDate, version,
                            null, null);
                }
                list.add(archiveFile);
            } catch (IOException ioe) {
                log.warning(ioe.toString());
            }
        }
        // Read-only, so others don't trash this.
        return Collections.unmodifiableList(list);
    }

    /**
     * Calls {@link #getArchivedFilesInfos(FileInfo)} and sorts the returned
     * list according to the version number.
     *
     * @param fileInfo
     *            The file to get the archived versions of.
     * @return A alpha-numerically ascending sorted list of all versions of
     *         {@code fileInfo} in the history.
     */
    @Override
    public List<FileInfo> getSortedArchivedFilesInfos(FileInfo fileInfo) {
        List<FileInfo> versions = new ArrayList<>(getArchivedFilesInfos(fileInfo));
        versions.sort(Comparator.comparingInt(FileInfo::getVersion));
        return versions;
    }


    /**
     * @param fileInfo The file to get the archived version of.
     * @return The path to the file in the history.
     */
    @Override
    public Path getArchivedFile(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        Path subdirectory;
        try {
            subdirectory = archiveDirectory
                    .resolve(FileInfoFactory
                            .encodeIllegalChars(fileInfo.getRelativeName()))
                    .getParent();
        } catch (InvalidPathException e) {
            // PFS-2000:
            log.warning("Unable to resolve versions for file: "
                    + fileInfo.toDetailString() + ". " + e);
            return null;
        }
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
     * @param versionInfo the FileInfo of the archived file.
     * @param target
     */
    @Override
    public boolean restore(FileInfo versionInfo, Path target)
            throws IOException {
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
                    && Files.notExists(target.getParent())) {
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

    /**
     * Restore a file version, archiving the current file first to preserve it.
     *
     * @param versionInfo the FileInfo of the archived version to restore.
     * @param currentFile the FileInfo of the current file (may be null if no current file exists).
     * @param target the target path to restore to.
     * @return true if restore succeeded, false if archived version not found.
     */
    @Override
    public boolean restore(FileInfo versionInfo, FileInfo currentFile, Path target)
            throws IOException {
        // Temporarily suspend version limit to prevent deletion of versions
        int originalLimit = this.versionsPerFile;
        this.versionsPerFile = -1;
        try {
            // Archive current file before overwriting
            if (currentFile != null && !currentFile.isDeleted() && Files.exists(target)) {
                archive(currentFile, target, true);
            }
            return restore(versionInfo, target);
        } finally {
            this.versionsPerFile = originalLimit;
            Path archiveTarget = getArchiveTarget(versionInfo);
            if (Files.exists(archiveTarget.getParent())) {
                List<Path> list = getArchivedFiles(archiveTarget.getParent(),
                        versionInfo.getFilenameOnly());
                checkArchivedFile(list);
            }
        }
    }

    @Override
    public int getVersionsPerFile() {
        return versionsPerFile;
    }

    @Override
    public void setVersionsPerFile(int versionsPerFile) {
        this.versionsPerFile = versionsPerFile;
    }

    @Override
    public synchronized long getSize() {
        Long thisSize = size;
        if (thisSize == null) {
            long s = calculateUserPayloadSize(archiveDirectory);
            size = s;
            thisSize = s;
            saveSize();
        }
        return thisSize;
    }

    private static long calculateUserPayloadSize(Path dir) {
        if (Files.notExists(dir)) {
            return 0L;
        }
        long[] total = {0L};
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (!name.equals(SIZE_INFO_FILE) && !isMetaFileName(name)) {
                        total[0] += attrs.size();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ioe) {
            log.warning("Unable to calculate archive size of "
                    + dir + ": " + ioe);
        }
        return total[0];
    }

    /**
     * Purge the whole folder
     * @param folder
     * @param account
     * @throws IOException
     */
    @Override
    public void purge(Folder folder, Account account) throws IOException {
        Reject.ifFalse(folder.getFileArchiver() == this, "Folder archive mismatch");

        long freedSpace = size != null ? size : 0L;
        purge0(archiveDirectory);
        size = 0L; saveSize();

        folder.fireArchivePurged();

        String logMessage = "Successfully cleared versioning of folder " + folder.getName() + " by " + account;
        if (freedSpace > 0) {
            logMessage += " (Removed " + FileUtils.byteCountToDisplaySize(freedSpace) + ")";
        }
        log.info(logMessage);
    }

    /**
     * purge a specific file or directory
     * @param fileInfo
     * @param folder
     * @param account
     * @throws IOException
     */
    @Override
    public void purge(FileInfo fileInfo, Folder folder, Account account) throws IOException {
        Reject.ifFalse(folder.getFileArchiver() == this, "Folder archive mismatch");

        long freedSpace = 0;
        boolean purgedSubdirs = false;
        if (fileInfo.isDiretory()) {
            Path dir = archiveDirectory.resolve(fileInfo.getRelativeName());
            purge0(dir);
            purgedSubdirs = true;
        } else {
            for (FileInfo archivedFileInfo : getArchivedFilesInfos(fileInfo)) {
                Path archivedFile = getArchivedFile(archivedFileInfo);
                freedSpace += Files.size(archivedFile);
                purge0(archivedFile);
                deleteMetaIfExists(archivedFile);
            }
        }
        if (!purgedSubdirs && size != null) {
            size -= freedSpace;
        } else {
            size = null;
        }
        saveSize();

        folder.fireArchivePurged();
        String logMessage =
            "Successfully cleared versioning of " + (fileInfo.isDiretory() ? "Directory" : "File") + fileInfo.getRelativeName() + " by " + account;
        logMessage = purgedSubdirs ? logMessage : logMessage + " (Removed "
            + FileUtils.byteCountToDisplaySize(freedSpace) + ")";
        log.info(logMessage);

        if (purgedSubdirs) {
            folder.getController().getIOProvider().startIO(this::getSize);
        }
    }

    private void purge0(Path path) throws IOException {
        PathUtils.recursiveDelete(path);
    }

    private static boolean isMetaFileName(String name) {
        return name.endsWith(META_SUFFIX) || name.endsWith(META_WRITING_SUFFIX);
    }

    private static Path metaPathFor(Path archivedFile) {
        Path parent = archivedFile.getParent();
        String name = archivedFile.getFileName().toString();
        return parent.resolve(name + META_SUFFIX);
    }

    private static Path metaWritingPathFor(Path archivedFile) {
        Path parent = archivedFile.getParent();
        String name = archivedFile.getFileName().toString();
        return parent.resolve(name + META_WRITING_SUFFIX);
    }

    private void writeMeta(Path archivedFile, FileInfo fileInfo) {
        Path metaFile = metaPathFor(archivedFile);
        Path tempFile = metaWritingPathFor(archivedFile);
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                out.writeObject(fileInfo);
            }
            try {
                Files.move(tempFile, metaFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ame) {
                Files.move(tempFile, metaFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warning("Unable to save archive metadata to " + metaFile + ": " + e);
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
        }
    }

    private FileInfo readMeta(Path archivedFile) {
        Path metaFile = metaPathFor(archivedFile);
        if (!Files.exists(metaFile)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(metaFile))) {
            Object value = in.readObject();
            if (value instanceof FileInfo) {
                return (FileInfo) value;
            }
            log.warning("Archive metadata at " + metaFile
                    + " is not a FileInfo, ignoring: " + value);
        } catch (IOException | ClassNotFoundException e) {
            log.warning("Unable to read archive metadata at " + metaFile + ": " + e);
        }
        return null;
    }

    void deleteMetaIfExists(Path archivedFile) {
        try {
            Files.deleteIfExists(metaPathFor(archivedFile));
            Files.deleteIfExists(metaWritingPathFor(archivedFile));
        } catch (IOException e) {
            log.warning("Unable to delete archive metadata next to "
                    + archivedFile + ": " + e);
        }
    }

    private void saveSize() {
        Path sizeFile = archiveDirectory.resolve(SIZE_INFO_FILE);
        if (size == null) {
            try {
                Files.deleteIfExists(sizeFile);
            } catch (IOException e) {
                log.warning("Unable to delete " + sizeFile + ". " + e);
            }
            return;
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(String.valueOf(size).getBytes());
        try {
            PathUtils.copyFromStreamToFile(bin, sizeFile);
            PathUtils.setAttributesOnWindows(sizeFile, true, true);
        } catch (IOException e) {
            log.fine("Unable to store size of archive to " + sizeFile);
        }
    }

    /**
     * Combined nightly maintenance: recover lost DAO entries, enforce version
     * limits, and delete old archives — all in a single walk of the archive
     * directory tree.
     *
     * @param cleanupDate delete archive files older than this date,
     *                    or null to skip age-based cleanup
     * @param dao         the FileInfo DAO to check for existing entries
     * @param folderInfo  the folder these archives belong to
     * @param myAccount   the account info of this device
     * @return deleted FileInfo entries for archived files missing from the DAO,
     *         ready to be stored
     */
    @Override
    public synchronized List<FileInfo> maintainAndCleanup(
        Date cleanupDate, FileInfoDAO dao, FolderInfo folderInfo, AccountInfo myAccount)
    {
        List<FileInfo> lostFileInfos = new ArrayList<>();
        if (Files.notExists(archiveDirectory)) {
            size = 0L;
            saveSize();
            return lostFileInfos;
        }
        
        long[] calculatedSize = {0L};
        walkArchive(archiveDirectory, (baseName, dir, versions) -> {
            
            recoverLostFileInfo(versions, dao, folderInfo, myAccount, lostFileInfos);
            enforceVersionLimits(versions);
            cleanupOldVersions(versions, cleanupDate);

            // Sum up surviving files
            for (Path version : versions) {
                if (Files.exists(version)) {
                    try {
                        calculatedSize[0] += Files.size(version);
                    } catch (IOException e) {
                        log.warning("Could not read size of " + version + ": " + e);
                    }
                }
            }
            
            return true;
        }, true);
        
        size = calculatedSize[0];
        saveSize();
        
        return lostFileInfos;
    }

    private void recoverLostFileInfo(Collection<Path> versions, FileInfoDAO dao,
        FolderInfo folderInfo, AccountInfo myAccount, List<FileInfo> lostFileInfos)
    {
        try {
            String relativeName = buildFileName(archiveDirectory, versions.iterator().next());
            FileInfo lookup = FileInfoFactory.lookupInstance(folderInfo, relativeName);
            if (dao.find(lookup, null) != null) {
                return;
            }
            Path newestPath = findNewestVersionPath(versions);
            if (newestPath == null) {
                return;
            }
            int version = getVersionNumber(newestPath);
            Date modDate = new Date(Files.getLastModifiedTime(newestPath).toMillis());
            FileInfo entry = readMeta(newestPath);
            if (entry == null) {
                entry = FileInfoFactory.archivedFile(folderInfo, relativeName,
                        null, Files.size(newestPath), mySelf,
                        FileInfo.UNKNOWN_FROM_ARCHIVE, modDate, version,
                        null, null);
            }
            lostFileInfos.add(FileInfoFactory.deletedFile(entry, mySelf, myAccount, modDate));
        } catch (IllegalArgumentException | IOException e) {
            log.warning("Skipping unrecognized archive file: " + e);
        }
    }

    private void enforceVersionLimits(Collection<Path> versions) {
        try {
            checkArchivedFile(versions);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to check " + versions, e);
        }
    }

    private void cleanupOldVersions(Collection<Path> versions, Date cleanupDate) {
        if (cleanupDate == null) {
            return;
        }
        for (Path version : versions) {
            if (Files.notExists(version)) {
                continue;
            }
            try {
                Date age = new Date(Files.getLastModifiedTime(version).toMillis());
                if (age.before(cleanupDate)) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Deleting old archive file " + version + " (" + age + ')');
                    }
                    Files.delete(version);
                    deleteMetaIfExists(version);
                }
            } catch (SecurityException e) {
                log.severe("Could not delete archive file " + version + ". " + e);
            } catch (IOException e) {
                log.warning("Could not read/delete " + version + ". " + e);
            }
        }
    }

    private static Path findNewestVersionPath(Collection<Path> versionPaths) {
        Path newest = null;
        int highestVersion = -1;
        for (Path file : versionPaths) {
            int version = getVersionNumber(file);
            if (version > highestVersion) {
                highestVersion = version;
                newest = file;
            }
        }
        return newest;
    }
}
