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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * An implementation of {@link FileArchiver} that tries to move a file to an
 * archive first, and falls back to copying otherwise, or if forced to.
 * <i>Note:</i> No support for removal of old files (yet) - special care of
 * directories might be required Archives are stored in an archives directory,
 * with suffix '_K_nnn', where 'nnn' is the version number. So 'data/info.txt'
 * archive version 6 would be 'archive/data/info.txt_K_6'.
 * 
 * @author dante
 */
public class CopyOrMoveFileArchiver implements FileArchiver {

    private static final Logger log = Logger
        .getLogger(CopyOrMoveFileArchiver.class.getName());
    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();
    private static final Pattern BASE_NAME_PATTERN = Pattern
        .compile("(.*)_K_\\d+");

    private final File archiveDirectory;
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
    public CopyOrMoveFileArchiver(File archiveDirectory, MemberInfo mySelf) {
        Reject.notNull(archiveDirectory, "archiveDirectory");
        Reject.ifFalse(archiveDirectory.isDirectory(),
            "archiveDirectory not a directory!");
        Reject.ifNull(mySelf, "Myself");
        this.archiveDirectory = archiveDirectory;
        // Default: Store unlimited # of files
        versionsPerFile = Integer.MAX_VALUE;
        this.mySelf = mySelf;
    }

    /**
     * @see FileArchiver#archive(FileInfo, File, boolean)
     */
    public void archive(FileInfo fileInfo, File source, boolean forceKeepSource)
        throws IOException
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(source, "source");

        File target = getArchiveTarget(fileInfo);

        if (target.exists()) {
            log.warning("File " + fileInfo
                + " seems to be archived already, doing nothing.");
            // Maybe throw Exception instead?
            return;
        }

        if (target.getParentFile().exists() || target.getParentFile().mkdirs())
        {
            // Reset cache
            size = null;
            boolean tryCopy = forceKeepSource;
            if (!tryCopy) {
                if (!source.renameTo(target)) {
                    log.severe("Failed to rename " + source
                        + ", falling back to copying");
                    tryCopy = true;
                }
            }
            if (tryCopy) {
                long lastModified = source.lastModified();
                FileUtils.copyFile(source, target);
                // Preserve last modification date.
                target.setLastModified(lastModified);
            }
            // Success, now check if we have to remove a file
            File[] list = getArchivedFiles(target.getParentFile(), fileInfo
                .getFilenameOnly());
            checkArchivedFile(list);
        } else {
            throw new IOException("Failed to create directory: "
                + target.getParent());
        }
    }

    private void checkArchivedFile(File[] versions) throws IOException {
        assert versions != null;
        if (versions.length <= versionsPerFile) {
            return;
        }

        Arrays.sort(versions, VERSION_COMPARATOR);
        int toDelete = versions.length - versionsPerFile;
        for (File f : versions) {
            if (toDelete <= 0) {
                break;
            }
            toDelete--;

            if (!f.delete()) {
                throw new IOException("Could not delete old version: " + f);
            }
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
        boolean check = checkRecursive(archiveDirectory, new HashSet<File>());
        size = null;
        return check;
    }

    private boolean checkRecursive(File dir, Set<File> checked) {
        assert dir != null && dir.isDirectory();
        assert checked != null;

        boolean allSuccessful = true;

        File[] flist = dir.listFiles();
        Map<String, Collection<File>> fileMap = new HashMap<String, Collection<File>>();
        for (File f : flist) {
            if (f.isDirectory()) {
                allSuccessful &= checkRecursive(f, checked);
            } else {
                String baseName = getBaseName(f);
                File vf = new File(dir, baseName);
                if (!checked.contains(vf)) {
                    checked.add(vf);
                }
                Collection<File> files = fileMap.get(baseName);
                if (files == null) {
                    files = new LinkedList<File>();
                    fileMap.put(baseName, files);
                }
                files.add(f);
            }
        }
        for (Collection<File> files : fileMap.values()) {
            try {
                checkArchivedFile(files.toArray(new File[files.size()]));
            } catch (IOException e) {
                allSuccessful = false;
                log.log(Level.WARNING, "Failed to check " + files, e);
            }
        }
        return allSuccessful;
    }

    private static String getBaseName(File file) {
        Matcher m = BASE_NAME_PATTERN.matcher(file.getName());
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalArgumentException("File not in archive: " + file);
        }
    }

    private File getArchiveTarget(FileInfo fileInfo) {
        return new File(archiveDirectory, fileInfo.getRelativeName() + "_K_"
            + fileInfo.getVersion());
    }

    private String getFileInfoName(File fileInArchive) {
        return buildFileName(archiveDirectory, fileInArchive);
    }

    private static String buildFileName(File baseDirectory, File file) {
        String fn = file.getName();
        int i = fn.lastIndexOf("_K_");
        if (i >= 0) {
            fn = fn.substring(0, i);
        }
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

    /**
     * Parse the file name for the last '_' and extract the following version
     * number. Like 'file.txt_K_45' returns 45.
     * 
     * @param file
     *            file to parse name.
     * @return the version.
     */
    private static int getVersionNumber(File file) {
        String tmp = file.getName();
        tmp = tmp.substring(tmp.lastIndexOf('_') + 1);
        return Integer.parseInt(tmp);
    }

    private static File[] getArchivedFiles(File directory, final String baseName)
    {
        return directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return belongsTo(name, baseName);
            }
        });
    }

    private static boolean belongsTo(String name, String baseName) {
        Matcher m = BASE_NAME_PATTERN.matcher(name);
        if (m.matches()) {
            return OSUtil.isWindowsSystem()
                && m.group(1).equalsIgnoreCase(baseName)
                || m.group(1).equals(baseName);
        }
        return false;
    }

    public ArchiveMode getArchiveMode() {
        return ArchiveMode.FULL_BACKUP;
    }

    public List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        // Find archive subdirectory.
        File subdirectory = FileUtils.buildFileFromRelativeName(
            archiveDirectory, fileInfo.getRelativeName()).getParentFile();
        if (!subdirectory.exists()) {
            return Collections.emptyList();
        }

        File target = getArchiveTarget(fileInfo);
        File[] archivedFiles = getArchivedFiles(target.getParentFile(),
            fileInfo.getFilenameOnly());
        if (archivedFiles == null || archivedFiles.length == 0) {
            return Collections.emptyList();
        }
        List<FileInfo> list = new ArrayList<FileInfo>();
        FolderInfo foInfo = fileInfo.getFolderInfo();
        for (File file : archivedFiles) {
            int version = getVersionNumber(file);
            Date modDate = new Date(file.lastModified());
            String name = getFileInfoName(file);
            FileInfo archiveFile = FileInfoFactory.archivedFile(foInfo, name,
                file.length(), mySelf, modDate, version);
            list.add(archiveFile);
        }
        return list;
    }

    /**
     * Comparator for comparing file versions.
     */
    private static class VersionComparator implements Comparator<File> {
        public int compare(File o1, File o2) {
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
    public boolean restore(FileInfo versionInfo, File target)
        throws IOException
    {

        File archiveFile = getArchiveTarget(versionInfo);
        if (archiveFile.exists()) {
            log.info("Restoring " + versionInfo.getRelativeName() + " to "
                + target.getAbsolutePath());
            FileUtils.copyFile(archiveFile, target);
            target.setLastModified(versionInfo.getModifiedDate().getTime());
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
            size = FileUtils.calculateDirectorySizeAndCount(archiveDirectory)[0];
        }
        return size;
    }

    public void purge() throws IOException {
        FileUtils.recursiveDelete(archiveDirectory);
    }
}
