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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;

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
    private final File archDir;

    public CopyOrMoveFileArchiver(File archiveDirectory) {
        Reject.notNull(archiveDirectory, "archiveDirectory");
        Reject.ifFalse(archiveDirectory.isDirectory(),
            "archiveDirectory not a directory!");
        this.archDir = archiveDirectory;
    }

    public void archive(FileInfo fileInfo, File source, boolean forceKeepSource)
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(source, "source");

        File target = getArchiveTarget(fileInfo);

        if (target.exists()) {
            log.severe("File " + fileInfo
                + " seems to be archived already, doing nothing.");
            return;
        }

        if (target.getParentFile().mkdirs()) {
            boolean tryCopy = forceKeepSource;
            if (!forceKeepSource) {
                if (!source.renameTo(target)) {
                    log.severe("Failed to rename " + source
                        + ", falling back to copying");
                    tryCopy = true;
                }
            }
            if (tryCopy) {
                try {
                    FileUtils.copyFile(source, target);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to copy " + source, e);
                }
            }
        } else {
            log.severe("Failed to create directory: " + target.getParent());
        }
    }

    protected File getArchiveTarget(FileInfo fileInfo) {
        return new File(archDir, fileInfo.getName() + "_K_"
            + fileInfo.getVersion());
    }
}
