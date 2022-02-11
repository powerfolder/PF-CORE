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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.security.Organization;

public class AccountFilterModel implements Serializable {
    private static final long serialVersionUID = 100L;

    private boolean disabledOnly;
    private boolean proUsersOnly;
    private boolean activeTrial;
    private String username;
    private String queryname;
    private String memberOfOrganizationOID = Organization.FILTER_MATCH_ALL;
    private List<String> adminOfOrganizationOID;
    private boolean reseller;
    private String[] permissionNames;
    private String sortingProperty;
    private String sortingOrder;
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
        Object oldValue = getQueryname();
        this.queryname = queryname != null
            ? queryname.toLowerCase().trim()
            : null;
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

    public String getSortingOrder() {
        return sortingOrder;
    }

    public void setSortingOrder(String sortingOrder) {
        this.sortingOrder = sortingOrder;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
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
