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
     * @return the active session/login associated with the current thread.
     */
    Identity getSession();

    /**
     * Clears the currently active session.
     */
    void destroySession();

    // Misc *******************************************************************

    /**
     * Adds a new login to this security manager.
     * 
     * @param login
     */
    void addIdentity(Identity login);

    /**
     * Saves the current state of the identities to disk.
     */
    void persist();

    /**
     * @param username
     *            the username of the login
     * @param password
     *            the password of the login
     * @return the login if acces is possible, null if user could not be logged
     *         in.
     */
    Identity authenticate(String username, String password);

    // Permissions ************************************************************

}