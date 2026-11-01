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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.Format;

public class GroupTest {

    /**
     * PFS-5510: {@link Group#getAllowedAccess(FolderInfo, String)} computes the group's EFFECTIVE
     * access on the addressed location, including permissions inherited from parent groups.
     */
    @Test
    public void testEffectiveAllowedAccessOnSubFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top");
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top,
            "team/shared", null, 0, null, null, new Date(), 1, null, true, null);
        FolderInfo teamShared = FolderInfoFactory.newFolder(location);

        Group writers = new Group("Writers");
        writers.grant(FolderPermission.readWrite(teamShared));

        assertEquals(AccessMode.NO_ACCESS, writers.getAllowedAccess(top));
        assertEquals(AccessMode.READ_WRITE, writers.getAllowedAccess(top, "team/shared"));
        assertEquals(AccessMode.READ_WRITE, writers.getAllowedAccess(top, "team/shared/deep/file.txt"));
        assertEquals(AccessMode.NO_ACCESS, writers.getAllowedAccess(top, "team/other"));
        // the subfolder itself as the addressed folder
        assertEquals(AccessMode.READ_WRITE, writers.getAllowedAccess(teamShared, "deep"));

        // inherited from the parent group (nested groups)
        Group child = new Group("Child");
        child.addParent(writers);
        assertEquals(AccessMode.READ_WRITE, child.getAllowedAccess(top, "team/shared/file.txt"));
        assertEquals(AccessMode.NO_ACCESS, child.getAllowedAccess(top, "team/other"));
    }

    private static final FolderInfo FOLDER_A =
        FolderInfoFactory.newTopFolderForTest("FolderA", "fA");
    private static final FolderInfo FOLDER_B =
        FolderInfoFactory.newTopFolderForTest("FolderB", "fB");

    @Test
    public void testAddNotesWithDate() {
        // 1. Arrange
        Group group = new Group();
        group.setNotes("");
        String notes = "Notes";
        String date = Format.formatDateCanonical(new Date());

        // 2. Action
        group.addNotesWithDate("");

        // 3. Assert
        assertEquals(group.getNotes(), "");

        // 2. Action
        group.addNotesWithDate(notes);

        // 3. Assert
        assertEquals(group.getNotes(), date + ": " + notes);

        // 2. Action
        group.addNotesWithDate(notes);

        // 3. Assert
        assertEquals(group.getNotes(), date + ": " + notes + "\n" + date + ": " + notes);
    }


    @Test
    public void testHasPermissionWalksParents() {
        // Flat group: own permissions only.
        Group solo = new Group("Solo");
        solo.grant(FolderPermission.read(FOLDER_A));
        assertTrue(solo.hasPermission(FolderPermission.read(FOLDER_A)));
        assertFalse(solo.hasPermission(FolderPermission.read(FOLDER_B)));

        // Chain grand -> parent -> child; grant on grand inherited by child.
        Group grand = new Group("Grand");
        Group parent = new Group("Parent");
        Group child = new Group("Child");
        grand.grant(FolderPermission.read(FOLDER_A));
        parent.addParent(grand);
        child.addParent(parent);
        assertTrue(child.hasPermission(FolderPermission.read(FOLDER_A)));
        assertFalse(child.hasPermission(FolderPermission.read(FOLDER_B)));
    }

    @Test
    public void testHasPermissionTerminatesOnCycles() {
        // Defensive: visited-set must short-circuit if a cycle slipped through.
        Group a = new Group("A");
        Group b = new Group("B");
        a.addParent(b);
        b.addParent(a);

        assertFalse(a.hasPermission(FolderPermission.read(FOLDER_A)));
    }

    @Test
    public void testWouldCreateCycle() {
        Group s = new Group("Self");
        assertTrue(s.wouldCreateCycle(s));

        Group a = new Group("A");
        Group b = new Group("B");
        Group c = new Group("C");
        b.addParent(a);
        c.addParent(b);
        assertTrue(c.wouldCreateCycle(a));

        Group d = new Group("D");
        assertFalse(d.wouldCreateCycle(a));
        assertFalse(a.wouldCreateCycle(d));

        Group p1 = new Group("P1");
        Group p2 = new Group("P2");
        Group shared = new Group("Shared");
        assertFalse(p1.wouldCreateCycle(shared));
        shared.addParent(p1);
        assertFalse(p2.wouldCreateCycle(shared));
        shared.addParent(p2);
        assertEquals(2, shared.getParents().size());
    }

    @Test
    public void testAddRemoveParent() {
        Group parent = new Group("P");
        Group child = new Group("C");

        child.addParent(parent);
        child.addParent(parent); // idempotent
        assertEquals(1, child.getParents().size());
        assertTrue(child.getParents().contains(parent));

        child.removeParent(parent);
        assertFalse(child.getParents().contains(parent));
    }

    /**
     * A group never OWNS a folder - ownership carries the quota and the charging and belongs to an
     * account. The grant is skipped for a top folder, for an inheriting subfolder and for an interrupted
     * one (PFC-3543), and it must not throw: it is a class invariant, not a caller error.
     */
    @Test
    public void testOwnerPermissionIsNeverGrantedToAGroup() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top-owner");
        FolderInfo inheriting = newSubFolder(top, "inheriting");
        FolderInfo interrupted = FolderInfoFactory.changeInheritsPermissions(
            newSubFolder(top, "interrupted"), false);

        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        try {
            Group owners = new Group("Owners");
            owners.grant(FolderPermission.owner(top));
            owners.grant(FolderPermission.owner(inheriting));
            owners.grant(FolderPermission.owner(interrupted));

            assertTrue(owners.getPermissions().isEmpty(),
                "A group must not hold any folder permission from an owner grant: "
                + owners.getPermissions());
            assertEquals(AccessMode.NO_ACCESS, owners.getAllowedAccess(top));
            assertEquals(AccessMode.NO_ACCESS, owners.getAllowedAccess(interrupted));
        } finally {
            // The feature flag is process-wide - never leak it into other tests.
            Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        }
    }

    /**
     * How a mode is changed: a bare grant of a LOWER mode is a no-op, because the higher one already
     * implies it - the callers therefore revoke first and grant afterwards
     * ({@code ServerSecurityService.setFolderPermission}). That makes it essential that
     * revokeAllFolderPermissions really clears EVERY mode; a leftover would keep being reported by
     * getAllowedAccess, which checks the higher modes first.
     */
    @Test
    public void testModeIsChangedByRevokingFirst() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top-lower");

        Group team = new Group("Team");
        team.grant(FolderPermission.admin(top));
        assertEquals(AccessMode.ADMIN, team.getAllowedAccess(top));

        // Granting a lower mode on top of a higher one changes nothing - it is already implied
        team.grant(FolderPermission.readWrite(top));
        assertEquals(1, team.getPermissions().size());
        assertEquals(AccessMode.ADMIN, team.getAllowedAccess(top));

        // The supported way down: revoke, then grant
        team.revokeAllFolderPermissions(top);
        assertTrue(team.getPermissions().isEmpty(),
            "Revoke must clear every mode: " + team.getPermissions());
        assertEquals(AccessMode.NO_ACCESS, team.getAllowedAccess(top));

        team.grant(FolderPermission.readWrite(top));
        assertEquals(1, team.getPermissions().size());
        assertEquals(AccessMode.READ_WRITE, team.getAllowedAccess(top));

        // Raising works with a bare grant, since the lower mode does not imply the higher one
        team.grant(FolderPermission.admin(top));
        assertEquals(1, team.getPermissions().size());
        assertEquals(AccessMode.ADMIN, team.getAllowedAccess(top));
    }

    /**
     * PFS-5510: the DIRECT permission is a different question from the effective access, and on a
     * subfolder the two differ. {@code getPermissionFor} answers the direct one on a group (its callers
     * select the direct holders), while {@link #getAllowedAccess} follows parents and inheritance.
     */
    @Test
    public void testDirectPermissionIgnoresParentsAndInheritance() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("TopFolder", "top-direct");
        FolderInfo teamShared = newSubFolder(top, "team/shared");

        Group parent = new Group("Parent");
        parent.grant(FolderPermission.admin(top));
        Group child = new Group("Child");
        child.grant(FolderPermission.read(teamShared));
        child.addParent(parent);

        // Granted here
        assertEquals(AccessMode.READ, child.getDirectPermissionFor(teamShared).getMode());
        assertEquals(AccessMode.READ, child.getPermissionFor(teamShared).getMode());
        // Only inherited, never granted on the child itself
        assertNull(child.getDirectPermissionFor(top));
        assertNull(child.getPermissionFor(top));
        // ...while the effective access sees the parent group and the top folder
        assertEquals(AccessMode.ADMIN, child.getAllowedAccess(top));
        assertEquals(AccessMode.ADMIN, child.getAllowedAccess(teamShared));
    }

    private static FolderInfo newSubFolder(FolderInfo top, String path) {
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top, path,
            null, 0, null, null, new Date(), 1, null, true, null);
        return FolderInfoFactory.newFolder(location);
    }
}
