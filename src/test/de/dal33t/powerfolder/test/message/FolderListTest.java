/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: TestHelper.java 13087 2010-07-26 11:36:59Z tot $
 */
package de.dal33t.powerfolder.test.message;

import java.nio.file.Path;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FolderListTest extends TestCase {

    public void testStoreLoad() {
        FolderList list = new FolderList();
        list.joinedMetaFolders = true;
        list.secretFolders = new FolderInfo[10];

        for (int i = 0; i < list.secretFolders.length; i++) {
            FolderInfo foInfo = new FolderInfo("Na" + IdGenerator.makeId(),
                IdGenerator.makeFolderId());
            list.secretFolders[i] = foInfo;
        }

        Path file = TestHelper.getTestDir().resolve("subdir/FolderList");
        assertTrue(list.store(file));
        assertEquals(list, FolderList.load(file));

        file = TestHelper.getTestDir().resolve("subdir/dsds/FolderList");
        assertTrue(list.store(file));
        assertEquals(list, FolderList.load(file));
    }
}
