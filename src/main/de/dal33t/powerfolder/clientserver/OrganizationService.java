package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.model.OrganizationListRequest;
import de.dal33t.powerfolder.model.OrganizationRestrictedUpdateRequest;
import de.dal33t.powerfolder.security.Organization;
import de.dal33t.powerfolder.model.OrganizationRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface OrganizationService {

    /**
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
     * @param newMaxUsers    new Users count
     */
    void changeMaxUsers(String organizationId, int newMaxUsers);

    /**
     * update Storage Size
     *
     * @param organizationId    the organization id
     * @param newMaxStorageSize new max storage
     */
    void changeStorageSize(String organizationId, long newMaxStorageSize);

    Organization create(OrganizationRequest request);

    Organization update(String organizationId, OrganizationRequest request);

    Organization update(String organizationId, OrganizationRestrictedUpdateRequest organizationRestrictedUpdateRequest);

    void deleteOrganization(String organizationID);

    Path generateOrganizationsReportCsv() throws IOException;

    List<Organization> getAll(OrganizationListRequest svcReq);

    List<Organization> getAll(OrganizationFilterModel filterModel);

    /**
     * Calculate the total size used by the {@link Organization}.
     *
     */
    long countSpaceAssigned(String orgOID);

    long countAccounts(String oid);

    boolean existsOtherOrganizationWithDomain(Organization organization, String domain);

    Organization findByID(String oid);

    Organization findByName(String name);

    boolean existsWithDomainFromEmail(String usernameCandidate);

    Organization findByLDAPDN(String ldapDN);

    void updateLdapDN(String oid, String ldapDN);

    void addNotesWithDate(String oid, String note);
}