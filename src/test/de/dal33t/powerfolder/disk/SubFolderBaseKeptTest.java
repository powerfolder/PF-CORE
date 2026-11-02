package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * PFS-5976: a rename that failed halfway left the location naming the old place - an empty leftover - while the data
 * sits at the configured base. Deriving the base from the location detached the share from its data, and the content
 * of an interrupted share was served as plain content of the top folder.
 */
public class SubFolderBaseKeptTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    private Path base;

    @Before
    public void setUp() {
        base = temp.getRoot().toPath();
    }

    private final FolderInfo share = FolderInfoFactory.newTopFolderForTest("share");

    @Test
    public void theBaseWithTheDataIsKeptWhenTheLocationNamesNothing() throws IOException {
        Path data = dataOf(share, "area/new");

        assertTrue(FolderRepository.keepsBase(share, data, base.resolve("area/old")));
    }

    @Test
    public void anEmptyLeftoverAtTheLocationDoesNotWin() throws IOException {
        Path data = dataOf(share, "area/new");
        Path leftover = Files.createDirectories(base.resolve("area/old/inner"));
        identify(leftover, share);

        assertTrue("Directories and a .PowerFolder"
            + " are no data", FolderRepository.keepsBase(share, data, base.resolve("area/old")));
    }

    @Test
    public void theLocationWinsWhereTheDataIs() throws IOException {
        Path old = Files.createDirectories(base.resolve("area/old"));
        Path data = dataOf(share, "area/new");

        assertFalse("The PFS-5935 case: the location is right", FolderRepository.keepsBase(share, old, data));
        assertFalse("...also when the old base is gone", FolderRepository.keepsBase(share, base.resolve("gone"), data));
    }

    /**
     * An emptied share at its place still has the versions of what was deleted - a copy of it elsewhere is no reason
     * to move. A nested share's archive counts just the same.
     */
    @Test
    public void archivedVersionsAtTheLocationWin() throws IOException {
        Path data = dataOf(share, "area/copy");
        Path old = Files.createDirectories(base.resolve("area/old"));
        identify(old, share);
        Path archive = Files.createDirectories(old.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR).resolve("archive"));
        Files.writeString(archive.resolve("Plan.pdf_K_1"), "deleted");
        Path nested = dataOf(share, "area/nested");
        Path nestedArchive = Files.createDirectories(base.resolve("area/nestedOld/inner")
            .resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR).resolve("archive"));
        Files.writeString(nestedArchive.resolve("Plan.pdf_K_1"), "deleted");

        assertFalse("Its own archive", FolderRepository.keepsBase(share, data, old));
        assertFalse("A nested one's", FolderRepository.keepsBase(share, nested, base.resolve("area/nestedOld")));
    }

    @Test
    public void nothingToKeepWithoutData() {
        assertFalse(FolderRepository.keepsBase(share, base.resolve("gone"), base.resolve("also gone")));
    }

    /** The data of another folder - or of nobody - is no reason to stay: the share would be bound to foreign files. */
    @Test
    public void aBaseWithoutTheIdentityOfTheShareIsNotKept() throws IOException {
        Path foreign = dataOf(FolderInfoFactory.newTopFolderForTest("other"), "area/foreign");
        Path anonymous = Files.createDirectories(base.resolve("area/anonymous"));
        Files.writeString(anonymous.resolve("Plan.pdf"), "data");

        assertFalse("Another folder's data", FolderRepository.keepsBase(share, foreign, base.resolve("area/old")));
        assertFalse("Data without identity", FolderRepository.keepsBase(share, anonymous, base.resolve("area/old")));
    }

    /** Only the system directory itself is no data - a name that merely contains it is. */
    @Test
    public void aNameContainingTheSystemDirectoryIsData() throws IOException {
        Path data = Files.createDirectories(base.resolve("area/new/copy" + Constants.POWERFOLDER_SYSTEM_SUBDIR));
        Files.writeString(data.resolve("Plan.pdf"), "data");
        identify(data.getParent(), share);

        assertTrue(FolderRepository.keepsBase(share, data.getParent(), base.resolve("area/old")));
    }

    private Path dataOf(FolderInfo folder, String relative) throws IOException {
        Path dir = Files.createDirectories(base.resolve(relative + "/inner"));
        Files.writeString(dir.resolve("Plan.pdf"), "data");
        identify(dir.getParent(), folder);
        return dir.getParent();
    }

    /** What {@code Folder.updateInfo} writes and the move carries along with the data. */
    private static void identify(Path dir, FolderInfo folder) throws IOException {
        Path system = Files.createDirectories(dir.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR));
        try (OutputStream out = Files.newOutputStream(system.resolve("FolderInfo"));
             ObjectOutputStream oout = new ObjectOutputStream(out))
        {
            oout.writeObject(folder);
        }
    }
}
