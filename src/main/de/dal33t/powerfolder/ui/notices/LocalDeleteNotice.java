/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: LocalDeleteNotice.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

/**
 * Notice to report a local mass deletion event. Show in notification and add to app model.
 */
public class LocalDeleteNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private FolderInfo folderInfo;

    public LocalDeleteNotice(FolderInfo folderInfo) {
        super(Translation.getTranslation("warning_notice.title"), Translation
            .getTranslation("warning_notice.mass_deletion",
                folderInfo.getLocalizedName()));
        this.folderInfo = folderInfo;
    }

    public Runnable getPayload(final Controller controller) {
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
                    }
                    controller.getUIController().getApplicationModel()
                        .getNoticesModel().clearNotice(LocalDeleteNotice.this);
                } else if (response == 1) {
                    // Discard changes. Remove all old FileInfos with
                    // deleted-flag.
                    Folder folder = folderInfo.getFolder(controller);
                    if (folder != null) {
                        // Discard all locally deleted files
                        for (FileInfo fInfo : folder.getKnownFiles()) {
                            // Discard all changes which are not in sync with dis.
                            if (!fInfo.inSyncWithDisk(fInfo.getDiskFile(controller.getFolderRepository()))) {
                                folder.getDAO().delete(null, fInfo);
                            }
                        }
                        // And re-download them
                        controller.getFolderRepository().getFileRequestor()
                            .triggerFileRequesting(folderInfo);
                    }
                    controller.getUIController().getApplicationModel()
                        .getNoticesModel().clearNotice(LocalDeleteNotice.this);
                }
            }
        };
    }

    public boolean isNotification() {
        return true;
    }

    public boolean isActionable() {
        return true;
    }

    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.WARINING;
    }

    public boolean isPersistable() {
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((folderInfo == null) ? 0 : folderInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        LocalDeleteNotice other = (LocalDeleteNotice) obj;
        if (folderInfo == null) {
            if (other.folderInfo != null)
                return false;
        } else if (!folderInfo.equals(other.folderInfo))
            return false;
        return true;
    }
}