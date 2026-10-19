package de.dal33t.powerfolder.light;


import java.util.Collections;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * PFS-5926: a permission carries a copy of its folder, location included, and the access to a path is resolved from
 * it. After the subfolder moved, an account still holding the old copy - cached for minutes - opened the subfolder
 * empty: its own permission no longer enclosed the path, and the barrier above it answered instead.
 */
public class FolderInfoMovedSubFolderTest {

    @Test
    public void testThePathIsResolvedAtTheCurrentLocation() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("Workspace");
        String id = "pfs5926-moved-" + System.nanoTime();
        // Two copies of the same folder - not through the factory, which interns and would hand back the first
        FolderInfo held = new FolderInfo("S", id, 1, FileInfoFactory.lookupDirectory(top, "old"));
        FolderInfo moved = new FolderInfo("S", id, 2, FileInfoFactory.lookupDirectory(top, "new"));
        moved.intern(true);

        FolderInfo governing = FolderInfo.findEnclosingSubFolder(Collections.singletonList(held), top,
            "new/S/file.txt");

        assertNotNull(governing, "The permission still encloses the moved subfolder");
        assertEquals(id, governing.getId());
        assertEquals("new/S", governing.locationPath());
    }

    /** A folder nobody mounted is not in the pool - its copy is all there is, and it is not put there. */
    @Test
    public void testACopyOfAFolderNotInThePoolIsTakenAsItIs() {
        FolderInfo top = FolderInfoFactory.newTopFolderForTest("Workspace");
        FolderInfo held = new FolderInfo("S", "pfs5926-alone-" + System.nanoTime(), 1,
            FileInfoFactory.lookupDirectory(top, "area"));

        assertSame(held, FolderInfo.findEnclosingSubFolder(Collections.singletonList(held), top, "area/S/file.txt"));
        assertSame(held, held.latest(), "Not put into the pool by the lookup");
    }
}
