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
 * $Id: SendMessageTask.java 9008 2009-08-13 12:56:12Z harry $
 */
package de.dal33t.powerfolder.task;

import java.util.logging.Logger;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Task to create a folder on the server.
 *
 * @author sprajc
 */
public class CreateFolderOnServerTask extends ServerRemoteCallTask {
    private static final Logger LOG = Logger
        .getLogger(CreateFolderOnServerTask.class.getName());

    private static final long serialVersionUID = 100L;
    private FolderInfo foInfo;
    private SyncProfile syncProfile;
    private Integer archiveVersions;

    public CreateFolderOnServerTask(AccountInfo issuer, FolderInfo foInfo,
        SyncProfile syncProfile)
    {
        super(issuer, DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
        this.syncProfile = syncProfile;
    }

    /**
     * No issuer. Creates folder as soon as possible as any user.
     *
     * @param foInfo
     * @param syncProfile
     *            the syncprofile to use or null to take servers default.
     */
    public CreateFolderOnServerTask(FolderInfo foInfo, SyncProfile syncProfile)
    {
        super(DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
        this.syncProfile = syncProfile;
    }

    public Integer getArchiveVersions() {
        return archiveVersions;
    }

    public void setArchiveVersions(Integer archiveVersions) {
        this.archiveVersions = archiveVersions;
    }

    @Override
    public void executeRemoteCall(ServerClient client) throws Exception {
        if (!getController().getFolderRepository().hasJoinedFolder(foInfo)) {
            LOG.warning("Not longer locally synced. "
                + "Not setting up cloud backup for: " + foInfo);
            // Remove task
            remove();
        }
        if (client.isLoggedIn()) {
            // Only do this with security context.
            LOG.info("Setting folder up for cloud backup: " + foInfo);
            client.getFolderService().createFolder(foInfo, syncProfile);

            if (archiveVersions != null) {
                client.getFolderService().setArchiveMode(foInfo, archiveVersions);
            }

            // Remove task
            remove();
        }
    }

}
