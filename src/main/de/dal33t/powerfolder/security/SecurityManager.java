package de.dal33t.powerfolder.security;

/**
 * A security manager handles the access control to a powerfolder security
 * realm.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface SecurityManager {

    // Session handling *******************************************************

    /**
     * @return the active session/account associated with the current thread.
     */
    Account getSession();

    /**
     * Clears the currently active session.
     */
    void destroySession();

    // Misc *******************************************************************

    /**
     * Saves a new or updates an old account. Afterwards the account is
     * persisted.
     * 
     * @param login the identity to save
     */
    void saveIdentity(Account login);

    /**
     * @param username
     *            the username of the login
     * @param password
     *            the password of the login
     * @return the account if acces is possible, null if user could not be logged
     *         in.
     */
    Account authenticate(String username, String password);

    // Permissions ************************************************************

}