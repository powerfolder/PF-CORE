package de.dal33t.powerfolder.util;

import java.io.File;
import java.util.logging.Logger;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.schlichtherle.truezip.file.TFile;

public enum ArchiveMode {
    FULL_BACKUP("archive.full_backup") {

        @Override
        public FileArchiver getInstance(Folder f) {
            File archive = new TFile(f.getSystemSubDir(), "archive");
            if (!f.checkIfDeviceDisconnected() && !archive.exists()
                && !archive.mkdirs())
            {
                log.warning("Failed to create archive directory in system subdirectory: "
                    + archive);
            }
            return new CopyOrMoveFileArchiver(archive, f.getController()
                .getMySelf().getInfo());
        }

    };

    private static Logger log = Logger.getLogger(ArchiveMode.class.getName());
    private final String key;

    ArchiveMode(String key) {
        assert StringUtils.isNotEmpty(key);
        this.key = key;
    }

    /**
     * Simplifies usage in GUI
     */
    @Override
    public String toString() {
        return Translation.getTranslation(key);
    }

    public abstract FileArchiver getInstance(Folder f);
}
