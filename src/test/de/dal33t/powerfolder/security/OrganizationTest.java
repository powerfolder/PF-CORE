/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.dal33t.powerfolder.util.Format;

public class OrganizationTest {

    @Test
    public void testAddNotesWithDate() {
        // 1. Arrange
        Organization organization = new Organization();
        organization.setNotes("");
        String notes = "Notes";
        String date = Format.formatDateCanonical(new Date());
        
        // 2. Action
        organization.addNotesWithDate("");
        
        // 3. Assert
        assertEquals(organization.getNotes(), "");
        
        // 2. Action
        organization.addNotesWithDate(notes);
        
        // 3. Assert
        assertEquals(organization.getNotes(), date + ": " + notes);
        
        // 2. Action
        organization.addNotesWithDate(notes);
        
        // 3. Assert
        assertEquals(organization.getNotes(), date + ": " + notes + "\n" + date + ": " + notes);
    }

    @Test
    public void prepareDomains() {
        String host = "subdir.of.another.hostname.com";
        List<String> listOfDomains = Organization.prepareDomains(host);

        assertEquals(4, listOfDomains.size());
        assertTrue(listOfDomains.contains("subdir.of.another.hostname.com"));
        assertTrue(listOfDomains.contains("of.another.hostname.com"));
        assertTrue(listOfDomains.contains("another.hostname.com"));
        assertTrue(listOfDomains.contains("hostname.com"));
    }
}
