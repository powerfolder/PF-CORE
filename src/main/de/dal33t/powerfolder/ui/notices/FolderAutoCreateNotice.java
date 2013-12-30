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
 * $Id: FolderAutoCreateNotice.java 12401 2011-07-29 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * Notice that a folder was autocreated.
 */
public class FolderAutoCreateNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private final FolderInfo folderInfo;

    public FolderAutoCreateNotice(FolderInfo folderInfo) {

        super(Translation.getTranslation("folder_auto_create_notice.title"),
                Translation.getTranslation("folder_auto_create_notice.text",
                        folderInfo.getName()));

        this.folderInfo = folderInfo;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.INFORMATION;
    }

    public Object getPayload(Controller controller) {
        return null;
    }

    public boolean isActionable() {
        return true;
    }

    public boolean isNotification() {
        return true;
    }

    public boolean isPersistable() {
        return true;
    }
}
