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

import junit.framework.TestCase;

import java.util.Date;


public class FolderInfoFactoryTest extends TestCase {


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
    public void testSubFolderOfSubFolderStillPointsAtTheTopFolder() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo outer = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "outer"));
        assertEquals(top, outer.getTopFolder());

        // "inner" as the outer subfolder hands it out: its own folder, its own relative name.
        DirectoryInfo innerInOuter = FileInfoFactory.lookupDirectory(outer, "inner");
        FolderInfo inner = FolderInfoFactory.newFolder(innerInOuter);

        assertEquals("The top folder is the root, not the enclosing subfolder", top, inner.getTopFolder());
        assertFalse("The top folder must not itself be a subfolder", inner.getTopFolder().isSubFolder());
        assertEquals("The nesting shows in the path", "outer", inner.getTopPath());
        assertEquals("outer/inner", inner.getLocation().getRelativeName());
        assertEquals("outer/inner", inner.locationPath());
    }


    /**
     * PFC-3543: the same holds for a parent that was not computed here but read - from a stored row or
     * from a peer that sent the folder as it had it. Both come through FolderInfo#setParent.
     */
    public void testReadParentInSubFolderCoordinatesIsLifted() {
        FolderInfo top = FolderInfoFactory.newTopFolder("top");
        FolderInfo outer = FolderInfoFactory.newFolder(FileInfoFactory.lookupDirectory(top, "outer"));

        FolderInfo inner = FolderInfoFactory.unmarshallExistingFolder("inner-id", "inner", 3,
            FileInfoFactory.lookupDirectory(outer, "middle"));

        assertEquals(top, inner.getTopFolder());
        assertEquals("outer/middle", inner.getTopPath());
        assertEquals("outer/middle/inner", inner.locationPath());
    }

}
