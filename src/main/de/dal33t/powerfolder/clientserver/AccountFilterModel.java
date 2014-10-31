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

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.security.Organization;

public class AccountFilterModel extends Model {
    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_DISABLED_ONLY = "disabledOnly";
    public static final String PROPERTY_PRO_USERS_ONLY = "proUsersOnly";
    public static final String PROPERTY_PAYING_OS_ONLY = "payingOSOnly";
    public static final String PROPERTY_ACTIVE_TRIAL = "activeTrial";
    public static final String PROPERTY_USERNAME = "username";
    public static final String PROPERTY_QUERYNAME = "queryname";
    public static final String PROPERTY_PAGE_SIZE = "pageSize";
    public static final String PROPERTY_PAGE_NUMBER = "pageNumber";

    private boolean disabledOnly;
    private boolean proUsersOnly;
    private boolean activeTrial;
    private String username;
    private String queryname;
    private String organizationOID = Organization.FILTER_MATCH_ALL;
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
        firePropertyChange(PROPERTY_DISABLED_ONLY, oldValue, this.disabledOnly);
    }

    public boolean isProUsersOnly() {
        return proUsersOnly;
    }

    public void setProUsersOnly(boolean proUsersOnly) {
        Object oldValue = isProUsersOnly();
        this.proUsersOnly = proUsersOnly;
        firePropertyChange(PROPERTY_PRO_USERS_ONLY, oldValue, this.proUsersOnly);
    }

    public boolean isActiveTrial() {
        return activeTrial;
    }

    public void setActiveTrial(boolean activeTrial) {
        Object oldValue = isActiveTrial();
        this.activeTrial = activeTrial;
        firePropertyChange(PROPERTY_ACTIVE_TRIAL, oldValue, this.activeTrial);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Object oldValue = getUsername();
        this.username = username != null ? username.toLowerCase().trim() : null;
        firePropertyChange(PROPERTY_USERNAME, oldValue, this.username);
    }

    public String getQueryname() {
        return queryname;
    }

    public void setQueryname(String queryname) {
        Object oldValue = getQueryname();
        this.queryname = queryname != null
            ? queryname.toLowerCase().trim()
            : null;
        firePropertyChange(PROPERTY_QUERYNAME, oldValue, this.queryname);
    }

    public String getOrganizationOID() {
        return organizationOID;
    }

    public void setOrganizationOID(String organizationOID) {
        this.organizationOID = organizationOID;
    }

    public boolean isAnyOrganization() {
        return Organization.FILTER_MATCH_ALL.equals(organizationOID);
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

    // Logic ******************************************************************

    public void reset() {
        activeTrial = false;
        disabledOnly = false;
        proUsersOnly = false;
        username = null;
        organizationOID = Organization.FILTER_MATCH_ALL;
        maxResults = 0;
    }
}
