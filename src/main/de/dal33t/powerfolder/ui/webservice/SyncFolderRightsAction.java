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
