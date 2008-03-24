package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * A access to the system indentified by username & password.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Account extends Model implements Serializable {
    private static final Logger LOG = Logger.getLogger(Account.class);
    private static final long serialVersionUID = 100L;

    private String username;
    private String password;
    private Date registerDate;
    private boolean newsLetter;
    private boolean proUser;

    private Collection<Permission> permissions;
    private OnlineStorageSubscription osSubscription;

    public Account() {
        this.permissions = new CopyOnWriteArrayList<Permission>();
        this.osSubscription = new OnlineStorageSubscription();
    }

    // Basic permission stuff *************************************************

    public void grant(Permission permission) {
        Reject.ifNull(permission, "Permission is null");
        LOG.debug("Granted permission to " + this + ": " + permission);
        permissions.add(permission);
    }

    public void revoke(Permission permission) {
        Reject.ifNull(permission, "Permission is null");
        LOG.debug("Revoked permission from " + this + ": " + permission);
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        Reject.ifNull(permission, "Permission is null");

        // TODO Solve this a better way.
        if (permissions.contains(Permissions.ADMIN_PERMISSIONS)) {
            // Admin has access to evetrything
            return true;
        }
        return permissions.contains(permission);
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    // Accessing / API ********************************************************

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    public boolean isNewsLetter() {
        return newsLetter;
    }

    public void setNewsLetter(boolean newsLetter) {
        this.newsLetter = newsLetter;
    }

    public OnlineStorageSubscription getOSSubscription() {
        return osSubscription;
    }

    public boolean isProUser() {
        return proUser;
    }

    public void setProUser(boolean proUser) {
        this.proUser = proUser;
    }

    public String toString() {
        return "Account '" + username + "', pro: " + proUser + ", "
            + permissions.size() + " permissions";
    }

    // Convinience ************************************************************

    /**
     * @param controller
     * @return the total size used by this user
     */
    public long calulateTotalFoldersSize(Controller controller) {
        long totalSize = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    continue;
                }
                totalSize += f.getStatistic().getSize(controller.getMySelf());
            }
        }
        return totalSize;
    }

    /**
     * @param controller
     * @return the total # of folders by this user
     */
    public int countNumberOfFolders(Controller controller) {
        int nFolders = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    LOG.warn("Got unjoined folder: " + fp.getFolder());
                    continue;
                }
                nFolders++;
            }
        }
        return nFolders;
    }

    // Permission convinience ************************************************

    /**
     * Answers if the user is allowed to read the folder contents.
     * 
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to read the folder contents
     */
    public boolean hasReadPermissions(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(new FolderAdminPermission(foInfo))
            || hasPermission(new FolderReadPermission(foInfo));
    }

    /**
     * Answers if the user is allowed to write into the folder.
     * 
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to write into the folder.
     */
    public boolean hasWritePermissions(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(new FolderAdminPermission(foInfo))
            || hasPermission(new FolderWritePermission(foInfo));
    }
}
