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
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Util;
import de.schlichtherle.truezip.file.TFile;

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
    private static final String SIZE_INFO_FILE = "Size";

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
        versionsPerFile = -1;
        this.mySelf = mySelf;
        this.size = loadSize();
    }

    private Long loadSize() {
        File sizeFile = new File(archiveDirectory, SIZE_INFO_FILE);
        if (!sizeFile.exists()) {
            return null;
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(sizeFile);
            byte[] buf = StreamUtils.readIntoByteArray(fin);
            return Long.valueOf(new String(buf));
        } catch (Exception e) {
            log.fine("Unable to read size of archive to " + sizeFile + ". " + e);
            return null;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        }
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
            log.warning("File " + fileInfo.toDetailString()
                + " seems to be archived already, doing nothing.");
            // Maybe throw Exception instead?
            return;
        }

        long oldSize = getSize();
        if (target.getParentFile().exists() || target.getParentFile().mkdirs())
        {
            // Reset cache
            // size = null;
            boolean tryCopy = forceKeepSource;
            if (!tryCopy) {
                if (!source.renameTo(target)) {
                    log.severe("Failed to rename " + source
                        + ", falling back to copying");
                    tryCopy = true;
                } else if (size != null) {
                    size += target.length();
                }
            }
            if (tryCopy) {
                long lastModified = source.lastModified();
                FileUtils.copyFile(source, target);
                // Preserve last modification date.
                target.setLastModified(lastModified);
                if (size != null) {
                    size += target.length();
                }
            }

            if (log.isLoggable(Level.FINE)) {
                log.fine("Archived " + fileInfo.toDetailString() + " from "
                    + source + " to " + target);
            }

            // Success, now check if we have to remove a file
            File[] list = getArchivedFiles(target.getParentFile(),
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

    private void checkArchivedFile(File[] versions) throws IOException {
        assert versions != null;
        if (versionsPerFile < 0) {
            // Unlimited. Don't check
            return;
        }
        if (versions.length <= versionsPerFile) {
            return;
        }

        Arrays.sort(versions, VERSION_COMPARATOR);
        int toDelete = versions.length - versionsPerFile;
        long oldSize = size;
        for (File f : versions) {
            if (toDelete <= 0) {
                break;
            }
            toDelete--;

            long len = f.length();
            if (!f.delete()) {
                throw new IOException("Could not delete old version: " + f);
            } else {
                if (size != null) {
                    size -= len;
                }
                if (log.isLoggable(Level.FINE)) {
                    log.fine("checkArchivedFile: Deleted archived file " + f);
                }
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
        boolean check = checkRecursive(archiveDirectory, new HashSet<File>());
        size = null;
        return check;
    }

    private boolean checkRecursive(File dir, Set<File> checked) {
        assert dir != null && dir.isDirectory();
        assert checked != null;

        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            // Is empty or not existent.
            return true;
        }

        boolean allSuccessful = true;

        File[] flist = dir.listFiles();
        Map<String, Collection<File>> fileMap = new HashMap<String, Collection<File>>();
        for (File f : flist) {
            if (f.getName().equals(SIZE_INFO_FILE)) {
                continue;
            }
            if (f.isDirectory()) {
                boolean thisSuccessfuly = checkRecursive(f, checked);
                if (thisSuccessfuly) {
                    f.delete();
                }
                allSuccessful &= thisSuccessfuly;
            } else {
                String baseName = getBaseName(f);
                File vf = new TFile(dir, baseName);
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

    private TFile getArchiveTarget(FileInfo fileInfo) {
        return new TFile(archiveDirectory,
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName())
                + "_K_" + fileInfo.getVersion());
    }

    private String getFileInfoName(File fileInArchive) {
        return buildFileName(archiveDirectory, fileInArchive);
    }

    private static String buildFileName(File baseDirectory, File file) {
        String fn = FileInfoFactory.decodeIllegalChars(file.getName());
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
            fn = FileInfoFactory.decodeIllegalChars(parent.getName()) + '/'
                + fn;
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
            return Util.equalsRelativeName(m.group(1), baseName);
        }
        return false;
    }

    public ArchiveMode getArchiveMode() {
        return ArchiveMode.FULL_BACKUP;
    }

    public boolean hasArchivedFileInfo(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        // Find archive subdirectory.
        File subdirectory = FileUtils.buildFileFromRelativeName(
            archiveDirectory,
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName()))
            .getParentFile();
        if (!subdirectory.exists()) {
            return false;
        }
        String[] files = subdirectory.list();
        if (files == null || files.length == 0) {
            return false;
        }
        String fn = FileInfoFactory.encodeIllegalChars(fileInfo
            .getFilenameOnly());
        for (String fileName : files) {
            if (fileName.startsWith(fn)) {
                return true;
            }
        }
        return false;
    }

    public List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        // Find archive subdirectory.
        File subdirectory = FileUtils.buildFileFromRelativeName(
            archiveDirectory,
            FileInfoFactory.encodeIllegalChars(fileInfo.getRelativeName()))
            .getParentFile();
        if (!subdirectory.exists()) {
            return Collections.emptyList();
        }

        File target = getArchiveTarget(fileInfo);
        File[] archivedFiles = getArchivedFiles(target.getParentFile(),
            FileInfoFactory.encodeIllegalChars(fileInfo.getFilenameOnly()));
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
        TFile archiveFile = getArchiveTarget(versionInfo);
        if (archiveFile.exists()) {
            log.fine("Restoring " + versionInfo.getRelativeName() + " to "
                + target.getAbsolutePath());
            if (target.getParentFile() != null
                && !target.getParentFile().exists())
            {
                target.getParentFile().mkdirs();
            }
            archiveFile.cp(target);
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
            long s = FileUtils.calculateDirectorySizeAndCount(archiveDirectory)[0];
            File sizeFile = new File(archiveDirectory, SIZE_INFO_FILE);
            if (sizeFile.exists()) {
                s -= sizeFile.length();
            }
            size = s;
            saveSize();
        }
        return size;
    }

    public void purge() throws IOException {
        FileUtils.recursiveDelete(archiveDirectory);
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

        cleanupOldArchiveFiles(archiveDirectory, cleanupDate);
    }

    private static void cleanupOldArchiveFiles(File file, Date cleanupDate) {
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                cleanupOldArchiveFiles(file1, cleanupDate);
            }
        } else {
            Date age = new Date(file.lastModified());
            if (age.before(cleanupDate)) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Deleting old archive file " + file + " (" + age
                        + ')');
                }
                try {
                    file.delete();
                } catch (SecurityException e) {
                    log.severe("Could not delete archive file " + file);
                }
            }
        }
    }

    private void saveSize() {
        File sizeFile = new File(archiveDirectory, SIZE_INFO_FILE);
        ByteArrayInputStream bin = new ByteArrayInputStream(String
            .valueOf(size).getBytes());
        try {
            FileUtils.copyFromStreamToFile(bin, sizeFile);
        } catch (IOException e) {
            log.fine("Unable to store size of archive to " + sizeFile);
        }
    }
}
