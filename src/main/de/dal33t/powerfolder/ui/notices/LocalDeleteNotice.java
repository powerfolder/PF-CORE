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
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

/**
 * Notice to report a local mass deletion event. Show in notification and add to app model.
 */
public class LocalDeleteNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private FolderInfo folderInfo;

    public LocalDeleteNotice(String title, String summary, FolderInfo folderInfo) {
        super(title, summary);
        this.folderInfo = folderInfo;
    }

    public Runnable getPayload(final Controller controller) {
        return new Runnable() {
            public void run() {
                int response = DialogFactory.genericDialog(
                        controller, Translation.getTranslation(
                                "local_delete_notice.title"),
                        Translation.getTranslation(
                                "local_delete_notice.message", folderInfo.name),
                        new String[]{
                                Translation.getTranslation(
                                        "local_delete_notice.broadcast_deletions"),
                                Translation.getTranslation(
                                        "local_delete_notice.remove_folder_locally"),
                                Translation.getTranslation("general.close")},
                        0, GenericDialogType.WARN);
                if (response == 0) {
                    // Broadcast deletions
                    FolderRepository folderRepository = controller
                            .getFolderRepository();
                    Folder folder = folderRepository.getFolder(folderInfo);
                    folder.scanLocalFiles(true);
                } else if (response == 1) {
                    // Remove folder locally
                    FolderRepository folderRepository = controller
                        .getFolderRepository();
                    Folder folder = folderRepository.getFolder(folderInfo);
                    if (folder != null) {
                        folderRepository.removeFolder(folder, false);
                    }
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
}