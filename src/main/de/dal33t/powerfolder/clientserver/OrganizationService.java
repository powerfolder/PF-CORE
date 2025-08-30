package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.security.Organization;

public interface OrganizationService {

    /**
     *
     * @param name the organization name (customer name)
     * @return the newly created Organization with Default server params
     */

    Organization simplifiedCreate(final String name);

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
     * @param newMaxUsers new Users count
     */
    void changeMaxUsers(String organizationId, int newMaxUsers);

    /**
     * update Storage Size
     *
     * @param organizationId the organization id
     * @param newMaxStorageSize new max storage
     */
    void changeStorageSize(String organizationId, long newMaxStorageSize);
}
