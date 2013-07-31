package de.dal33t.powerfolder.security;

import java.util.List;

import de.dal33t.powerfolder.util.db.GenericDAO;

/**
 * PFS-779: DAO for PFS-779: Organization wide admin role to manage user
 * accounts per "admin domain"/Organization - Multitenancy - Mandantenfähigkeit
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface OrganizationDAO extends GenericDAO<Organization> {

    /**
     * Find a Organization by its name.
     * 
     * @param name
     * @return
     */
    Organization findByName(String name);

    /**
     * Get all.
     * 
     * @return
     */
    List<Organization> getAll();

}
