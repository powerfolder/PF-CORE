package de.dal33t.powerfolder.security;

import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.clientserver.GroupFilterModel;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.db.GenericDAO;

/**
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
 */
public interface GroupDAO extends GenericDAO<Group> {

    /**
     * Find a group by its name.
     * 
     * @param groupname
     *            The group's name
     * @return the group referenced by {@code name}, or {@code null} if it was
     *         not found.
     */
    Group findByGroupname(String groupname);

    /**
     * Get all Groups.
     * 
     * @return a list of all groups.
     */
    List<Group> getGroups();

    /**
     * Get a list of Groups that apply to the passed filter.
     * 
     * @param filterModel
     *            The filters to apply
     * @return a list of all groups that fit the filter.
     */
    List<Group> getGroups(GroupFilterModel filterModel);

    /**
     * Get a list of all groups, that hold a permission to a folder.
     * 
     * @param folderInfo
     *            The folder
     * @return a list of all groups that have permission to {@code folderInfo}.
     */
    Collection<Group> findWithFolderPermission(FolderInfo folderInfo);

    /**
     * Store several groups.
     * 
     * @param groups
     *            The groups to store
     */
    void store(Group... groups);

    /**
     * Get the number of groups, that belong to an organization.
     * 
     * @param org
     *            The organization
     * @return the number of groups associated with {@code org}.
     */
    int countGroupsWithOrganization(Organization org);

    /**
     * Find a group with a certain LDAP distinguished name. PFS-420
     * 
     * @param ldapDN
     *            The destinguished name
     * @return the group that is referenced by the {@code ldapDN}, or
     *         {@code null} if no group was found.
     */
    Group findByLdapDN(String ldapDN);

    /**
     * Get all groups, that have an LDAP distinguished name set. PFS-420
     * 
     * @return a list of all groups that were imported from an LDAP/AD server.
     */
    List<Group> getLdapGroups();
}
