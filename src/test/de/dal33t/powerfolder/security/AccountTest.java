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
 *
 * $Id$
 */
package de.dal33t.powerfolder.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import de.dal33t.powerfolder.util.Format;

public class AccountTest {

    @Test
    public void testJSONData() throws JSONException {
        Account a = new Account();
        JSONObject o = a.getJSONObject();
        o.put("cmpEnabled", true);
        o.put("avangateSubscriptionID", "3DEC58");
        a.setJSONObject(o);

        JSONObject r = a.getJSONObject();
        assertTrue(r.getBoolean("cmpEnabled"));
        assertEquals("3DEC58", r.get("avangateSubscriptionID"));

        a.put("avangateSubscriptionID", "XASA");

        r = a.getJSONObject();
        assertTrue(r.getBoolean("cmpEnabled"));
        assertEquals("XASA", r.get("avangateSubscriptionID"));
    }

    @Test
    public void testAddNotesWithDate() {
        // 1. Arrange
        Account account = new Account();
        account.setNotes("");
        String notes = "Notes";
        String date = Format.formatDateCanonical(new Date());
        
        // 2. Action
        account.addNotesWithDate("");
        
        // 3. Assert
        assertEquals(account.getNotes(), "");
        
        // 2. Action
        account.addNotesWithDate(notes);
        
        // 3. Assert
        assertEquals(account.getNotes(), date + ": " + notes);
        
        // 2. Action
        account.addNotesWithDate(notes);
        
        // 3. Assert
        assertEquals(account.getNotes(), date + ": " + notes + "\n" + date + ": " + notes);
    }
    
    @Test
    public void testAddEmail() {
        // 1. Arrange
        Account account = new Account();

        // 3. Assert
        try {
            account.addEmail("");
            fail("Expected IllegalArgumentException is not thrown");
        } catch (IllegalArgumentException exception) {
            assertEquals("Email", exception.getMessage());
        }

        // 2. Action
        boolean added = account.addEmail("Test@test.de");
        
        // 3. Assert
        assertEquals("test@test.de", account.getEmails().get(0));
        assertTrue(added);
        
        // 2. Action
        added = account.addEmail("test@test.de");

        // 3. Assert
        assertFalse(added);
    }
    
    @Test
    public void testAddEmailWithLDAPSearchBase() {
        // 1. Arrange
        String email1 = "Test1@test.de";
        String email2 = "test1@test.de";
        String ldapSearchBase1 = "dc=Test1";
        String ldapSearchBase2 = "dc=test1";
        String ldapSearchBase3 = "dc=test2";
        Account account = new Account();

        // Empty email
        // 3. Assert
        try {
            account.addEmail("", ldapSearchBase1);
            fail("Expected IllegalArgumentException is not thrown");
        } catch (IllegalArgumentException exception) {
            assertEquals("Email", exception.getMessage());
        }
        
        // Empty LDAP
        // 3. Assert
        try {
            account.addEmail(email1, "");
            fail("Expected IllegalArgumentException is not thrown");
        } catch (IllegalArgumentException exception) {
            assertEquals("LDAP", exception.getMessage());
        }

        // Add email with LDAP
        // 2. Action
        boolean changed = account.addEmail(email1, ldapSearchBase1);
        
        // 3. Assert
        assertEquals(1, account.getEmails().size());
        assertEquals(email2, account.getEmails().get(0));
        assertTrue(changed);
        
        // Add same email with LDAP again (test duplicate detection)
        // 2. Action
        changed = account.addEmail(email1, ldapSearchBase2);
        
        // 3. Assert
        assertEquals(1, account.getEmails().size());
        assertEquals(email2, account.getEmails().get(0));
        assertFalse(changed);
        
        // Add same email with and without LDAP (test duplicate deletion)
        // 2. Action
        changed = account.addEmail(email1);
        changed = account.addEmail(email1, ldapSearchBase2);
        
        // 3. Assert
        assertEquals(1, account.getEmails().size());
        assertEquals(email2, account.getEmails().get(0));
        assertTrue(changed);
        
        // Add same email with other LDAP
        // 2. Action
        changed = account.addEmail(email1);
        changed = account.addEmail(email1, ldapSearchBase3);
        
        // 3. Assert
        assertEquals(2, account.getEmails().size());
        assertEquals(email2, account.getEmails().get(0));
        assertEquals(email2, account.getEmails().get(1));
        assertTrue(changed);
    }
    
    @Test
    public void testRemoveEmail() {
        // 1. Arrange
        String email = "Test1@test.de";
        Account account = new Account();
        account.addEmail(email);
        
        // 2. Action
        boolean deleted = account.removeEmail(email);
        
        // 3. Assert
        assertEquals(0, account.getEmails().size());
        assertTrue(deleted);
        
        // 1. Arrange
        String ldapSearchBase = "dc=test1";
        account.addEmail(email, ldapSearchBase);
        
        // 2. Action
        deleted = account.removeEmail(email);
        
        // 3. Assert
        assertEquals(0, account.getEmails().size());
        assertTrue(deleted);
    }
    
    @Test
    public void testRemoveNonExistingLdapEmails() {
        // 1. Arrange
        String email1 = "test1@test.de";
        String email2 = "test2@test.de";
        String email3 = "test3@test.de";
        String email4 = "test4@test.de";
        String ldapSearchBase1 = "dc=test1";
        String ldapSearchBase2 = "dc=test2";
        Account account = new Account();
        account.addEmail(email1, ldapSearchBase1);
        account.addEmail(email2, ldapSearchBase1);
        account.addEmail(email3, ldapSearchBase2);
        account.addEmail(email4);
        ArrayList<String> ldapEmails = new ArrayList<String>();
        ldapEmails.add(email1);

        // 2. Action
        boolean changed = account.removeNonExistingLdapEmails(ldapEmails, ldapSearchBase1);
        
        // 3. Assert
        assertEquals(3, account.getEmails().size());
        assertEquals(email1, account.getEmails().get(0));
        assertEquals(email3, account.getEmails().get(1));
        assertEquals(email4, account.getEmails().get(2));
        assertTrue(changed);
    }
    
    @Test
    public void testGetEmails() {
        // 1. Arrange
        String email1 = "test1@test.de";
        String email2 = "test2@test.de";
        String email3 = "test3@test.de";
        String ldapSearchBase = "dc=test1";
        Account account = new Account();
        account.addEmail(email1, ldapSearchBase);
        account.addEmail(email2, ldapSearchBase);
        account.addEmail(email3);
        
        // 2. Action
        List<String> emails = account.getEmails();
        
        // 3. Assert
        assertEquals(email1, emails.get(0));
        assertEquals(email2, emails.get(1));
        assertEquals(email3, emails.get(2));
    }
    
}
