package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.Constants;
import junit.framework.TestCase;

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

        assertEquals(foInfo, metaFolder.lookupParentFolderInfo());
        assertEquals(metaFolder, foInfo.getMetaFolderInfo());

        // Fallback stuff if something really is wrong in the code:
        assertEquals(metaFolder, metaFolder.getMetaFolderInfo());
        assertEquals(foInfo, foInfo.lookupParentFolderInfo());
        assertFalse(metaFolder.equals(foInfo));
    }

    public void testGetArchiveInfo() {
        FolderInfo foInfo = FolderInfoFactory.newTopFolderForTest("Name of folder");
        assertFalse(foInfo.toString(), foInfo.isArchiveFolder());
        assertFalse(foInfo.id,
                foInfo.id.contains(Constants.ARCHIVEFOLDER_ID_PREFIX));
        assertFalse(foInfo.getName(),
                foInfo.getName().contains(Constants.ARCHIVEFOLDER_ID_PREFIX));

        FolderInfo archiveFolder = foInfo.getArchiveFolderInfo();
        assertTrue(archiveFolder.toString(), archiveFolder.isArchiveFolder());
        assertTrue(archiveFolder.id,
                archiveFolder.id.contains(Constants.ARCHIVEFOLDER_ID_PREFIX));
        assertTrue(archiveFolder.getName(),
                archiveFolder.getName().contains(Constants.ARCHIVEFOLDER_ID_PREFIX));

        assertEquals(foInfo, archiveFolder.lookupParentFolderInfo());
        assertEquals(archiveFolder, foInfo.getArchiveFolderInfo());

        // Fallback stuff if something really is wrong in the code:
        assertEquals(archiveFolder, archiveFolder.getArchiveFolderInfo());
        assertEquals(foInfo, foInfo.lookupParentFolderInfo());
        assertFalse(archiveFolder.equals(foInfo));
    }
}
