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
