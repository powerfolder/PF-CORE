package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PFS-5976: a rename that failed halfway left the location naming the old place - an empty leftover - while the data
 * sits at the configured base. Deriving the base from the location detached the share from its data, and the content
 * of an interrupted share was served as plain content of the top folder.
 */
public class SubFolderBaseKeptTest {

    @TempDir
    Path base;

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

        assertTrue(FolderRepository.keepsBase(share, data, base.resolve("area/old")), "Directories and a .PowerFolder"
            + " are no data");
    }

    @Test
    public void theLocationWinsWhereTheDataIs() throws IOException {
        Path old = Files.createDirectories(base.resolve("area/old"));
        Path data = dataOf(share, "area/new");

        assertFalse(FolderRepository.keepsBase(share, old, data), "The PFS-5935 case: the location is right");
        assertFalse(FolderRepository.keepsBase(share, base.resolve("gone"), data), "...also when the old base is gone");
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

        assertFalse(FolderRepository.keepsBase(share, data, old), "Its own archive");
        assertFalse(FolderRepository.keepsBase(share, nested, base.resolve("area/nestedOld")), "A nested one's");
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

        assertFalse(FolderRepository.keepsBase(share, foreign, base.resolve("area/old")), "Another folder's data");
        assertFalse(FolderRepository.keepsBase(share, anonymous, base.resolve("area/old")), "Data without identity");
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
