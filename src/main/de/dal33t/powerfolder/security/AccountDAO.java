package de.dal33t.powerfolder.security;

import java.util.Collection;

/**
 * CRUD for Accounts.
 * <P>
 * Note: Not all security manager support this type of access to the own
 * database.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface AccountDAO {

    /**
     * Closes the data storage of this DAO.
     */
    void close();

    /**
     * @param oid
     * @return the account with the given OID or null if not found.
     */
    Account findByOID(String oid);

    /**
     * @param username
     * @return the account with the given username or null if not found.
     */
    Account findByUsername(String username);

    /**
     * @return all accounts.
     */
    Collection<Account> getAccounts();

    /**
     * Stores or updates one or more accounts
     * <p>
     * Should be accessed from classes that implement
     * <code>SecurityManager</code> only (or tests).
     * 
     * @param accounts
     */
    void store(Account... accounts);

    /**
     * Deletes the given account.
     * <p>
     * Should be accessed from classes that implement
     * <code>SecurityManager</code> only (or tests).
     * 
     * @param oid
     *            the OID of the account.
     * @return false if not found, true if deleted
     */
    boolean delete(String oid);
}
