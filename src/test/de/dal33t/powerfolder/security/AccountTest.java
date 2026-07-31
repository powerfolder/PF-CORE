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
 *
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.Format;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class AccountTest {

    /**
     * PFS-5510: {@link Account#getAllowedAccess(FolderInfo, String)} computes the EFFECTIVE access
     * on the addressed location - permissions granted on an enclosing shared subfolder (directly or
     * through (nested) groups) raise it above the plain top-folder access, and it is never lower.
     */
    @Test
    public void testEffectiveAllowedAccessOnSubFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top");
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top,
            "structure/deep/shared", null, 0, null, null, new Date(), 1, null, true, null);
        FolderInfo structureDeepShared = FolderInfoFactory.newFolder(location);

        // hans: direct READ on the top folder, READ_WRITE through a group on the subfolder
        Group writers = new Group("Writers");
        writers.grant(FolderPermission.readWrite(structureDeepShared));

        Account hans = new Account();
        hans.grant(FolderPermission.read(top));
        hans.addGroup(writers);

        assertEquals(AccessMode.READ, hans.getAllowedAccess(top));
        assertEquals(AccessMode.READ, hans.getAllowedAccess(top, "elsewhere/file.txt"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(top, "structure/deep/shared"));
        assertEquals(AccessMode.READ_WRITE,
            hans.getAllowedAccess(top, "structure/deep/shared/deeper/file.txt"));
        // the subfolder itself as the addressed folder
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(structureDeepShared, ""));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(structureDeepShared, "deeper/file.txt"));

        // nested groups: member of the child group only, the parent holds the subfolder permission
        Group parent = new Group("Parent");
        parent.grant(FolderPermission.readWrite(structureDeepShared));
        Group child = new Group("Child");
        child.addParent(parent);

        Account nested = new Account();
        nested.addGroup(child);

        assertEquals(AccessMode.NO_ACCESS, nested.getAllowedAccess(top));
        assertEquals(AccessMode.READ_WRITE,
            nested.getAllowedAccess(top, "structure/deep/shared/deep/file.txt"));
        assertEquals(AccessMode.NO_ACCESS, nested.getAllowedAccess(top, "unrelated"));

        // a weaker subfolder grant never lowers a stronger top-folder access (union, highest wins)
        Group readers = new Group("Readers");
        readers.grant(FolderPermission.read(structureDeepShared));

        Account boss = new Account();
        boss.grant(FolderPermission.readWrite(top));
        boss.addGroup(readers);

        assertEquals(AccessMode.READ_WRITE,
            boss.getAllowedAccess(top, "structure/deep/shared/file.txt"));
    }

    /**
     * PFS-5510: mirrors the manually verified end-to-end scenario (localhost + narvi QA):
     * hans@powerfolder.com holds direct READ on the top folder "!Test!", the group "Schreibgruppe"
     * holds READ_WRITE on the shared subfolder "!Test!/Schreibrechte", and hans is a member of that
     * group. hans must be able to write inside "Schreibrechte" (root and deeper) while the rest of
     * "!Test!" stays read-only - the direct READ must not override the group write access.
     */
    @Test
    public void testHansWritesInSchreibrechteViaSchreibgruppe() {
        FolderInfo test = FolderInfoFactory.newTopFolderForTest("!Test!", "test");
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(test,
            "Schreibrechte", null, 0, null, null, new Date(), 1, null, true, null);
        FolderInfo schreibrechte = FolderInfoFactory.newFolder(location);

        Group schreibgruppe = new Group("Schreibgruppe");
        schreibgruppe.grant(FolderPermission.readWrite(schreibrechte));

        Account hans = new Account();
        hans.setUsername("hans@powerfolder.com");
        hans.grant(FolderPermission.read(test));
        hans.addGroup(schreibgruppe);

        // hans sees the top folder read-only ...
        assertEquals(AccessMode.READ, hans.getAllowedAccess(test));
        assertEquals(AccessMode.READ, hans.getAllowedAccess(test, "elsewhere/report.txt"));
        // ... but has effective write access inside the shared subfolder - root and deeper
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(test, "Schreibrechte"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(test, "Schreibrechte/deep"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(test, "Schreibrechte/deep/upload.txt"));
        // the subfolder addressed by its own folder id behaves the same
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(schreibrechte, ""));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(schreibrechte, "deep"));

        // without the group membership only the direct READ remains
        hans.removeGroup(schreibgruppe);
        assertEquals(AccessMode.READ, hans.getAllowedAccess(test, "Schreibrechte/deep"));
    }

    /**
     * PFS-5510 / PFC-3543: an INTERRUPTED subfolder is decoupled from its top folder - the top-folder
     * permission does not reach it, only permissions granted on the subfolder itself (directly or
     * through groups) count there. The effective access may therefore be LOWER than the top-folder
     * access inside an interrupted subfolder.
     */
    @Test
    public void testEffectiveAllowedAccessHonorsInterruptedInheritance() {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        try {
            FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top-inh");
            DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top,
                "isolated", null, 0, null, null, new Date(), 1, null, true, null);
            FolderInfo isolated = FolderInfoFactory.changeInheritsPermissions(
                FolderInfoFactory.newFolder(location), false);

            Group readers = new Group("Readers");
            readers.grant(FolderPermission.read(isolated));

            Account account = new Account();
            account.grant(FolderPermission.readWrite(top));
            account.addGroup(readers);

            // Outside the interrupted subfolder the top-folder READ_WRITE applies...
            assertEquals(AccessMode.READ_WRITE, account.getAllowedAccess(top, "elsewhere"));
            // ...inside it the top permission does NOT inherit - only the subfolder grant counts.
            assertEquals(AccessMode.READ, account.getAllowedAccess(top, "isolated"));
            assertEquals(AccessMode.READ, account.getAllowedAccess(top, "isolated/deep/file.txt"));
        } finally {
            // The feature flag is process-wide - never leak it into other tests.
            Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        }
    }

    @Test
    public void testLEU() {
        // 1. Arrange
        String ldapSearchBase1 = "OU=User,OU=Xxx,DC=adint,DC=dir";
        String ldapSearchBase2 = "OU=Xxx,DC=adint,DC=dir";
        
        Account shansen = new Account();
        shansen.setUsername("shansen");
        shansen.addEmail("shansen@xx.de", ldapSearchBase1);
        shansen.addEmail("shansen@uni.xx.de");
        shansen.addEmail("sohn.hansen@xx.de");
        shansen.addEmail("sohn.hansen@uni.xx.de");
        
        ArrayList<String> ldapEmails = new ArrayList<String>();
        ldapEmails.add("shansen@xx.de");

        // 2. Action
        boolean changed = shansen.removeNonExistingLdapEmails(ldapEmails, ldapSearchBase1);
        
        // 3. Assert
        assertEquals(4, shansen.getEmails().size());
        assertFalse(changed);
    }
    
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
        String ldapSearchBase1 = "dc=Test1";
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

    /**
     * PFS-5684: The base path follows the username on a rename.
     */
    @Test
    public void testBasePathFollowsUsernameRename() {
        Account account = new Account();
        account.setUsername("usera");
        account.setBasePath("/data/powerfolder/usera");

        account.setUsername("userb");

        assertEquals("/data/powerfolder/userb", account.getBasePath());
    }

    @Test
    public void testBasePathFollowsUsernameRenameOnWindowsPath() {
        Account account = new Account();
        account.setUsername("usera");
        account.setBasePath("D:\\PowerFolder\\storage\\usera");

        account.setUsername("userb");

        assertEquals("D:\\PowerFolder\\storage\\userb", account.getBasePath());
    }

    @Test
    public void testBasePathFollowsUsernameRenameWithMail() {
        Account account = new Account();
        account.setUsername("user.a@company.com");
        account.setBasePath("/data/powerfolder/user.a@company.com");

        account.setUsername("user.b@company.com");

        assertEquals("/data/powerfolder/user.b@company.com", account.getBasePath());
    }

    @Test
    public void testBasePathFollowsUsernameRenameWithInvalidFilenameChars() {
        Account account = new Account();
        account.setUsername("user/a");
        account.setBasePath("/data/powerfolder/user_a");

        account.setUsername("user:b");

        assertEquals("/data/powerfolder/user_b", account.getBasePath());
    }

    @Test
    public void testBasePathFollowsUsernameRenameWithTrailingSeparator() {
        Account account = new Account();
        account.setUsername("usera");
        account.setBasePath("/data/powerfolder/usera/");

        account.setUsername("userb");

        assertEquals("/data/powerfolder/userb/", account.getBasePath());
    }

    @Test
    public void testBasePathFollowsUsernameRenameCaseInsensitive() {
        Account account = new Account();
        account.setUsername("UserA");
        account.setBasePath("/data/powerfolder/usera");

        account.setUsername("userb");

        assertEquals("/data/powerfolder/userb", account.getBasePath());
    }

    @Test
    public void testCustomBasePathIsKeptOnUsernameRename() {
        Account account = new Account();
        account.setUsername("usera");
        account.setBasePath("/mnt/volume7/customdir");

        account.setUsername("userb");

        assertEquals("/mnt/volume7/customdir", account.getBasePath());
    }

    @Test
    public void testBasePathUntouchedWithoutRename() {
        Account account = new Account();
        account.setUsername("usera");
        account.setBasePath("/data/powerfolder/usera");

        // Same username again, e.g. on a store without changes
        account.setUsername("usera");

        assertEquals("/data/powerfolder/usera", account.getBasePath());
    }

    @Test
    public void testBlankBasePathStaysBlankOnUsernameRename() {
        Account account = new Account();
        account.setUsername("usera");

        account.setUsername("userb");

        assertEquals(null, account.getBasePath());
    }

    @Test
    public void testBasePathIsNotAdaptedOnAccountCreation() {
        Account account = new Account();
        account.setBasePath("/data/powerfolder/usera");

        // No old username: this is the initial assignment, not a rename
        account.setUsername("userb");

        assertEquals("/data/powerfolder/usera", account.getBasePath());
    }

}
