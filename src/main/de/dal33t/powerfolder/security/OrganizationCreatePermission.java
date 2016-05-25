package de.dal33t.powerfolder.security;

/**
 * Permission to allow a user to create organizations using the Organizations
 * App.<br />
 * <br />
 * This user is allowed to create {@link Organization Organizations} and
 * {@link Account Users} of those Organizations, and grant
 * {@link OrganizationAdminPermission} to users of the organization he/she is
 * member of.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class OrganizationCreatePermission extends SingletonPermission {

    private static final long serialVersionUID = 100L;
    public static final OrganizationCreatePermission INSTANCE = new OrganizationCreatePermission();

    private OrganizationCreatePermission() {
    }

    @Override
    public boolean implies(Permission impliedPermision) {
        return false;
    }
}
