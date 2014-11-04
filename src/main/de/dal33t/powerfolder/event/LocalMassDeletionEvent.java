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
 * $Id: LocalMassDeletionEvent.java 8169 2009-06-10 11:57:40Z harry $
 */
package de.dal33t.powerfolder.event;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;

public class LocalMassDeletionEvent {

    private final Folder folder;
    private final FileInfo fInfo;

    public LocalMassDeletionEvent(Folder folder, FileInfo fInfo) {
        Reject.ifNull(fInfo, "Folder is null");
        this.folder = folder;
        this.fInfo = fInfo;
    }

    public FileInfo getFile() {
        return fInfo;
    }

    public Folder getFolder() {
        return folder;
    }
}
