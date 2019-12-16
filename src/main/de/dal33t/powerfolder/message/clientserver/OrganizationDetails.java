package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;
import java.util.Collection;

import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.Organization;

public class OrganizationDetails implements Serializable {

    private static final long serialVersionUID = 100L;

    private Organization organization;
    private long users;
    private Collection<Account> resellers;
    private boolean resellerOrg;

    // Add later if necessary:
    // private long spaceUsed;

    public OrganizationDetails(Organization organization, long users, boolean resellerOrg, Collection<Account> resellers) {
        super();
        this.organization = organization;
        this.users = users;
        this.resellerOrg = resellerOrg;
        this.resellers = resellers;
    }

    public Organization getOrganization() {
        return organization;
    }

    public long getUsers() {
        return users;
    }

    public boolean isNoOrganization() {
        return organization == null;
    }

    public boolean isResellerOrg() {
        return resellerOrg;
    }

    public Collection<Account> getResellers() {
        return resellers;
    }

    public String getResellersAsString() {
        if (resellers == null || resellers.isEmpty()) {
            return "";
        }
        String accs = "";
        for (Account a: resellers) {
            accs += a.getUsername();
            accs += " ";
        }
        return accs.trim();
    }
}
