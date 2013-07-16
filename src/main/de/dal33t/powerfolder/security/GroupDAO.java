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
     * @return
     */
    Group findByGroupname(String groupname);

    /**
     * Get all Groups.
     * 
     * @return
     */
    List<Group> getGroups();

    /**
     * Get a list of Groups that apply to the passed filter.
     * 
     * @param filterModel
     * @return
     */
    List<Group> getGroups(GroupFilterModel filterModel);

    /**
     * Get a list of all groups, that hold a permission to a folder.
     * 
     * @param folderInfo
     * @return
     */
    Collection<Group> findWithFolderPermission(FolderInfo folderInfo);
}
