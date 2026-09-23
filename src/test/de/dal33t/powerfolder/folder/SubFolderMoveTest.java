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
package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.InterruptedSubFolderIndex;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-5850 / PFS-5528: what happens today when a shared subfolder is moved, renamed or copied.
 * <p>
 * A shared subfolder is a folder of its own whose identity IS its location inside the top folder
 * ({@link FolderInfo#locationPath()}), and an interrupted one owns its content database on top of
 * that. The web offers copy, cut/paste, rename and drag&drop on its row like on any other directory,
 * and the execution behind those actions knows nothing about it. These tests pin what that costs -
 * they are written to FAIL until the move operation carries the folder along, and each one names the behaviour it
 * expects instead.
 * <p>
 * One controller, no network: everything here happens inside one repository.
 */
public class SubFolderMoveTest extends ControllerTestCase {

    private static final String PARENT_DIR = "structure";
    private static final String SHARED_DIR = PARENT_DIR + "/shared";

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Process-wide and off by default in production - the interrupted cases need it.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        setupTestFolder(SyncProfile.HOST_FILES);
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * Renaming the directory of a share on disk - what {@code FilesAPIServlet.rename} does - leaves the
     * registration pointing at the old path. The next mount derives the base dir from that stale
     * location and hands the folder an empty directory next to its own data.
     */
    @Test
    public void testRenamingCarriesTheShare() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());
        Path oldDir = subFolder.getLocalBase();
        Path newDir = oldDir.resolveSibling("renamed");

        subFolder = subFolder.move(PARENT_DIR + "/renamed");

        assertNotNull(subFolder, "The rename must not be refused");
        assertTrue(Files.exists(newDir.resolve("content.txt")), "The data is at the new path");
        assertFalse(Files.exists(oldDir), "Nothing is left at the old path");
        assertEquals(PARENT_DIR + "/renamed", subFolder.getInfo().locationPath(), "The share must follow the directory it stands for");
        // correctSubFolderBaseDir derives the base from the location on every mount - so the location
        // is what decides whether the folder finds its data again after a restart.
        Path derivedBase = getFolder().getLocalBase().resolve(subFolder.getInfo().locationPath());
        assertEquals(newDir, derivedBase, "The derived base must be the directory that holds the data");
    }

    /**
     * Moving the directory the way the web does it - copy to the target, then delete the source -
     * dissolves the share on the way: {@code unshareSubFoldersIn} removes every share at or below the
     * deleted directory, and the data reappears at the target as an ordinary directory.
     */
    @Test
    public void testMovingCarriesTheShare() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());
        Path target = getFolder().getLocalBase().resolve("moved");

        subFolder = subFolder.move("moved");

        assertNotNull(subFolder, "The move must not be refused");
        assertTrue(Files.exists(target.resolve("content.txt")), "The content is at the target");
        assertNotNull(getController().getFolderRepository().getFolder(subFolder.getInfo()), "The share must survive the move - it is a folder, not a directory");
        assertEquals("moved", subFolder.getInfo().locationPath(), "...and it must name its new location");
    }

    /**
     * A move unmounts the folder, moves the directory and mounts it again. In that window two
     * self-healers would "repair" the very disagreement the move is making: the hierarchy correction
     * derives the parent from the filesystem, and a scan of the folder above sees the old directory
     * gone and dissolves the share. Both have to recognise a move in flight - and let go of it again
     * when it is over.
     */
    @Test
    public void testAMoveInFlightIsProtectedFromTheSelfHealers() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        FolderRepository repository = getController().getFolderRepository();
        assertFalse(repository.isMoving(subFolder.getInfo()), "Nothing is moving yet");

        Folder moved = subFolder.move("moved");
        assertNotNull(moved, "The move must not be refused");

        assertFalse(repository.isMoving(moved.getInfo()), "The mark is gone once the move is done");
        // The correction runs in the constructor of the mounted instance - with the mark set it must
        // have kept its hands off, so the folder is still a subfolder at its new place.
        assertTrue(moved.getInfo().isSubFolder(), "The moved folder is still a subfolder");
        assertEquals("moved", moved.getInfo().locationPath());
        assertSame(moved, repository.findSubFolder(FileInfoFactory.lookupDirectory(getFolder().getInfo(), "moved")), "...and the share is still registered");
    }

    /**
     * The rows of the moved subtree keep their identity: same OID, one version further. A move that let
     * the scanner rediscover the content instead would hand out new OIDs - and every public link into
     * the subtree points at one, so they would all die on a rename.
     */
    @Test
    public void testMovingKeepsTheIdentityOfTheRows() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());

        FileInfo before = getFolder().getFileInfo(SHARED_DIR + "/content.txt");
        assertNotNull(before, "Precondition: the row is known");
        String oidBefore = before.getOID();

        subFolder.move("moved");
        scanFolder(getFolder());

        FileInfo after = getFolder().getFileInfo("moved/content.txt");
        assertNotNull(after, "The row must be at the new location");
        assertEquals(oidBefore, after.getOID(), "...and it must be the same row");
        assertTrue(after.getVersion() > before.getVersion(), "...one version further, so peers take it");
    }

    /** A move onto an existing directory is refused, and refusing must leave everything as it was. */
    @Test
    public void testMoveOntoAnExistingDirectoryChangesNothing() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        Files.createDirectories(getFolder().getLocalBase().resolve("occupied"));
        scanFolder(getFolder());

        assertNull(subFolder.move("occupied"), "The move must be refused");

        assertEquals(SHARED_DIR, subFolder.getInfo().locationPath(), "The share keeps its location");
        assertSame(subFolder, getController().getFolderRepository().getFolder(subFolder.getInfo()), "...and stays the folder it was");
        assertTrue(Files.exists(subFolder.getLocalBase()), "...with its data where it was");
    }

    /**
     * The subfolders below a moved one travel with it. Their location is stored in TOP-folder
     * coordinates, so re-pointing only the moved folder would leave every nested one naming a path
     * that is gone - and the base derived from it an empty directory beside the real data.
     */
    @Test
    public void testMovingCarriesNestedShares() throws IOException {
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder inner = shareSubDirectory(SHARED_DIR + "/inner");
        TestHelper.createRandomFile(inner.getLocalBase(), "deep.txt");
        scanFolder(getFolder());

        outer = outer.move("moved");
        assertNotNull(outer, "The move must not be refused");

        assertEquals("moved/inner", inner.getInfo().locationPath(), "The nested share must follow its parent");
        assertTrue(Files.exists(getFolder().getLocalBase().resolve("moved/inner/deep.txt")), "...and its content moved with it");
    }

    /**
     * An interrupted subfolder owns its rows, so its directory row leaves the top folder (PFC-3575) -
     * it cannot be addressed through a row of its parent any more. What must stay is the resolution by
     * LOCATION: that is how a file operation names the subfolder it is about to move or copy.
     */
    @Test
    public void testInterruptedSubFolderStaysResolvableByItsLocation() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo location = FileInfoFactory.lookupDirectory(getFolder().getInfo(), SHARED_DIR);
        assertNull(getFolder().getDAO().find(location, null), "The directory row leaves the top folder with the content (PFC-3575)");
        assertSame(subFolder, getController().getFolderRepository().findSubFolder(location), "...but the subfolder must still be resolvable by its location");
    }

    /**
     * AK-8 (BVL spec 5): copying a directory that holds a subfolder with interrupted inheritance must
     * carry the interruption - the copy is decoupled from the target parent and takes over the explicit
     * permissions of the template. Today the copy is bytes and nothing else.
     */
    @Test
    public void testCopyingCarriesTheInterruption() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        subFolder.setInheritsPermissions(false);

        Map<FolderInfo, Folder> created = getFolder().copyTree(PARENT_DIR, "copy-of-structure");

        assertNotNull(created, "The copy must not be refused");
        assertEquals(1, created.size(), "AK-8: the copy of the interrupted subfolder must be a subfolder of its own");
        Folder copy = created.get(subFolder.getInfo());
        assertNotNull(copy, "...keyed by the template it was copied from");
        assertEquals("copy-of-structure/shared", copy.getInfo().locationPath(), "...at the copied location");
        assertFalse(copy.getInfo().inheritsPermissions(), "...with the interruption of the template");
        assertTrue(Files.exists(copy.getLocalBase().resolve("secret.txt")), "...and the content of the template");
        assertFalse(copy.getId().equals(subFolder.getId()), "The template keeps its own identity");
    }

    /**
     * The copy is an unfiltered walk of the file tree, so the system directory of a share travels with
     * it - a second folder database inside a directory that is not a folder.
     */
    @Test
    public void testCopyingCarriesTheSystemDirectory() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo sourceDir = (DirectoryInfo) getFolder().getFileInfo(PARENT_DIR);
        Path target = getFolder().getLocalBase().resolve("copy-of-structure");
        assertTrue(getFolder().copy(sourceDir, target));

        assertFalse(Files.exists(target.resolve("shared").resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR)), "The system directory of the nested share must not be copied along");
    }

    /**
     * Copying a directory that CONTAINS an interrupted subfolder copies everything behind the barrier,
     * whatever the caller may read. The barrier is a permission boundary; the copy walks straight
     * through it.
     */
    @Test
    public void testCopyingReadsPastABarrier() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo parentDir = (DirectoryInfo) getFolder().getFileInfo(PARENT_DIR);
        Path target = getFolder().getLocalBase().resolve("copy-of-structure");
        assertTrue(getFolder().copy(parentDir, target));

        assertFalse(Files.exists(target.resolve("shared/secret.txt")), "Content behind the barrier must not travel into the copy unchecked");
    }

    /**
     * The barrier index keeps an ABSOLUTE path per interrupted subfolder. After the directory moved,
     * that path names a directory that no longer exists, while the real data sits unguarded.
     */
    @Test
    public void testMovedInterruptedSubFolderTakesItsBarrierAlong() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        subFolder = subFolder.move("moved");
        assertNotNull(subFolder, "The move must not be refused");

        FolderInfo[] barriers = InterruptedSubFolderIndex.barriersOf(getFolder().getInfo());
        assertEquals(1, barriers.length, "Precondition: the subfolder is still a barrier");
        assertEquals("moved", barriers[0].locationPath(), "The barrier must follow the data, not stay at the old location");
    }

    // Helpers ****************************************************************

    /** Creates the directory, scans it in and turns it into a shared subfolder. */
    private Folder shareSubDirectory(String relativeName) throws IOException {
        Files.createDirectories(getFolder().getPhysicalDir().resolve(relativeName));
        scanFolder(getFolder());
        DirectoryInfo subDirInfo = (DirectoryInfo) getFolder().getFileInfo(relativeName);
        assertNotNull(subDirInfo, relativeName + ": not scanned in");
        Folder subFolder = getFolder().share(subDirInfo);
        assertNotNull(subFolder, relativeName + ": not shared");
        return subFolder;
    }
}
