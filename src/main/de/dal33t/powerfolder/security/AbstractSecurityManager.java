package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * The security manager for the client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class AbstractSecurityManager extends PFComponent implements
    SecurityManager
{

    public AbstractSecurityManager(Controller controller) {
        super(controller);
    }

    /**
     * Retrieves the default folder permission from security settings.
     * 
     * @param foInfo
     * @return
     */
    protected abstract FolderPermission retrieveDefaultPermission(
        FolderInfo foInfo);

    public final boolean hasFolderPermission(Member member,
        FolderPermission permission)
    {
        Reject.ifNull(member, "Member is null");
        Reject.ifNull(permission, "Permission is null");

        if (!Feature.SECURITY_CHECKS.isEnabled()) {
            return true;
        }
        
        FolderInfo foInfo = permission.getFolder();
        AccountInfo aInfo = getAccountInfo(member);
        boolean hasPermission = false;
        boolean useDefaultPermission = false;
        if (aInfo != null) {
            // Step 1) Check if this account has that permission.
            hasPermission = hasPermission(aInfo, permission);
            // Step 2) Check if this account has at least lowest access level
            if (!hasPermission) {
                boolean hasFolderPermissionSet = hasPermission(aInfo,
                    new FolderReadPermission(foInfo));
                if (hasFolderPermissionSet) {
                    // His permissions have been explicitly set. So he truely
                    // does not has the currently checked permission
                    return false;
                } else {
                    // This account has not direct FolderPermission to this
                    // server. Use default permission.
                    useDefaultPermission = true;
                }
            }
        }
        if (aInfo == null || useDefaultPermission) {
            FolderPermission defaultPermission = retrieveDefaultPermission(foInfo);
            if (defaultPermission != null) {
                hasPermission = defaultPermission.equals(permission)
                    || defaultPermission.implies(permission);
                logWarning("Using default permission(" + defaultPermission
                    + ") for " + member);;
            }
        }
        return hasPermission;
    }
}
