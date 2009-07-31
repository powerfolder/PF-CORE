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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * An implementation of {@link FileArchiver} that tries to move a file to an
 * archive first, and falls back to copying otherwise, or if forced to.
 * <i>Note:</i> No support for removal of old files (yet) - special care of
 * directories might be required
 * 
 * @author dante
 */
public class CopyOrMoveFileArchiver implements FileArchiver {
    private static final Logger log = Logger
        .getLogger(CopyOrMoveFileArchiver.class.getName());
    private static final Comparator<File> VERSION_COMPARATOR = new Comparator<File>()
    {

        public int compare(File o1, File o2) {
            return getVersionNumber(o1) - getVersionNumber(o2);
        }

    };
    private static final Pattern BASE_NAME = Pattern.compile("(.*)_K_\\d+");
    private final File archDir;
    private volatile int versionsPerFile;

    /**
     * Constructs a new FileArchiver which stores backups in the given
     * directory.
     * 
     * @param archiveDirectory
     */
    public CopyOrMoveFileArchiver(File archiveDirectory) {
        Reject.notNull(archiveDirectory, "archiveDirectory");
        Reject.ifFalse(archiveDirectory.isDirectory(),
            "archiveDirectory not a directory!");
        this.archDir = archiveDirectory;
    }

    /**
     * @see de.dal33t.powerfolder.disk.FileArchiver#archive(de.dal33t.powerfolder.light.FileInfo,
     *      java.io.File, boolean)
     */
    public void archive(FileInfo fileInfo, File source, boolean forceKeepSource)
        throws IOException
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(source, "source");

        File target = getArchiveTarget(fileInfo);

        if (target.exists()) {
            log.severe("File " + fileInfo
                + " seems to be archived already, doing nothing.");
            // Maybe throw Exception instead?
            return;
        }

        if (target.getParentFile().exists() || target.getParentFile().mkdirs())
        {
            boolean tryCopy = forceKeepSource;
            if (!forceKeepSource) {
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
            File[] list = getArchivedFiles(target.getParentFile(),
                getBaseName(fileInfo));
            checkArchivedFile(list);
        } else {
            throw new IOException("Failed to create directory: "
                + target.getParent());
        }
    }

    private void checkArchivedFile(File[] versions) throws IOException {
        assert versions != null;
        if (versions.length <= versionsPerFile || versionsPerFile == 0) {
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
     * Set the number of versions to keep per file. Setting 0 will keep all
     * versions. Setting a lesser number as the current one will have no
     * immediate effect on the archive. To perform maintenance, a call to
     * maintain() is required.
     * 
     * @param versionsPerFile
     */
    public void setVersionsPerFile(int versionsPerFile) {
        Reject.ifTrue(versionsPerFile < 0, "versionsPerFile was "
            + versionsPerFile);
        this.versionsPerFile = versionsPerFile;
    }

    /**
     * Tries to ensure that only the allowed amount of versions per file is in
     * the archive.
     * 
     * @return true the maintenance worked successfully for all files, false if
     *         it failed for at least one file
     */
    public boolean maintain() {
        return checkRecursive(archDir, new HashSet<File>());
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
                    Collection<File> files = fileMap.get(baseName);
                    if (files == null) {
                        files = new LinkedList<File>();
                        fileMap.put(baseName, files);
                    }
                }
            }
        }
        for (Collection<File> files : fileMap.values()) {
            try {
                checkArchivedFile(files.toArray(new File[0]));
            } catch (IOException e) {
                allSuccessful = false;
                log.log(Level.WARNING, "Failed to check " + files, e);
            }
        }
        return allSuccessful;
    }

    protected String getBaseName(FileInfo fileInfo) {
        return fileInfo.getName();
    }

    private String getBaseName(File file) {
        Matcher m = BASE_NAME.matcher(file.getName());
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalArgumentException("File not in archive: " + file);
        }
    }

    private File getArchiveTarget(FileInfo fileInfo) {
        return new File(archDir, getBaseName(fileInfo) + "_K_"
            + fileInfo.getVersion());
    }

    private static int getVersionNumber(File file) {
        String tmp = file.getName();
        tmp = tmp.substring(tmp.lastIndexOf('_') + 1);
        return Integer.parseInt(tmp);
    }

    private File[] getArchivedFiles(File dir, final String baseName) {
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return belongsTo(name, baseName);
            }
        });
    }

    private boolean belongsTo(String name, String baseName) {
        Matcher m = BASE_NAME.matcher(name);
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
}
