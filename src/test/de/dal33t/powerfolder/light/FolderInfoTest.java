package de.dal33t.powerfolder.light;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.util.IdGenerator;

public class FolderInfoTest extends TestCase {

    public void testGetMetaInfo() {
        FolderInfo foInfo = new FolderInfo("Name of folder",
            IdGenerator.makeId());
        assertFalse(foInfo.toString(), foInfo.isMetaFolder());
        assertFalse(foInfo.id,
            foInfo.id.contains(Constants.METAFOLDER_ID_PREFIX));
        assertFalse(foInfo.name,
            foInfo.name.contains(Constants.METAFOLDER_ID_PREFIX));

        FolderInfo metaFolder = foInfo.getMetaFolderInfo();
        assertTrue(metaFolder.toString(), metaFolder.isMetaFolder());
        assertTrue(metaFolder.id,
            metaFolder.id.contains(Constants.METAFOLDER_ID_PREFIX));
        assertTrue(metaFolder.name,
            metaFolder.name.contains(Constants.METAFOLDER_ID_PREFIX));

        assertEquals(foInfo, metaFolder.getParentFolderInfo());
        assertEquals(metaFolder, foInfo.getMetaFolderInfo());

        // Fallback stuff if something really is wrong in the code:
        assertEquals(metaFolder, metaFolder.getMetaFolderInfo());
        assertEquals(foInfo, foInfo.getParentFolderInfo());
        assertFalse(metaFolder.equals(foInfo));
    }

}
