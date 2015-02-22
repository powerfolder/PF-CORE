package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

public class LocalDeletionProblem extends ResolvableProblem {

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
                        Translation.get("local_delete_notice.title",
                            fileInfo.getFilenameOnly()),
                        Translation
                            .get("local_delete_notice.message"),
                        new String[]{
                            Translation
                                .get("local_delete_notice.broadcast_deletions"),
                            Translation
                                .get("local_delete_notice.discard_deletions")},
                        0, GenericDialogType.WARN);
                Folder folder = folderInfo.getFolder(controller);
                if (folder == null) {
                    return;
                }
                if (response == 0) {
                    // Broadcast deletions
                    FileInfo oldFI = folder.getFile(fileInfo);
                    folder.scanChangedFile(oldFI);
                } else if (response == 1) {
                    // Discard changes. Remove all old FileInfos with
                    // deleted-flag.
                    folder.getDAO().delete(null, fileInfo);
                    // And re-download them
                    controller.getFolderRepository().getFileRequestor()
                        .triggerFileRequesting(folderInfo);
                }
                folder.removeProblem(LocalDeletionProblem.this);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation
            .get("local_delete_notice.resolution_description");
    }

    @Override
    public String getDescription() {
        return Translation
            .get("warning_notice.mass_deletion",
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
