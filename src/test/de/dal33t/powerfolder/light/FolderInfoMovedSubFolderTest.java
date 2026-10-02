package de.dal33t.powerfolder.light;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * PFS-5926: a permission carries a copy of its folder, location included, and the access to a path is resolved from
 * it. After the subfolder moved, an account still holding the old copy - cached for minutes - opened the subfolder
 * empty: its own permission no longer enclosed the path, and the barrier above it answered instead.
 */
public class FolderInfoMovedSubFolderTest extends TestCase {

    public void testThePathIsResolvedAtTheCurrentLocation() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("Workspace");
        String id = "pfs5926-moved-" + System.nanoTime();
        // Two copies of the same folder - not through the factory, which interns and would hand back the first
        FolderInfo held = new FolderInfo("S", id, 1, FileInfoFactory.lookupDirectory(top, "old"));
        FolderInfo moved = new FolderInfo("S", id, 2, FileInfoFactory.lookupDirectory(top, "new"));
        moved.intern(true);

        FolderInfo governing = FolderInfo.findEnclosingSubFolder(Collections.singletonList(held), top,
            "new/S/file.txt");

        assertNotNull("The permission still encloses the moved subfolder", governing);
        assertEquals(id, governing.getId());
        assertEquals("new/S", governing.locationPath());
    }

    /** A folder nobody mounted is not in the pool - its copy is all there is, and it is not put there. */
    public void testACopyOfAFolderNotInThePoolIsTakenAsItIs() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("Workspace");
        FolderInfo held = new FolderInfo("S", "pfs5926-alone-" + System.nanoTime(), 1,
            FileInfoFactory.lookupDirectory(top, "area"));

        assertSame(held, FolderInfo.findEnclosingSubFolder(Collections.singletonList(held), top, "area/S/file.txt"));
        assertSame("Not put into the pool by the lookup", held, held.latest());
    }
}
