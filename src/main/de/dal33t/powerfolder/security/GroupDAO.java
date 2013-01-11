package de.dal33t.powerfolder.security;

import java.util.List;

import de.dal33t.powerfolder.util.db.GenericDAO;

/**
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
 */
public interface GroupDAO extends GenericDAO<Group> {

    Group findByGroupname(String groupname);

    List<Group> getGroups();
}
