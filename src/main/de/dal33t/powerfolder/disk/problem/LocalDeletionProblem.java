package de.dal33t.powerfolder.disk.problem;

import java.nio.file.Path;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

public class LocalDeletionProblem extends ResolvableProblem {

    private static final Logger log = Logger
        .getLogger(LocalDeletionProblem.class.getName());

    private final FolderInfo folderInfo;
    private final FileInfo fileInfo;

    public LocalDeletionProblem(FolderInfo foInfo, FileInfo fileInfo) {
        this.folderInfo = foInfo;
        this.fileInfo = fileInfo;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            @Override
            public void run() {
                int response = DialogFactory
                    .genericDialog(
                        controller,
                        Translation.getTranslation("local_delete_notice.title", fileInfo.getFilenameOnly()),
                        Translation.getTranslation(
                            "local_delete_notice.message"),
                        new String[]{
                            Translation
                                .getTranslation("local_delete_notice.broadcast_deletions"),
                            Translation
                                .getTranslation("local_delete_notice.discard_deletions")},
                        0, GenericDialogType.WARN);
                Folder folder = folderInfo.getFolder(controller);
                if (response == 0) {
                    // Broadcast deletions
                    if (folder != null) {
                        FileInfo oldFI = folder.getFile(fileInfo);
                        folder.scanChangedFile(oldFI);
                    } else {
                        log.info("Folder for " + folderInfo + " was null.");
                    }
                } else if (response == 1) {
                    // Discard changes. Remove all old FileInfos with
                    // deleted-flag.
                    if (folder != null) {
                        // Discard all locally deleted files
                        for (FileInfo fInfo : folder.getKnownFiles()) {
                            // Discard all changes which are not in sync with
                            // disk.
                            Path diskFile = fInfo.getDiskFile(controller.getFolderRepository());
                            boolean notInSync = !fInfo.inSyncWithDisk(diskFile);
                            if (notInSync) {
                                folder.getDAO().delete(null, fInfo);
                            }
                        }
                        // And re-download them
                        controller.getFolderRepository().getFileRequestor()
                            .triggerFileRequesting(folderInfo);
                    } else {
                        log.info("Folder for " + folderInfo + " was null.");
                    }
                }
                folder.removeProblem(LocalDeletionProblem.this);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation
            .getTranslation("local_delete_notice.resolution_description");
    }

    @Override
    public String getDescription() {
        return Translation
            .getTranslation("warning_notice.mass_deletion",
                fileInfo.getFilenameOnly());
    }

    @Override
    public String getWikiLinkKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }

        LocalDeletionProblem ldp = (LocalDeletionProblem) obj;

        return fileInfo.equals(ldp.fileInfo);
    }

    @Override
    public int hashCode() {
        return fileInfo.hashCode();
    }
}
