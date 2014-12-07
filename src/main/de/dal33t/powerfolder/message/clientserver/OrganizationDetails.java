package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

import de.dal33t.powerfolder.security.Organization;

public class OrganizationDetails implements Serializable {

    private static final long serialVersionUID = 100L;

    private Organization organization;
    private long users;

    // Add later if necessary:
    // private long spaceUsed;

    public OrganizationDetails(Organization organization, long users) {
        super();
        this.organization = organization;
        this.users = users;
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
}
