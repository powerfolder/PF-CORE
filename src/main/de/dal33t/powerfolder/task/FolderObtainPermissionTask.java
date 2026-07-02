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
 */
package de.dal33t.powerfolder.task;

import java.util.logging.Logger;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.util.Reject;

/**
 * @author sprajc
 */
public class FolderObtainPermissionTask extends ServerRemoteCallTask {
    private static final long serialVersionUID = 100L;
    private static final Logger LOG = Logger
        .getLogger(FolderObtainPermissionTask.class.getName());

    private FolderInfo foInfo;

    public FolderObtainPermissionTask(AccountInfo aInfo, FolderInfo foInfo) {
        super(aInfo, DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
    }

    @Override
    public boolean executeRemoteCall(ServerClient client) throws Exception {
        if (!getController().getFolderRepository().hasJoinedFolder(foInfo)) {
            remove();
            return true;
        }
        if (getController().getOSClient().getServer().isMySelf()) {
            remove();
            return true;
        }
        FolderPermission fp = client.getSecurityService()
            .obtainFolderPermission(foInfo);
        LOG.fine("Obtained permission on " + foInfo + ": " + fp);
        remove();
        return true;
    }
}
