package de.dal33t.powerfolder.disk.problem;

import java.nio.file.Path;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.notices.LocalDeleteNotice;
import de.dal33t.powerfolder.util.Translation;

public class LocalDeletionProblem extends ResolvableProblem {

    private FolderInfo folderInfo;
    private LocalDeleteNotice notice;

    public LocalDeletionProblem(FolderInfo foInfo, LocalDeleteNotice notice) {
        this.folderInfo = foInfo;
        this.notice = notice;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                int response = DialogFactory
                    .genericDialog(
                        controller,
                        Translation.getTranslation("local_delete_notice.title"),
                        Translation.getTranslation(
                            "local_delete_notice.message",
                            folderInfo.getLocalizedName()),
                        new String[]{
                            Translation
                                .getTranslation("local_delete_notice.broadcast_deletions"),
                            Translation
                                .getTranslation("local_delete_notice.discard_deletions")},
                        0, GenericDialogType.WARN);
                if (response == 0) {
                    // Broadcast deletions
                    Folder folder = folderInfo.getFolder(controller);
                    if (folder != null) {
                        folder.scanLocalFiles(true);
                        folder.removeProblem(LocalDeletionProblem.this);
                    }
                    controller.getUIController().getApplicationModel()
                        .getNoticesModel().clearNotice(notice);
                } else if (response == 1) {
                    // Discard changes. Remove all old FileInfos with
                    // deleted-flag.
                    Folder folder = folderInfo.getFolder(controller);
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
                        folder.removeProblem(LocalDeletionProblem.this);
                    }
                    controller.getUIController().getApplicationModel()
                        .getNoticesModel().clearNotice(notice);
                }
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
                folderInfo.getLocalizedName());
    }

    @Override
    public String getWikiLinkKey() {
        // TODO Auto-generated method stub
        return null;
    }

}
