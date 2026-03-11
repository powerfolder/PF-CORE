/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 *
 * $Id$
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.security.Organization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.dal33t.powerfolder.util.StringUtils.isNotBlank;

public class AccountFilterModel implements Serializable {
    private static final long serialVersionUID = 100L;

    private boolean disabledOnly;
    private boolean proUsersOnly;
    private boolean activeTrial;
    private Boolean activatedOnly;
    private String username;
    private String queryname;
    private String memberOfOrganizationOID = Organization.FILTER_MATCH_ALL;
    private List<String> adminOfOrganizationOID;
    private boolean reseller;
    private String[] permissionNames;
    private String sortingProperty;
    private boolean sortingAscending = true;

    private int pageNumber;
    private int maxResults;

    // Getter and Setter ******************************************************

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public boolean isDisabledOnly() {
        return disabledOnly;
    }

    public void setDisabledOnly(boolean disabledOnly) {
        Object oldValue = isDisabledOnly();
        this.disabledOnly = disabledOnly;
    }

    public boolean isProUsersOnly() {
        return proUsersOnly;
    }

    public void setProUsersOnly(boolean proUsersOnly) {
        Object oldValue = isProUsersOnly();
        this.proUsersOnly = proUsersOnly;
    }

    public boolean isActiveTrial() {
        return activeTrial;
    }

    public void setActiveTrial(boolean activeTrial) {
        Object oldValue = isActiveTrial();
        this.activeTrial = activeTrial;
    }

    public void setActivatedOnly(Boolean activatedOnly) {
        this.activatedOnly = activatedOnly;
    }

    public Boolean getActivatedOnly() {
        return activatedOnly;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Object oldValue = getUsername();
        this.username = username != null ? username.toLowerCase().trim() : null;
    }

    public String getQueryname() {
        return queryname;
    }

    public void setQueryname(String queryname) {
        this.queryname = queryname;
    }

    public void applyFromQuery(String query) {
        if (isNotBlank(query)) {
            Pattern p = Pattern.compile("\\bsort(?:down|up):([a-zA-Z0-9_]+)\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(query);

            if (m.find()) {
                String sortField = m.group(1); // e.g. "username"
                boolean ascending = m.group(0).toLowerCase().contains("sortup");

                setSortingProperty(sortField);
                setSortingAscending(ascending);

                // Remove the sorting directive from the query and reset it
                String cleanedQuery = query.replace(m.group(0), "").trim();
                setQueryname(cleanedQuery);
                return;
            }
        }
        // If nothing matched, just keep the original query
        setQueryname(query);
    }

    public String getMemberOfOrganizationOID() {
        return memberOfOrganizationOID;
    }

    public void setMemberOfOrganizationOID(String organizationOID) {
        this.memberOfOrganizationOID = organizationOID;
    }

    public boolean isMemberOfAnyOrganization() {
        return Organization.FILTER_MATCH_ALL.equals(memberOfOrganizationOID);
    }

    public List<String> getAdminOfOrganizationOIDs() {
        return adminOfOrganizationOID;
    }

    public void setAdminOfOrganizationOIDs(List<String> orgOIDs) {
        adminOfOrganizationOID = orgOIDs;
    }

    public void addAdminOfOrganizationOIDs(String orgOID) {
        if (orgOID == null) {
            return;
        }
        if (adminOfOrganizationOID == null) {
            adminOfOrganizationOID = new ArrayList<>();
        }
        adminOfOrganizationOID.add(orgOID);
    }

    public boolean isReseller() {
        return reseller;
    }

    public void setReseller(boolean reseller) {
        this.reseller = reseller;
    }

    public String getSortingProperty() {
        return sortingProperty;
    }

    public void setSortingProperty(String sortingProperty) {
        this.sortingProperty = sortingProperty;
    }

    public boolean isSortingAscending() {
        return sortingAscending;
    }

    public void setSortingAscending(boolean sortingAscending) {
        this.sortingAscending = sortingAscending;
    }

    public void setPageNumber(int pageNumber) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Calculates the offset for database queries.
     */
    public int getOffset() {
        if (pageNumber <= 1 || maxResults <= 0) {
            return 0;
        }
        return (pageNumber - 1) * maxResults;
    }

    public String[] getFilterByPermission() {
        return permissionNames;
    }

    public void setFilterByPermission(String[] permissionNames) {
        this.permissionNames = permissionNames;
    }

    // Logic ******************************************************************

    public void reset() {
        activeTrial = false;
        disabledOnly = false;
        proUsersOnly = false;
        username = null;
        adminOfOrganizationOID = null;
        memberOfOrganizationOID = Organization.FILTER_MATCH_ALL;
        maxResults = 0;
    }
}
