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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.InterruptedSubFolderIndex;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryAdapter;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /**
     * PFS-5976: the old names are deleted, not gone. A device that still had the files offered them again - unknown
     * to the server, which took them, and the old directory came back.
     */
    @Test
    public void testMovingLeavesTheOldNamesDeleted() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());
        FileInfo before = getFolder().getFileInfo(SHARED_DIR + "/content.txt");

        subFolder.move("moved");

        FileInfo old = getFolder().getDAO().find(FileInfoFactory.lookupInstance(getFolder().getInfo(),
            SHARED_DIR + "/content.txt"), null);
        assertNotNull(old, "The old name is still known");
        assertTrue(old.isDeleted(), "...as deleted");
        assertTrue(old.getVersion() > before.getVersion(), "...one version further, so peers delete their copy");
        FileInfo moved = getFolder().getFileInfo("moved/content.txt");
        assertFalse(moved.isDeleted(), "The moved one is alive");
        assertTrue(moved.getVersion() > old.getVersion(), "...and newer than the deletion of its old name");
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
     * PFS-5926: the nested share is mounted where its data now is. Its identity followed the move, its base did
     * not - opened by its own id it read the old place, and every file of it failed.
     */
    @Test
    public void testANestedShareIsMountedAtItsNewPlace() throws IOException {
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder inner = shareSubDirectory(SHARED_DIR + "/inner");
        TestHelper.createRandomFile(inner.getLocalBase(), "deep.txt");
        scanFolder(getFolder());

        assertNotNull(outer.move("moved"), "The move must not be refused");

        Folder mounted = getController().getFolderRepository().getFolder(inner.getInfo());
        assertNotNull(mounted, "The nested share stays mounted");
        assertEquals(getFolder().getLocalBase().resolve("moved/inner").toAbsolutePath(),
            mounted.getLocalBase().toAbsolutePath(),
            "...at the new place");
        assertTrue(Files.exists(mounted.getLocalBase().resolve("deep.txt")), "...where its content is");
    }

    /**
     * PFS-5976: a share inside an interrupted one borrows that one's database. Moving the outer share
     * remounted the inheriting one while the fileInfos it reads still named the old place - "FileInfo not in
     * subfolder", the remaining nested shares were never rebased and the database never learned the move.
     */
    @Test
    public void testMovingCarriesSharesNestedBehindABarrier() throws IOException {
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder barrier = shareSubDirectory(SHARED_DIR + "/barrier");
        TestHelper.createRandomFile(barrier.getLocalBase(), "restricted.txt");
        // Several, so that one of them comes before the barrier - the repository hands them out in hash order
        List<Folder> inners = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Folder inner = shareSubDirectory(SHARED_DIR + "/barrier/inner" + i);
            TestHelper.createRandomFile(inner.getLocalBase(), "deep.txt");
            inners.add(inner);
        }
        scanFolder(getFolder());
        barrier.setInheritsPermissions(false);
        scanFolder(getController().getFolderRepository().getFolder(barrier.getInfo()));

        assertNotNull(outer.move("moved"), "The move must not be refused");

        FolderRepository repository = getController().getFolderRepository();
        Path movedBase = getFolder().getLocalBase().resolve("moved");
        Folder movedBarrier = repository.getFolder(barrier.getInfo());
        assertNotNull(movedBarrier, "The interrupted share stays mounted");
        assertEquals(movedBase.resolve("barrier").toAbsolutePath(), movedBarrier.getLocalBase().toAbsolutePath());
        assertNotNull(movedBarrier.getFileInfo("restricted.txt"), "...and reads its fileInfos");
        for (Folder inner : inners) {
            Folder movedInner = repository.getFolder(inner.getInfo());
            assertNotNull(movedInner, inner + ": the share behind the barrier stays mounted");
            assertEquals(movedBase.resolve("barrier/" + inner.getName()).toAbsolutePath(),
                movedInner.getLocalBase().toAbsolutePath(), inner + ": at the new place");
            assertNotNull(movedInner.getFileInfo("deep.txt"), inner + ": still reads its fileInfos");
        }
    }

    /**
     * PFS-5976: the open search index of an interrupted share below holds a file inside the directory - Windows
     * refused to rename it, and the move of the share around it with it.
     */
    @Test
    public void testAnOpenIndexBelowDoesNotBlockTheMove() throws IOException {
        ConfigurationEntry.SEARCH_INDEX_ENABLED.setValue(getController(), true);
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder barrier = shareSubDirectory(SHARED_DIR + "/barrier");
        TestHelper.createRandomFile(barrier.getLocalBase(), "restricted.txt");
        scanFolder(getFolder());
        barrier.setInheritsPermissions(false);
        Folder interrupted = getController().getFolderRepository().getFolder(barrier.getInfo());
        assertTrue(interrupted.getSearchIndexManager().isSearchable(), "Precondition: its index is open");

        assertNotNull(outer.move("moved"), "The move must not be refused");
        Folder moved = getController().getFolderRepository().getFolder(barrier.getInfo());
        assertNotNull(moved.getSearchIndexManager(), "The share below has its index again");
    }

    /**
     * PFS-5976: a nested share that cannot be rebased is unmounted - the others are rebased all the same, its
     * identity names the new place for the server to store, and the barrier of an interrupted one guards the new
     * place instead of the old one that is gone.
     */
    @Test
    public void testANestedShareThatCannotBeRebasedStaysGuardedAtTheNewPlace() throws IOException {
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder failing = shareSubDirectory(SHARED_DIR + "/failing");
        TestHelper.createRandomFile(failing.getLocalBase(), "restricted.txt");
        Folder other = shareSubDirectory(SHARED_DIR + "/other");
        scanFolder(getFolder());
        failing.setInheritsPermissions(false);
        FolderRepository repository = getController().getFolderRepository();
        Folder interrupted = repository.getFolder(failing.getInfo());
        // Without its settings the rebase throws
        FolderSettings.removeEntries(getController().getConfig(), interrupted.getConfigEntryId());

        assertNotNull(outer.move("moved"), "The move goes through");

        Path movedBase = getFolder().getLocalBase().resolve("moved");
        assertEquals(movedBase.resolve("other").toAbsolutePath(),
            repository.getFolder(other.getInfo()).getLocalBase().toAbsolutePath(), "The others are rebased");
        assertNull(repository.getFolder(failing.getInfo()), "The failing one is unmounted");
        assertEquals("moved/failing", interrupted.getInfo().locationPath(), "...its identity names the new place");
        assertTrue(getFolder().isInInterruptedSubFolder(movedBase.resolve("failing/restricted.txt")),
            "...and its barrier guards it there");
    }

    /**
     * PFS-5976: a share whose location names a place without data while its configured base holds the data and its
     * identity - a rename that failed halfway. Mounted, it takes the place of its data as its location: the barrier
     * and the base agree again, and the next mount does not ask once more.
     */
    @Test
    public void testAShareMountsWhereItsDataIs() throws IOException {
        getController().getMySelf().setServer(true);
        Folder share = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(share.getLocalBase(), "kept.txt");
        FolderRepository repository = getController().getFolderRepository();
        FolderSettings settings = FolderSettings.load(getController(), share.getConfigEntryId());
        FolderInfo info = share.getInfo();
        Path oldBase = share.getLocalBase();
        repository.removeFolder(share, false);
        Path data = getFolder().getLocalBase().resolve(PARENT_DIR + "/renamed");
        Files.move(oldBase, data);
        List<FolderRepositoryEvent> created = new CopyOnWriteArrayList<>();
        repository.addFolderRepositoryListener(new FolderRepositoryAdapter() {
            @Override
            public void folderCreated(FolderRepositoryEvent e) {
                created.add(e);
            }

            @Override
            public boolean fireInEventDispatchThread() {
                return false;
            }
        });

        Folder mounted = repository.createFolder(info, settings.changeBaseDir(data));

        assertEquals(PARENT_DIR + "/renamed", mounted.getInfo().locationPath(), "The location follows the data");
        assertEquals(data.toAbsolutePath(), mounted.getLocalBase().toAbsolutePath(), "...the base stays with it");
        assertFalse(Files.exists(oldBase), "No empty directory at the old place");
        assertEquals(1, created.size(), "Mounted once");
        assertEquals(SHARED_DIR, created.get(0).getOldLocationPath(), "The server is told where the share was");

        repository.removeFolder(mounted, false);
        repository.createFolder(mounted.getInfo(), settings.changeBaseDir(data));
        assertNull(created.get(1).getOldLocationPath(), "...and only when it moved");
    }

    /**
     * PFS-5976: a rebase that fails after the unmount kept nothing of the share - without its config entry the static
     * mounter never mounted it again. It stays configured at the new place.
     */
    @Test
    public void testARebaseThatFailsKeepsTheShareConfiguredAtTheNewPlace() throws IOException {
        Folder share = shareSubDirectory(SHARED_DIR);
        FolderRepository repository = getController().getFolderRepository();
        Path newBase = Files.createDirectories(getFolder().getLocalBase().resolve(PARENT_DIR + "/elsewhere"));
        // Refuses every mount below the top folder
        ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY.setValue(getController(), true);
        try {
            assertThrows(IllegalStateException.class, () -> repository.rebaseLocalFolder(share, newBase));
        } finally {
            ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY.setValue(getController(), false);
        }

        FolderSettings kept = FolderSettings.load(getController(), share.getConfigEntryId());
        assertNotNull(kept, "The share stays configured");
        assertEquals(newBase.toAbsolutePath(), kept.getLocalBaseDir().toAbsolutePath(), "...at the new place");
    }

    /** PFS-5976: a client follows the location of its server - only the server can store a moved one. */
    @Test
    public void testAClientKeepsTheLocationOfItsServer() throws IOException {
        Folder share = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(share.getLocalBase(), "kept.txt");
        FolderRepository repository = getController().getFolderRepository();
        FolderSettings settings = FolderSettings.load(getController(), share.getConfigEntryId());
        FolderInfo info = share.getInfo();
        repository.removeFolder(share, false);
        Path data = getFolder().getLocalBase().resolve(PARENT_DIR + "/renamed");
        Files.move(share.getLocalBase(), data);

        Folder mounted = repository.createFolder(info, settings.changeBaseDir(data));

        assertEquals(info.locationPath(), mounted.getInfo().locationPath(), "The location stays");
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
