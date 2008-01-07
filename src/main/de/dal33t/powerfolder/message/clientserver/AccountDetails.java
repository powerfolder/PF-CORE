package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Format;

/**
 * Capsulates a identity and adding additional information.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AccountDetails implements Serializable {
    private static final long serialVersionUID = 100L;

    private Account user;
    private long spaceUsed;
    private int nFolders;

    public AccountDetails(Account user, long spaceUsed, int nFolders) {
        super();
        this.user = user;
        this.spaceUsed = spaceUsed;
        this.nFolders = nFolders;
    }

    public Account getAccount() {
        return user;
    }

    public long getSpaceUsed() {
        return spaceUsed;
    }

    public int getNFolders() {
        return nFolders;
    }

    public String toString() {
        return "AccountDetails, " + user + ". " + nFolders + " folders, "
            + Format.formatBytesShort(spaceUsed);
    }
}