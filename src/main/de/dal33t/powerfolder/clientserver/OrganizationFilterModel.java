package de.dal33t.powerfolder.clientserver;

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

import com.jgoodies.binding.beans.Model;

public class OrganizationFilterModel extends Model {

    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_NAME = "name";

    private String name;
    private int maxResults;

    // Getter and Setter

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Object oldValue = getName();
        this.name = name;
        firePropertyChange(PROPERTY_NAME, oldValue, this.name);
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
