package de.dal33t.powerfolder.clientserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 */

public class OrganizationFilterModel implements Serializable {

    private static final long serialVersionUID = 100L;

    private String name;
    private int maxResults;
    private int pageNumber = 1;
    private List<String> orgOIDs;
    private String adminAccountOID;
    private String adminAccountUsername;

    // Getter and Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Object oldValue = getName();
        this.name = name;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        this.pageNumber = pageNumber;
    }


    /**
     * Calculates the offset for database queries.
     * Same semantics as AccountFilterModel.
     */
    public int getOffset() {
        if (pageNumber <= 1 || maxResults <= 0) {
            return 0;
        }
        return (pageNumber - 1) * maxResults;
    }

    public boolean isEmptyOrgOIDs() {
        return orgOIDs == null || orgOIDs.isEmpty();
    }

    public List<String> getOrgOIDs() {
        return orgOIDs;
    }

    public void setOrgOIDs(List<String> orgOIDs) {
        this.orgOIDs = orgOIDs;
    }

    public void addOrgOID(String orgOID) {
        if (orgOIDs == null) {
            orgOIDs = new ArrayList<>();
        }
        orgOIDs.add(orgOID);
    }

    public String getAdminAccountOID() {
        return adminAccountOID;
    }

    public void setAdminAccountOID(String adminAccountOID) {
        this.adminAccountOID = adminAccountOID;
    }

    public String getAdminAccountUsername() {
        return adminAccountUsername;
    }

    public void setAdminAccountUsername(String adminAccountUsername) {
        this.adminAccountUsername = adminAccountUsername;
    }
}
