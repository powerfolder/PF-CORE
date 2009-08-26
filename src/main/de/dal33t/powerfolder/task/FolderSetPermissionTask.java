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
public class FolderSetPermissionTask extends ServerRemoteCallTask {
    private static final long serialVersionUID = 100L;
    private static final Logger LOG = Logger
        .getLogger(FolderSetPermissionTask.class.getName());

    private final FolderPermission permission;
    private FolderInfo foInfo;

    public FolderSetPermissionTask(AccountInfo aInfo, FolderInfo foInfo,
        FolderPermission permission)
    {
        super(aInfo, DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
        this.permission = permission;
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
        client.getSecurityService().setFolderPermission(getIssuer(), foInfo,
            permission);
        LOG.warning("Set permission on " + foInfo + " for " + getIssuer()
            + " to " + permission);
        remove();
    }
}
