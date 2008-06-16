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
package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.action.BaseAction;

/**
 * Sync the folder membership with the rights on the server
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class SyncFolderRightsAction extends BaseAction {
    private ServerClient client;

    protected SyncFolderRightsAction(ServerClient client) {
        super("sync_folder_rights", client.getController());
        this.client = client;
    }

    public void actionPerformed(ActionEvent e) {
        client.syncFolderRights();
    }

}
