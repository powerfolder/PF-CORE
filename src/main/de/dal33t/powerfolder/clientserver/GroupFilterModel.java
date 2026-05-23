/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
package de.dal33t.powerfolder.clientserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.security.Organization;

public class GroupFilterModel implements Serializable {
    private static final long serialVersionUID = 100L;

    private String queryname;
    private String organizationOID = Organization.FILTER_MATCH_ALL;
    private List<String> adminOfOrganizationOIDs;

    private int maxResults;

    public static GroupFilterModel all(int maxResults) {
        GroupFilterModel filterModel = new GroupFilterModel();
        filterModel.maxResults = maxResults;
        return filterModel;
    }

    // Getter and Setter

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public String getQueryname() {
        return queryname;
    }

    public void setQueryname(String queryname) {
        this.queryname = queryname ;

    }

    public String getMemberOfOrganizationOID() {
        return organizationOID;
    }

    public void setMemberOfOrganizationOID(String organizationOID) {
        this.organizationOID = organizationOID;
    }

    public boolean isMemberOfAnyOrganization() {
        return Organization.FILTER_MATCH_ALL.equals(organizationOID);
    }

    public List<String> getAdminOfOrganizationOIDs() {
        return adminOfOrganizationOIDs;
    }

    public void setAdminOfOrganizationOIDs(List<String> orgOIDs) {
        adminOfOrganizationOIDs = orgOIDs;
    }

    public void addAdminOfOrganizationOIDs(String orgOID) {
        if (orgOID == null) {
            return;
        }
        if (adminOfOrganizationOIDs == null) {
            adminOfOrganizationOIDs = new ArrayList<>();
        }
        adminOfOrganizationOIDs.add(orgOID);
    }

    // Logic

    public void reset() {
        queryname = null;
        maxResults = 0;
        adminOfOrganizationOIDs = null;
        organizationOID = Organization.FILTER_MATCH_ALL;
    }
}
