package de.dal33t.powerfolder.clientserver;

import com.jgoodies.binding.beans.Model;

public class OrganizationFilterModel extends Model {

    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_NAME = "name";

    private String name;
    private int maxResults;

    // Getter and Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Object oldValue = getName();
        this.name = name;
        firePropertyChange(PROPERTY_NAME, oldValue, this.name);
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
