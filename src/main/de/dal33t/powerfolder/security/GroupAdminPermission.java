package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.util.Reject;

/**
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
 */
public class GroupAdminPermission implements Permission {

    private static final long serialVersionUID = 100L;
    private Group group;

    public GroupAdminPermission(Group group) {
        Reject.ifNull(group, "Group is null");
        this.group = group;
    }

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String getId() {
        return group.getOID() + "_GP_" + getClass().getSimpleName();
    }

}
