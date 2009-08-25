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

    private final AccountInfo account;
    private FolderInfo foInfo;

    public FolderObtainPermissionTask(FolderInfo foInfo, AccountInfo aInfo) {
        super(DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(foInfo, "FolderInfo");
        Reject.ifNull(aInfo, "AccountInfo");
        this.foInfo = foInfo;
        this.account = aInfo;
    }

    @Override
    public void executeRemoteCall(ServerClient client) throws Exception {
        if (!getController().getFolderRepository().hasJoinedFolder(foInfo)) {
            remove();
            return;
        }
        if (getController().getOSClient().getServer().isMySelf()) {
            remove();
            return;
        }
        if (!client.getAccount().isValid()) {
            return;
        }
        if (!account.equals(client.getAccount().createInfo())) {
            return;
        }
        FolderPermission fp = client.getSecurityService()
            .obtainFolderPermission(foInfo);
        LOG.warning("Obtained permission on " + foInfo + ": " + fp);
        remove();
    }
}
