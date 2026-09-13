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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Process-wide and off by default in production - the interrupted cases need it.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        setupTestFolder(SyncProfile.HOST_FILES);
    }

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
    public void testRenamingCarriesTheShare() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());
        Path oldDir = subFolder.getLocalBase();
        Path newDir = oldDir.resolveSibling("renamed");

        subFolder = subFolder.move(PARENT_DIR + "/renamed");

        assertNotNull("The rename must not be refused", subFolder);
        assertTrue("The data is at the new path", Files.exists(newDir.resolve("content.txt")));
        assertFalse("Nothing is left at the old path", Files.exists(oldDir));
        assertEquals("The share must follow the directory it stands for",
            PARENT_DIR + "/renamed", subFolder.getInfo().locationPath());
        // correctSubFolderBaseDir derives the base from the location on every mount - so the location
        // is what decides whether the folder finds its data again after a restart.
        Path derivedBase = getFolder().getLocalBase().resolve(subFolder.getInfo().locationPath());
        assertEquals("The derived base must be the directory that holds the data", newDir, derivedBase);
    }

    /**
     * Moving the directory the way the web does it - copy to the target, then delete the source -
     * dissolves the share on the way: {@code unshareSubFoldersIn} removes every share at or below the
     * deleted directory, and the data reappears at the target as an ordinary directory.
     */
    public void testMovingCarriesTheShare() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());
        Path target = getFolder().getLocalBase().resolve("moved");

        subFolder = subFolder.move("moved");

        assertNotNull("The move must not be refused", subFolder);
        assertTrue("The content is at the target", Files.exists(target.resolve("content.txt")));
        assertNotNull("The share must survive the move - it is a folder, not a directory",
            getController().getFolderRepository().getFolder(subFolder.getInfo()));
        assertEquals("...and it must name its new location",
            "moved", subFolder.getInfo().locationPath());
    }

    /**
     * A move unmounts the folder, moves the directory and mounts it again. In that window two
     * self-healers would "repair" the very disagreement the move is making: the hierarchy correction
     * derives the parent from the filesystem, and a scan of the folder above sees the old directory
     * gone and dissolves the share. Both have to recognise a move in flight - and let go of it again
     * when it is over.
     */
    public void testAMoveInFlightIsProtectedFromTheSelfHealers() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        FolderRepository repository = getController().getFolderRepository();
        assertFalse("Nothing is moving yet", repository.isMoving(subFolder.getInfo()));

        Folder moved = subFolder.move("moved");
        assertNotNull("The move must not be refused", moved);

        assertFalse("The mark is gone once the move is done",
            repository.isMoving(moved.getInfo()));
        // The correction runs in the constructor of the mounted instance - with the mark set it must
        // have kept its hands off, so the folder is still a subfolder at its new place.
        assertTrue("The moved folder is still a subfolder", moved.getInfo().isSubFolder());
        assertEquals("moved", moved.getInfo().locationPath());
        assertSame("...and the share is still registered", moved,
            repository.findSubFolder(FileInfoFactory.lookupDirectory(getFolder().getInfo(), "moved")));
    }

    /**
     * The rows of the moved subtree keep their identity: same OID, one version further. A move that let
     * the scanner rediscover the content instead would hand out new OIDs - and every public link into
     * the subtree points at one, so they would all die on a rename.
     */
    public void testMovingKeepsTheIdentityOfTheRows() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "content.txt");
        scanFolder(getFolder());

        FileInfo before = getFolder().getFileInfo(SHARED_DIR + "/content.txt");
        assertNotNull("Precondition: the row is known", before);
        String oidBefore = before.getOID();

        subFolder.move("moved");
        scanFolder(getFolder());

        FileInfo after = getFolder().getFileInfo("moved/content.txt");
        assertNotNull("The row must be at the new location", after);
        assertEquals("...and it must be the same row", oidBefore, after.getOID());
        assertTrue("...one version further, so peers take it", after.getVersion() > before.getVersion());
    }

    /** A move onto an existing directory is refused, and refusing must leave everything as it was. */
    public void testMoveOntoAnExistingDirectoryChangesNothing() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        Files.createDirectories(getFolder().getLocalBase().resolve("occupied"));
        scanFolder(getFolder());

        assertNull("The move must be refused", subFolder.move("occupied"));

        assertEquals("The share keeps its location", SHARED_DIR, subFolder.getInfo().locationPath());
        assertSame("...and stays the folder it was", subFolder,
            getController().getFolderRepository().getFolder(subFolder.getInfo()));
        assertTrue("...with its data where it was", Files.exists(subFolder.getLocalBase()));
    }

    /**
     * The subfolders below a moved one travel with it. Their location is stored in TOP-folder
     * coordinates, so re-pointing only the moved folder would leave every nested one naming a path
     * that is gone - and the base derived from it an empty directory beside the real data.
     */
    public void testMovingCarriesNestedShares() throws IOException {
        Folder outer = shareSubDirectory(SHARED_DIR);
        Folder inner = shareSubDirectory(SHARED_DIR + "/inner");
        TestHelper.createRandomFile(inner.getLocalBase(), "deep.txt");
        scanFolder(getFolder());

        outer = outer.move("moved");
        assertNotNull("The move must not be refused", outer);

        assertEquals("The nested share must follow its parent", "moved/inner", inner.getInfo().locationPath());
        assertTrue("...and its content moved with it",
            Files.exists(getFolder().getLocalBase().resolve("moved/inner/deep.txt")));
    }

    /**
     * An interrupted subfolder owns its rows, so its directory row leaves the top folder (PFC-3575) -
     * it cannot be addressed through a row of its parent any more. What must stay is the resolution by
     * LOCATION: that is how a file operation names the subfolder it is about to move or copy.
     */
    public void testInterruptedSubFolderStaysResolvableByItsLocation() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo location = FileInfoFactory.lookupDirectory(getFolder().getInfo(), SHARED_DIR);
        assertNull("The directory row leaves the top folder with the content (PFC-3575)",
            getFolder().getDAO().find(location, null));
        assertSame("...but the subfolder must still be resolvable by its location", subFolder,
            getController().getFolderRepository().findSubFolder(location));
    }

    /**
     * AK-8 (BVL spec 5): copying a directory that holds a subfolder with interrupted inheritance must
     * carry the interruption - the copy is decoupled from the target parent and takes over the explicit
     * permissions of the template. Today the copy is bytes and nothing else.
     */
    public void testCopyingCarriesTheInterruption() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        subFolder.setInheritsPermissions(false);

        Map<FolderInfo, Folder> created = getFolder().copyTree(PARENT_DIR, "copy-of-structure");

        assertNotNull("The copy must not be refused", created);
        assertEquals("AK-8: the copy of the interrupted subfolder must be a subfolder of its own", 1,
            created.size());
        Folder copy = created.get(subFolder.getInfo());
        assertNotNull("...keyed by the template it was copied from", copy);
        assertEquals("...at the copied location", "copy-of-structure/shared",
            copy.getInfo().locationPath());
        assertFalse("...with the interruption of the template",
            copy.getInfo().inheritsPermissions());
        assertTrue("...and the content of the template",
            Files.exists(copy.getLocalBase().resolve("secret.txt")));
        assertFalse("The template keeps its own identity", copy.getId().equals(subFolder.getId()));
    }

    /**
     * The copy is an unfiltered walk of the file tree, so the system directory of a share travels with
     * it - a second folder database inside a directory that is not a folder.
     */
    public void testCopyingCarriesTheSystemDirectory() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo sourceDir = (DirectoryInfo) getFolder().getFileInfo(PARENT_DIR);
        Path target = getFolder().getLocalBase().resolve("copy-of-structure");
        assertTrue(getFolder().copy(sourceDir, target));

        assertFalse("The system directory of the nested share must not be copied along",
            Files.exists(target.resolve("shared").resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR)));
    }

    /**
     * Copying a directory that CONTAINS an interrupted subfolder copies everything behind the barrier,
     * whatever the caller may read. The barrier is a permission boundary; the copy walks straight
     * through it.
     */
    public void testCopyingReadsPastABarrier() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        DirectoryInfo parentDir = (DirectoryInfo) getFolder().getFileInfo(PARENT_DIR);
        Path target = getFolder().getLocalBase().resolve("copy-of-structure");
        assertTrue(getFolder().copy(parentDir, target));

        assertFalse("Content behind the barrier must not travel into the copy unchecked",
            Files.exists(target.resolve("shared/secret.txt")));
    }

    /**
     * The barrier index keeps an ABSOLUTE path per interrupted subfolder. After the directory moved,
     * that path names a directory that no longer exists, while the real data sits unguarded.
     */
    public void testMovedInterruptedSubFolderTakesItsBarrierAlong() throws IOException {
        Folder subFolder = shareSubDirectory(SHARED_DIR);
        TestHelper.createRandomFile(subFolder.getLocalBase(), "secret.txt");
        scanFolder(getFolder());
        // The interruption moves the rows into the subfolder's own database.
        subFolder.setInheritsPermissions(false);

        subFolder = subFolder.move("moved");
        assertNotNull("The move must not be refused", subFolder);

        FolderInfo[] barriers = InterruptedSubFolderIndex.barriersOf(getFolder().getInfo());
        assertEquals("Precondition: the subfolder is still a barrier", 1, barriers.length);
        assertEquals("The barrier must follow the data, not stay at the old location",
            "moved", barriers[0].locationPath());
    }

    // Helpers ****************************************************************

    /** Creates the directory, scans it in and turns it into a shared subfolder. */
    private Folder shareSubDirectory(String relativeName) throws IOException {
        Files.createDirectories(getFolder().getPhysicalDir().resolve(relativeName));
        scanFolder(getFolder());
        DirectoryInfo subDirInfo = (DirectoryInfo) getFolder().getFileInfo(relativeName);
        assertNotNull(relativeName + ": not scanned in", subDirInfo);
        Folder subFolder = getFolder().share(subDirInfo);
        assertNotNull(relativeName + ": not shared", subFolder);
        return subFolder;
    }
}
