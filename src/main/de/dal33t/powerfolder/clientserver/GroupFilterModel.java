package de.dal33t.powerfolder.clientserver;

import com.jgoodies.binding.beans.Model;

public class GroupFilterModel extends Model {
    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_GROUPNAME = "groupname";

    private String groupname;

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
        this.groupname = groupname != null ? groupname.toLowerCase().trim() : null;
        firePropertyChange(PROPERTY_GROUPNAME, oldValue, this.groupname);
    }

    // Logic

    public void reset() {
        groupname = null;
        maxResults = 0;
    }
}
