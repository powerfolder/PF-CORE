package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.Organization;

public interface OrganizationService {

    /**
     *
     * @param name the organization name (customer name)
     * @return the newly created Organization with Default server params
     */

    Organization simplifiedCreate(final String name);

    /**
     *
     * @param name the organization name (company / team name)
     * @return the newly created Organization with default trial values
     */
    Organization createTrial(final String name, final Account creator);

    /**
     * cancel subscription (Disable organization)
     *
     * @param organizationId the organization id
     */
    void cancelSubscription(String organizationId);

    /**
     * renew subscription (Enable organization)
     *
     * @param organizationId the organization id
     */
    void renewSubscription(String organizationId);

    /**
     * update max user count
     *
     * @param organizationId the organization id
     * @param oldMaxUsers old max Users count
     * @param newMaxUsers new max Users count
     */
    void changeMaxUsers(String organizationId, int oldMaxUsers, int newMaxUsers);

    /**
     * update Storage Size
     *
     * @param organizationId the organization id
     * @param oldMaxStorageSize the previous max storage
     * @param newMaxStorageSize new max storage
     *
     */
    void changeSpaceAllowed(String organizationId, long oldMaxStorageSize, long newMaxStorageSize);
}
