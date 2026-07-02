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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.Format;

public class GroupTest {

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
}
