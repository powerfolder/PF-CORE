package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;
import java.util.Collection;

import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.Organization;

public class OrganizationDetails implements Serializable {

    private static final long serialVersionUID = 100L;

    private Organization organization;
    private long users;
    private long assignedSpace;
    private Collection<Account> resellers;
    private boolean resellerOrg;

    public OrganizationDetails(Organization organization, long users, long assignedSpace, boolean resellerOrg, Collection<Account> resellers) {
        super();
        this.organization = organization;
        this.users = users;
        this.assignedSpace = assignedSpace;
        this.resellerOrg = resellerOrg;
        this.resellers = resellers;
    }

    public Organization getOrganization() {
        return organization;
    }

    public long getUsers() {
        return users;
    }

    public long getAssignedSpace() {
        return assignedSpace;
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
