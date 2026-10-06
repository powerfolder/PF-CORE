package de.dal33t.powerfolder.light;


import java.util.Date;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * PFS-5927: a location is mapped into the interrupted subfolder that stores it, whichever folder it was addressed
 * through. A WebDAV drive mapped onto a shared subfolder addresses that subfolder, and every entry of a listing
 * threw "FileInfo not in subfolder".
 */
public class FileInfoMapIntoSubFolderTest {

    private FolderInfo top;
    private FolderInfo shared;
    private FolderInfo nested;

    @BeforeEach
    protected void setUp() throws Exception {
        top = FolderInfoFactory.newTopFolderForTest("TopFolder");
        shared = newSubFolder(top, "area/shared");
        nested = newSubFolder(top, "area/shared/inner");
    }

    /** The customer's case: the drive is the subfolder itself, and so is the innermost one around the path. */
    @Test
    public void testALocationAddressedThroughItsOwnSubFolderStaysAsItIs() {
        FileInfo addressed = FileInfoFactory.lookupInstance(shared, "docs/report.pdf");

        FileInfo mapped = FileInfoFactory.mapInto(addressed, shared);

        assertSame(addressed, mapped);
    }

    @Test
    public void testALocationOfANestedSubFolderMapsIntoIt() {
        FileInfo mapped = FileInfoFactory.mapInto(FileInfoFactory.lookupInstance(shared, "inner/report.pdf"),
            nested);

        assertEquals(nested, mapped.getFolderInfo());
        assertEquals("report.pdf", mapped.getRelativeName());
        assertFalse(mapped instanceof DirectoryInfo);
    }

    @Test
    public void testTheNestedSubFolderItselfMapsToItsBaseDirectory() {
        FileInfo mapped = FileInfoFactory.mapInto(FileInfoFactory.lookupDirectory(shared, "inner"), nested);

        assertEquals(nested, mapped.getFolderInfo());
        assertEquals("", mapped.getRelativeName());
        assertTrue(mapped instanceof DirectoryInfo);
    }

    /** Through the top folder, as before. */
    @Test
    public void testALocationAddressedThroughTheTopFolderMapsAsBefore() {
        FileInfo mapped = FileInfoFactory.mapInto(FileInfoFactory.lookupInstance(top, "area/shared/docs/a.txt"),
            shared);

        assertEquals(shared, mapped.getFolderInfo());
        assertEquals("docs/a.txt", mapped.getRelativeName());
    }

    private static FolderInfo newSubFolder(FolderInfo top, String path) {
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top, path,
            null, 0, null, null, new Date(), 1, null, true, null);
        return FolderInfoFactory.newFolder(location);
    }
}
