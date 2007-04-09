package de.dal33t.powerfolder.security;

/**
 * A very general identity, may be an webinterface login for ex.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface Identity {
    /**
     * Checks on a permission.
     * 
     * @param permission
     *            the permission this identity should own.
     * @return true if this identity has the permission.
     */
    boolean hasPermission(Permission permission);

    /**
     * Grants this identity the permission.
     * 
     * @param permission
     *            the permission to grant.
     */
    void grant(Permission permission);

    /**
     * Revokes this identity the permission.
     * 
     * @param permission
     *            the permission to revoke.
     */
    void revoke(Permission permission);
}
