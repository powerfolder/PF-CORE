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
import de.dal33t.powerfolder.Constants;

public class FolderInfoTest extends TestCase {

    public void testGetMetaInfo() {
        FolderInfo foInfo = FolderInfoFactory.newTopFolderForTest("Name of folder");
        assertFalse(foInfo.toString(), foInfo.isMetaFolder());
        assertFalse(foInfo.id,
            foInfo.id.contains(Constants.METAFOLDER_ID_PREFIX));
        assertFalse(foInfo.getName(),
            foInfo.getName().contains(Constants.METAFOLDER_ID_PREFIX));

        FolderInfo metaFolder = foInfo.getMetaFolderInfo();
        assertTrue(metaFolder.toString(), metaFolder.isMetaFolder());
        assertTrue(metaFolder.id,
            metaFolder.id.contains(Constants.METAFOLDER_ID_PREFIX));
        assertTrue(metaFolder.getName(),
            metaFolder.getName().contains(Constants.METAFOLDER_ID_PREFIX));

        assertEquals(foInfo, metaFolder.lookupContentFolderInfo());
        assertEquals(metaFolder, foInfo.getMetaFolderInfo());

        // Fallback stuff if something really is wrong in the code:
        assertEquals(metaFolder, metaFolder.getMetaFolderInfo());
        assertEquals(foInfo, foInfo.lookupContentFolderInfo());
        assertFalse(metaFolder.equals(foInfo));
    }

}
