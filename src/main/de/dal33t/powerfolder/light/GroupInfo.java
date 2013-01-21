package de.dal33t.powerfolder.light;

import java.io.Serializable;

import org.hibernate.annotations.Index;

import de.dal33t.powerfolder.security.Group;

/**
 * Leightweight reference/info object to an {@link Group}
 * 
 * @author sprajc
 */
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 100L;

    private String oid;

    @Index(name = "IDX_GROUP_NAME")
    private String displayName;

    public GroupInfo(String oid, String displayName) {
        this.oid = oid;
        this.displayName = displayName;
    }
    
    public String getOID() {
        return oid;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }
}
