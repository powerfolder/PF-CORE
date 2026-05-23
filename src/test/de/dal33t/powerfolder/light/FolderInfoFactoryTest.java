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



}
