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
package de.dal33t.powerfolder.light;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;


public class FolderInfoFactoryTest {


    @Test
    public void testParentFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        DirectoryInfo subDir = FileInfoFactory.lookupDirectory(top, "subDirect");
        FolderInfo subDirect = FolderInfoFactory.newFolder(subDir);
        assertEquals(top, subDirect.getParent().getFolderInfo());
        assertEquals("", subDirect.getParent().getRelativeName());
        assertEquals("subDirect", subDirect.getLocation().getRelativeName());

        DirectoryInfo topDeepDir = FileInfoFactory.lookupDirectory(top, "this/is/in/a/deep/structure/subDeep");
        FolderInfo subDeep = FolderInfoFactory.newFolder(topDeepDir);

        assertEquals(top, subDeep.getParent().getFolderInfo());
        assertEquals("this/is/in/a/deep/structure", subDeep.getParent().getRelativeName());
        assertEquals("this/is/in/a/deep/structure/subDeep", subDeep.getLocation().getRelativeName());

        subDirect = FolderInfoFactory.rename(subDirect, "subDirectRENAMED");
        assertEquals(top, subDirect.getParent().getFolderInfo());
        assertEquals("", subDirect.getParent().getRelativeName());
    }




    /**
     * PFC-3543: a subfolder names the TOP folder as its top folder - always, however deep it sits and
     * whatever coordinates the caller holds it in. A directory inside an already interrupted subfolder
     * arrives in THAT subfolder's coordinates (the DAO proxy answers in the coordinates of the folder
     * owning the row); taking it unchanged would make the middle folder the top folder of the new one,
     * and the chain then holds itself in place through fk_fi_topfolder.
     */
    @Test
    public void testSubFolderOfSubFolderStillPointsAtTheTopFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo outer = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "outer"));
        assertEquals(top, outer.getTopFolder());

        // "inner" as the outer subfolder hands it out: its own folder, its own relative name.
        DirectoryInfo innerInOuter = FileInfoFactory.lookupDirectory(outer, "inner");
        FolderInfo inner = FolderInfoFactory.newFolder(innerInOuter);

        assertEquals(top, inner.getTopFolder(), "The top folder is the root, not the enclosing subfolder");
        assertFalse(inner.getTopFolder().isSubFolder(), "The top folder must not itself be a subfolder");
        assertEquals("outer", inner.getTopPath(), "The nesting shows in the path");
        assertEquals("outer/inner", inner.getLocation().getRelativeName());
        assertEquals("outer/inner", inner.locationPath());
    }


    /**
     * PFC-3543: the same holds for a parent that was not computed here but read - from a stored row or
     * from a peer that sent the folder as it had it. Both come through FolderInfo#setParent.
     */
    @Test
    public void testReadParentInSubFolderCoordinatesIsLifted() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo outer = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "outer"));

        FolderInfo inner = FolderInfoFactory.unmarshallExistingFolder("inner-id", "inner", 3,
            FileInfoFactory.lookupDirectory(outer, "middle"));

        assertEquals(top, inner.getTopFolder());
        assertEquals("outer/middle", inner.getTopPath());
        assertEquals("outer/middle/inner", inner.locationPath());
    }

    /**
     * PFS-5850: the name of a subfolder IS the last segment of its location - a rename is therefore a
     * location change, and everything derived from the location (base dir, barrier, row prefix) moves
     * with it. This pins the property the relocation relies on.
     */
    @Test
    public void testTheNameIsTheLastSegmentOfTheLocation() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo sub = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "a/b/name"));
        assertEquals("a/b/name", sub.locationPath());

        FolderInfo renamed = FolderInfoFactory.rename(sub, "other");
        assertEquals("a/b", renamed.getTopPath());
        assertEquals("a/b/other", renamed.locationPath());
        assertEquals(sub.getId(), renamed.getId(), "A rename is a new version of the same folder");
    }

    /**
     * PFS-5850: a move is ONE version bump for parent and name together - two calls would publish an
     * intermediate FolderInfo naming a location that never existed on disk. Tags and the inheritance
     * flag travel with it, like on a rename, and the identity does not change at all.
     */
    @Test
    public void testMoveCarriesEverythingButTheLocation() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo sub = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "a/b/name"));
        sub = FolderInfoFactory.changeTags(sub, "[\"tag\"]");
        sub = FolderInfoFactory.changeInheritsPermissions(sub, false);
        int versionBefore = sub.getVersion();

        FolderInfo moved = FolderInfoFactory.move(sub, FileInfoFactory.lookupDirectory(top, "c"), "other");

        assertEquals("c/other", moved.locationPath());
        assertEquals(versionBefore + 1, moved.getVersion(), "One bump for parent and name together");
        assertEquals(sub.getId(), moved.getId(), "The identity is what makes permissions survive");
        assertEquals(sub.getTags(), moved.getTags(), "The tags travel");
        // The getter answers "inherits" while the feature is off, so the stored flag is what is asserted
        // here - that is the value the factory has to carry.
        assertFalse(moved.storedInheritsPermissions(), "The interruption travels");
        assertEquals(top, moved.getTopFolder(), "The top folder is unchanged");
    }

    /** A move to the place it already sits changes nothing - not even the version. */
    @Test
    public void testMoveToTheSamePlaceIsNoChange() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo sub = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "a/name"));

        assertSame(sub, FolderInfoFactory.move(sub, FileInfoFactory.lookupDirectory(top, "a"), "name"));
    }

}
