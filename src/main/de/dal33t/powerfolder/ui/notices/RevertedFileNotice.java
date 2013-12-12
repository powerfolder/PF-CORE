/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class RevertedFileNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private FileInfo fileInfo;

    public RevertedFileNotice(String title, String summary, FileInfo file) {
        super(title, summary);
        fileInfo = file;
    }

    @Override
    public Runnable getPayload(final Controller controller) {
        return new Runnable() {
            public void run() {
                DialogFactory.genericDialog(controller, Translation
                    .getTranslation("reverted_file_notice.title"), Translation
                    .getTranslation("reverted_file_notice.message", fileInfo
                        .getFolderInfo().getLocalizedName(), fileInfo
                        .getRelativeName()), new String[]{Translation
                    .getTranslation("general.ok")}, 0, GenericDialogType.INFO);
            }
        };
    }

    @Override
    public boolean isNotification() {
        return true;
    }

    @Override
    public boolean isActionable() {
        return true;
    }

    @Override
    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.INFORMATION;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }
}
