/*
 * Copyright 2004 - 2021 Christian Sprajc. All rights reserved.
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

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.RemoteCallException;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

import java.util.logging.Logger;

public class FolderRenameTask extends ServerRemoteCallTask {
    private static final Logger LOG = Logger.getLogger(FolderRenameTask.class.getName());
    private static final long serialVersionUID = 100L;

    private FolderInfo newFolderInfo;
    private MemberInfo initiator;

    public FolderRenameTask(FolderInfo newFolderInfo, Member initiator) {
        super(30);
        Reject.ifNull(newFolderInfo, "newFolderInfo");
        Reject.ifNull(initiator, "initiator");
        this.newFolderInfo = newFolderInfo;
        this.initiator = initiator.getInfo();
    }

    @Override
    protected boolean executeRemoteCall(ServerClient client) throws Exception {
        return rename(client);
    }

    private boolean rename(ServerClient client) {
        Folder folder = newFolderInfo.getFolder(getController());
        if (folder == null) {
            LOG.warning(newFolderInfo + ": not found for rename.");
            return true;
        }
        if (getController().getMySelf().isServer()) {
            LOG.warning(folder + ": Not renaming to new folder name. Rename on server is done differently");
            return true;
        }
        if (folder.getInfo().getVersion() > newFolderInfo.getVersion()) {
            LOG.warning(folder + ": Not renaming to new folder name. Remote version lower: " + newFolderInfo + " at " + initiator);
            return true;
        }
        if (!folder.hasAdminPermission(initiator.getNode(getController(), true))) {
            LOG.warning(folder + ": Initiator " + initiator + " has no folder admin permission to rename to: " + newFolderInfo);
            return true;
        }
        LOG.info("Renaming local " + folder.getInfo() + ". Remote: " + newFolderInfo + " by " + initiator);

        String ownerDisplayname = null;
        try {
            if (!client.getAccount().hasOwnerPermission(folder.getInfo())) {
                ownerDisplayname = getController().getOSClient()
                        .getFolderService(folder.getInfo()).getOwnerDisplayname(folder.getInfo());
            }
        } catch (RemoteCallException e) {
            LOG.warning(folder + ": Unable to retrieve owner name. " + e);
        }
        return getController().getFolderRepository().renameFolder(newFolderInfo, true, ownerDisplayname);
    }
}
