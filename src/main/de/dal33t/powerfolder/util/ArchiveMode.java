package de.dal33t.powerfolder.util;

import java.io.File;
import java.util.logging.Logger;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;

public enum ArchiveMode {
    NO_BACKUP("archive.no_backup") {

        @Override
        public FileArchiver getInstance(Folder f) {
            return null;
        }
    },
    NORMAL_BACKUP("archive.full_backup") {

        @Override
        public FileArchiver getInstance(Folder f) {
            File archive = new File(f.getSystemSubDir(), "archive");
            if (!archive.mkdirs()) {
                log
                    .warning("Failed to create archive directory in system subdirectory: "
                        + archive);
            }
            return new CopyOrMoveFileArchiver(archive);
        }

    };

    private static Logger log = Logger.getLogger(ArchiveMode.class.getName());
    private final String tkey;

    ArchiveMode(String tkey) {
        assert StringUtils.isNotEmpty(tkey);
        this.tkey = tkey;
    }

    /**
     * Simplifies usage in GUI
     */
    @Override
    public String toString() {
        return Translation.getTranslation(tkey);
    }

    public abstract FileArchiver getInstance(Folder f);
}
