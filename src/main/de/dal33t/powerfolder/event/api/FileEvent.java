/*
 * Copyright 2004 - 2017 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.event.api;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Format;

import java.nio.file.Path;

/**
 * PFS-1766: Generic event regarding files and directories.
 *
 * @author Christian Sprajc
 */
public abstract class FileEvent extends WebHook {

    protected FileEvent(Controller controller, ConfigurationEntry urlEntry) {
        super(controller, urlEntry);
    }

    public FileEvent of(FileInfo fileInfo) {
        try {

        addParameter("folderID", fileInfo.getFolderInfo().getId());
        addParameter("folderName", fileInfo.getFolderInfo().getLocalizedName());

        if (fileInfo.isDiretory()) {
            addParameter("type", "dir");
        } else {
            addParameter("type", "file");
        }

        if (!fileInfo.isLookupInstance()) {
            addParameter("size", fileInfo.getSize());
        }

        addParameter("relativeName", fileInfo.getRelativeName());
        addParameter("fileName", fileInfo.getFilenameOnly());

        addParameter("version", fileInfo.getVersion());
        addParameter("deleted", fileInfo.isDeleted());

        Path diskFile = fileInfo.getDiskFile(getController().getFolderRepository());
        if (diskFile != null) {
            addParameter("path", diskFile.toString());
        }

        if (fileInfo.getModifiedDate() != null) {
            addParameter("modifiedDateMS", fileInfo.getModifiedDate().getTime());
            addParameter("modifiedDate", Format.formatDateCanonical(fileInfo.getModifiedDate()));
            addParameter("modifiedTime", Format.formatTimeShort(fileInfo.getModifiedDate()));
        }

        MemberInfo deviceInfo = fileInfo.getModifiedBy();
        if (deviceInfo != null) {
            addParameter("modifiedByDeviceID",deviceInfo.getId());
            addParameter("modifiedByDeviceName",deviceInfo.getNick());
            addParameter("modifiedByDeviceConnectAddress",deviceInfo.getConnectAddress());
        }

        AccountInfo accountInfo = fileInfo.getModifiedByAccount();
        if (accountInfo != null) {
            addParameter("modifiedByAccountID",accountInfo.getOID());
            addParameter("modifiedByAccountUsername",accountInfo.getUsername());
            addParameter("modifiedByAccountDisplayName",accountInfo.getDisplayName());
        }


        // PFS-1461: Start
        if (fileInfo.isLocked(getController())) {
            Lock lock = fileInfo.getLock(getController());
            if (lock != null) {
                String webSyncStatus = "locked";
                if (lock.getMemberInfo().equals(getMySelf().getInfo())) {
                    webSyncStatus = "edit";
                }
                for (Member node : getController().getNodeManager()
                        .getNodesAsCollection())
                {
                    if (node.isServer()
                            && node.getInfo().equals(lock.getMemberInfo()))
                    {
                        webSyncStatus = "edit";
                    }
                }
                addParameter("syncStatus",webSyncStatus);
            }
        } else if (SyncStatus.of(getController(), fileInfo) == SyncStatus.SYNCING) {
            addParameter("syncStatus","syncing");
        }

        } catch (RuntimeException e) {
            logWarning("Failed to send all parameters: " + e, e);
        }

        return this;
    }
}
