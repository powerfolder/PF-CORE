package de.dal33t.powerfolder.security;

import java.util.ArrayList;
import java.util.Collection;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Administration permission on one folder.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderAdminPermission implements Permission {
    private static final long serialVersionUID = 100L;

    private FolderInfo folder;

    public FolderAdminPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folderinfo is null");
        folder = foInfo;
    }

    public FolderInfo getFolder() {
        return folder;
    }

    /**
     * @param account
     * @return the folderinfo this account has admin permissions on.
     */
    public static Collection<FolderInfo> filter(Account account)
    {
        Reject.ifNull(account, "Account is null");
        Collection<FolderInfo> l = new ArrayList<FolderInfo>();
        for (Permission p : account.getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                l.add(((FolderAdminPermission) p).getFolder());
            }
        }
        return l;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FolderAdminPermission other = (FolderAdminPermission) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    public String toString() {
        return "FolderAdminPermission on " + folder;
    }
}
