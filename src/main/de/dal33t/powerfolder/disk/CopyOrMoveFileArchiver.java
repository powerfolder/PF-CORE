package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;

public class CopyOrMoveFileArchiver implements FileArchiver {
    private static final Logger log = Logger
        .getLogger(CopyOrMoveFileArchiver.class.getName());
    private final Folder folder;

    public CopyOrMoveFileArchiver(Folder folder) {
        Reject.notNull(folder, "folder");
        this.folder = folder;
    }

    public void archive(FileInfo fileInfo, File source, boolean forceKeepSource)
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(source, "source");

        File target = getArchiveTarget(fileInfo);

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

    private File getArchiveTarget(FileInfo fileInfo) {
        return null;
    }

}
