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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * List of available folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderList extends Message {
    private static final long serialVersionUID = 101L;

    /** List of public folders. LEFT for backward compatibility */
    public FolderInfo[] folders = new FolderInfo[0];

    /** Secret folders, Folder IDs are encrypted with magic Id */
    public FolderInfo[] secretFolders;

    public FolderList() {
        // Serialisation constructor
    }

    /**
     * Constructor which splits up public and secret folder into own array.
     * Folder Ids of secret folders will be encrypted with magic Id sent by
     * remote node
     * 
     * @param allFolders
     * @param remoteMagicId
     *            the magic id which was sent by the remote side
     */
    public FolderList(Collection<FolderInfo> allFolders, String remoteMagicId) {
        // Split folderlist into secret and public list
        // Encrypt secret folder ids with magic id
        List<FolderInfo> secretFos = new ArrayList<FolderInfo>(allFolders
            .size());
        for (FolderInfo folderInfo : allFolders) {
            if (!StringUtils.isBlank(remoteMagicId)) {
                // Send secret folder infos if magic id is not empty
                // Clone folderinfo
                FolderInfo secretFolder = (FolderInfo) folderInfo.clone();
                // Set Id to secure Id
                secretFolder.id = secretFolder.calculateSecureId(remoteMagicId);
                // Secret folder, encrypt folder id with magic id
                secretFos.add(secretFolder);
            }
        }
        this.secretFolders = new FolderInfo[secretFos.size()];
        secretFos.toArray(secretFolders);
    }

    public String toString() {
        return "FolderList: " + secretFolders.length + " folders";
    }
}