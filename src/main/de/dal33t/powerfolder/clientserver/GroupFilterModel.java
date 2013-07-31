package de.dal33t.powerfolder.clientserver;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.security.Organization;

public class GroupFilterModel extends Model {
    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_GROUPNAME = "groupname";

    private String groupname;
    private String organizationID = Organization.FILTER_MATCH_ALL;

    private int maxResults;

    // Getter and Setter

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public String getGroupname() {
        return groupname;
    }

    public void setGroupname(String groupname) {
        Object oldValue = getGroupname();
        this.groupname = groupname != null
            ? groupname.toLowerCase().trim()
            : null;
        firePropertyChange(PROPERTY_GROUPNAME, oldValue, this.groupname);
    }

    public String getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(String organizationID) {
        this.organizationID = organizationID;
    }

    public boolean isAnyOrganization() {
        return Organization.FILTER_MATCH_ALL.equals(organizationID);
    }

    // Logic

    public void reset() {
        groupname = null;
        maxResults = 0;
        organizationID = Organization.FILTER_MATCH_ALL;
    }
}
