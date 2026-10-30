/*
* Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
* Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
*/
package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderInfo;

/** Event fired by the FolderRepository */
public class FolderRepositoryEvent extends EventObject {
    private Folder folder;
    private FolderInfo folderInfo;
    // PFS-2227
    private Folder oldFolder;
    // PFS-5976
    private String oldLocationPath;

    /** create a FolderRepositoryEvent */
    public FolderRepositoryEvent(FolderRepository source) {
        super(source);
    }

    /**
     * create a FolderRepositoryEvent about a Folder
     *
     * @param folder
     *            the folder this event is about
     */
    public FolderRepositoryEvent(FolderRepository source, Folder folder, Folder oldFolder) {
        this(source, folder);
        this.oldFolder = oldFolder;
    }

    /**
     * PFS-5976: a folder whose mount moved its location to where its data is.
     *
     * @param oldLocationPath the location before, {@code null} if it did not move
     */
    public FolderRepositoryEvent(FolderRepository source, Folder folder, String oldLocationPath) {
        this(source, folder);
        this.oldLocationPath = oldLocationPath;
    }

    /**
     * create a FolderRepositoryEvent about a Folder
     *
     * @param folder
     *            the folder this event is about
     */
    public FolderRepositoryEvent(FolderRepository source, Folder folder) {
        this(source, folder.getInfo());
        this.folder = folder;
    }

    /**
     * create a FolderRepositoryEvent about a FolderInfo
     *
     * @param folderInfo
     *            the folder this event is about
     */
    public FolderRepositoryEvent(FolderRepository source, FolderInfo folderInfo)
    {
        super(source);
        this.folderInfo = folderInfo;

    }

    /**
     * @return Returns the folder, maybe null (then use getFolderInfo)
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * @return Returns the folderInfo, maybe null (then use getFolder)
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public Folder getOldFolder() {
        return oldFolder;
    }

    /** PFS-5976: the location a mount moved the folder away from - to store the new one; {@code null} if none. */
    public String getOldLocationPath() {
        return oldLocationPath;
    }
}
