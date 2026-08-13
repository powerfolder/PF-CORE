/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.security;

import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.clientserver.GroupFilterModel;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.db.GenericDAO;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public interface GroupDAO extends GenericDAO<Group> {

     /**
     * Finds all accounts by their organization ID.
     *
     * @param orgID
     */
    Collection<Group> findByOrgID(String orgID);

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
     * Nested Groups: direct subgroups of {@code parent}. Looks up rows in
     * {@code AGroup_Parents} where {@code parent_oid = parent.oid}.
     *
     * @param parent the parent group
     * @return the direct subgroups (empty if none)
     */
    Collection<Group> findChildrenOf(Group parent);

    /**
     * Nested Groups: direct subgroups of {@code parent}, limited to at most
     * {@code maxResults} rows (or no limit for a negative value). Used to build
     * a small subgroup preview without loading every child.
     *
     * @param parent the parent group
     * @param maxResults the maximum number of subgroups to return
     * @return up to {@code maxResults} direct subgroups (empty if none)
     */
    Collection<Group> findChildrenOf(Group parent, int maxResults);

    /**
     * Nested Groups: count of direct subgroups of {@code parent}. Cheap
     * alternative to {@link #findChildrenOf(Group)} when only the size is
     * needed (e.g. the {@code nChildGroups} field on {@code getAll}).
     */
    int countChildrenOf(Group parent);
    
    /**
     * PFS-1949
     * 
     * @param folderInfo
     *            The folder
     * @return the number of groups that have permission to {@code folderInfo}.
     */
    int countWithFolderPermission(FolderInfo folderInfo);

    /**
     * Return a list of groups, where the account specified by
     * {@code accountOID} is admin of. Take a look at the permissions of the
     * account. PFS-504
     * 
     * @param accountOID
     *            The OID of the admin.
     * @return A list of all Groups, the account is admin of.
     */
    Collection<Group> findWithGroupAdmin(String accountOID);

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
     * @return The number of groups that were imported via LDAP/AD.
     */
    int countWithLdapDN();

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
     * @param ldapDNSuffix
     * @return A list of all groups where the LDAP DN end with {@code ldapDNSuffix}.
     */
    List<Group> findByLdapDNSuffix(String ldapDNSuffix);

    /**
     * Get all groups, that have an LDAP distinguished name set. PFS-420
     * 
     * @param groupSearchBase
     *            The search context for groups
     * @return a list of all groups that were imported from an LDAP/AD server.
     */
    List<Group> getLdapGroups(String groupSearchBase);

    /**
     * Find all the groups matching name.
     *
     * @param name The group's name
     * @return the list of  groups  referenced by {@code name}, or {@code null} if it was
     *         not found.
     */

    Collection<Group> findByName(String name);

    /**
     * Deletes every group whose OID starts with {@code oidPrefix} with a few set-based statements
     * instead of one transaction per group: memberships, group-admin permissions on accounts,
     * parent/child links, the groups' own permissions and the groups themselves.
     * <p>
     * Intended for mass cleanups of generated groups that share a prefix (e.g. the CMIS migration
     * deletes its ~49k {@code M_} groups); deleting them one by one takes hours. Every group with
     * that prefix goes, so only use it when that is exactly what is wanted.
     *
     * @param oidPrefix OID prefix of the groups to delete
     * @return number of deleted groups
     */
    int deleteByOidPrefix(String oidPrefix);
}
