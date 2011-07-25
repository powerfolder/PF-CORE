/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: FolderBean.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.Folder;

public class FolderBean {

    public enum Type { Local, Typical, CloudOnly }  

    private final Type type;
    private final FolderInfo folderInfo;
    /** Typical and Online FolderBeans do not have folders. */
    private final Folder folder;
    private boolean online;

    public FolderBean(Type type, FolderInfo folderInfo, Folder folder,
                       boolean online) {
        this.type = type;
        this.folderInfo = folderInfo;
        this.folder = folder;
        this.online = online;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public Folder getFolder() {
        return folder;
    }

    public Type getType() {
        return type;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FolderBean that = (FolderBean) obj;
        return folderInfo.equals(that.folderInfo);
    }

    public int hashCode() {
        return folderInfo.hashCode();
    }

}
