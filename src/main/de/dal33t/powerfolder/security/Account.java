package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * A access to the system indentified by username & password.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Account extends Model implements Identity, Serializable {
    private static final Logger LOG = Logger.getLogger(Account.class);
    private static final long serialVersionUID = 100L;

    // General ****************************************************************
    private String username;
    private String password;
    private Date registerDate;
    private Collection<Permission> permissions;
    
    // Online Storage related *************************************************
    private boolean newsLetter;
    private Date validTill;
    private boolean disabledUsage;
    private boolean warnedUsage;
    private OnlineStorageSubscriptionType osType;
    

    public Account() {
        this.permissions = new CopyOnWriteArrayList<Permission>();
    }

    // Overriding *************************************************************

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

    // Specific ***************************************************************

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

    public boolean isNewsLetter() {
        return newsLetter;
    }

    public void setNewsLetter(boolean newsLetter) {
        this.newsLetter = newsLetter;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    public Date getValidTill() {
        return validTill;
    }

    public void setValidTill(Date validTill) {
        this.validTill = validTill;
    }

    public OnlineStorageSubscriptionType getOsType() {
        return osType;
    }

    public void setOsType(OnlineStorageSubscriptionType osType) {
        this.osType = osType;
    }

    public boolean isDisabledUsage() {
        return disabledUsage;
    }

    public void setDisabledUsage(boolean disabled) {
        this.disabledUsage = disabled;
    }

    public boolean isWarnedUsage() {
        return warnedUsage;
    }

    public void setWarnedUsage(boolean warnedUsage) {
        this.warnedUsage = warnedUsage;
    }

    public String toString() {
        return "Login '" + username + "', permissions " + permissions;
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
                    continue;
                }
                nFolders++;
            }
        }
        return nFolders;
    }

    /**
     * @return the days left until expire. 0 if expired -1 if never expires.
     */
    public int getDaysLeft() {
        if (getValidTill() == null) {
            return -1;
        }
        long timeValid = getValidTill().getTime() - System.currentTimeMillis();
        if (timeValid > 0) {
            int daysLeft = (int) (((double) timeValid) / (1000 * 60 * 60 * 24));
            return daysLeft;
        }
        return 0;
    }
}
