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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

/**
 * PFC-3543: {@code Folder.correctTopAndSubfolderRelation()} promoted a subfolder to a top folder when
 * it found no mounted folder above its local base. For a subfolder with INTERRUPTED permission
 * inheritance that auto-correction is destructive: dropping the parent silently discards the
 * permission barrier, the location and the index entry - and the version-bumped change replicates.
 * An empty ancestor chain is no proof of relocation either: the method runs in the Folder
 * CONSTRUCTOR, i.e. during mounting, before the top folder has to be registered (mount order), and
 * any path bug looks the same - the storage-path check was one and promoted 150 of them.
 * <p>
 * The guard keeps the hierarchy of an interrupted subfolder unchanged and logs SEVERE instead.
 * <p>
 * PFC-3649: the same holds for an inheriting subfolder - mounted before its top folder while a whole tree mounted,
 * 58 of them became top folders, and the promoted copy replaced theirs on every node.
 */
public class InterruptedSubFolderPromotionTest extends ControllerTestCase {

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // The feature is process-wide and off by default in production; enable it for these tests
        // and always clear it again in tearDown so it never leaks.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * The real-world race: an interrupted subfolder is mounted while its top folder is NOT (mount
     * order, or the data was moved by a bug). The constructor runs the correction - it must NOT
     * promote, the hierarchy stays for a human to inspect.
     */
    @Test
    public void testDoesNotPromoteInterruptedSubFolderWithoutMountedTop() {
        FolderInfo unmountedTop = FolderInfoFactory.newTopFolderForTest("topFolder");
        FolderInfo interrupted = newSubFolderInfo(unmountedTop, "secret", false);
        int versionBefore = interrupted.getVersion();

        Folder mounted = joinFolder(interrupted, testFolderBaseDir("orphan-interrupted"),
            SyncProfile.HOST_FILES);

        assertTrue(mounted.getInfo().isSubFolder(),
            "Interrupted subfolder must NOT be promoted to a top folder");
        assertFalse(mounted.getInfo().inheritsPermissions(), "Interruption flag must survive");
        assertEquals(versionBefore, mounted.getInfo().getVersion(),
            "No version bump - nothing may have changed");

        // Repeated correction runs (repository startup path) must stay a no-op as well.
        assertFalse(mounted.correctTopAndSubfolderRelation());
        assertTrue(mounted.getInfo().isSubFolder());
    }

    /** PFC-3649: an inheriting subfolder mounted before its top folder stays a subfolder. */
    @Test
    public void testDoesNotPromoteInheritingSubFolderWithoutMountedTop() {
        FolderInfo unmountedTop = FolderInfoFactory.newTopFolderForTest("topFolder");
        FolderInfo inheriting = newSubFolderInfo(unmountedTop, "plain", true);
        int versionBefore = inheriting.getVersion();

        Folder mounted = joinFolder(inheriting, testFolderBaseDir("orphan-inheriting"), SyncProfile.HOST_FILES);

        assertTrue(mounted.getInfo().isSubFolder(), "Inheriting subfolder must NOT be promoted to a top folder");
        assertEquals(versionBefore, mounted.getInfo().getVersion(), "No version bump - nothing may have changed");
        assertFalse(mounted.correctTopAndSubfolderRelation());
    }

    /** PFC-3649: the same when its top folder goes away while it stays mounted. */
    @Test
    public void testKeepsInheritingSubFolderWhenTopIsRemoved() {
        setupTestFolder(SyncProfile.HOST_FILES);
        Folder topFolder = getFolder();

        FolderInfo inheriting = newSubFolderInfo(topFolder.getInfo(), "plain", true);
        Folder mounted = joinFolder(inheriting, topFolder.getLocalBase().resolve("plain"), SyncProfile.HOST_FILES);
        assertTrue(mounted.getInfo().isSubFolder());
        int versionBefore = mounted.getInfo().getVersion();

        getController().getFolderRepository().removeFolder(topFolder, false);

        assertFalse(mounted.correctTopAndSubfolderRelation(), "Inheriting subfolder must NOT be promoted");
        assertTrue(mounted.getInfo().isSubFolder());
        assertEquals(versionBefore, mounted.getInfo().getVersion());
    }

    /**
     * The counterpart with interruption: same sequence, but the interrupted subfolder must survive
     * the removal of its top folder unchanged.
     */
    @Test
    public void testKeepsInterruptedSubFolderWhenTopIsRemoved() {
        setupTestFolder(SyncProfile.HOST_FILES);
        Folder topFolder = getFolder();

        FolderInfo interrupted = newSubFolderInfo(topFolder.getInfo(), "secret", false);
        Folder mounted = joinFolder(interrupted, topFolder.getLocalBase().resolve("secret"),
            SyncProfile.HOST_FILES);
        assertTrue(mounted.getInfo().isSubFolder());
        int versionBefore = mounted.getInfo().getVersion();

        // Below the mounted top folder there is nothing to correct.
        assertFalse(mounted.correctTopAndSubfolderRelation());

        getController().getFolderRepository().removeFolder(topFolder, false);

        assertFalse(mounted.correctTopAndSubfolderRelation(),
            "Interrupted subfolder must NOT be promoted when its top folder vanishes");
        assertTrue(mounted.getInfo().isSubFolder());
        assertFalse(mounted.getInfo().inheritsPermissions());
        assertEquals(versionBefore, mounted.getInfo().getVersion());
    }

    /** PFC-3649: QA step 2 - its top folder mounted afterwards, the subfolder stays what it was. */
    @Test
    public void testStaysSubFolderOfItsTopMountedAfterIt() {
        FolderInfo topInfo = FolderInfoFactory.newTopFolderForTest("laterTop");
        java.nio.file.Path topBase = testFolderBaseDir("later-top");
        FolderInfo inheriting = newSubFolderInfo(topInfo, "plain", true);
        Folder sub = joinFolder(inheriting, topBase.resolve("plain"), SyncProfile.HOST_FILES);
        int versionBefore = sub.getInfo().getVersion();

        Folder top = joinFolder(topInfo, topBase, SyncProfile.HOST_FILES);

        assertFalse(sub.correctTopAndSubfolderRelation());
        assertTrue(sub.getInfo().isSubFolder());
        assertEquals(top, sub.getTopFolder(), "The subfolder belongs to the top folder mounted after it");
        assertEquals(versionBefore, sub.getInfo().getVersion());
    }

    /**
     * PFC-3649: a subfolder whose own top folder is not mounted, inside the tree of another mounted folder, is neither
     * moved under that folder nor fails the correction.
     */
    @Test
    public void testKeepsOrphanInsideAnotherMountedFolder() {
        setupTestFolder(SyncProfile.HOST_FILES);
        Folder other = getFolder();
        FolderInfo unmountedTop = FolderInfoFactory.newTopFolderForTest("ownTop");
        FolderInfo inheriting = newSubFolderInfo(unmountedTop, "plain", true);

        Folder sub = joinFolder(inheriting, other.getLocalBase().resolve("plain"), SyncProfile.HOST_FILES);
        int versionBefore = sub.getInfo().getVersion();

        assertFalse(sub.correctTopAndSubfolderRelation());
        assertEquals(unmountedTop, sub.getInfo().getTopFolder(), "Stays a subfolder of its own top folder");
        assertEquals(versionBefore, sub.getInfo().getVersion());
    }

    /**
     * A subfolder FolderInfo below the given top folder. The orphan tests pass a top folder that is
     * never mounted.
     */
    private FolderInfo newSubFolderInfo(FolderInfo top, String name, boolean inheritsPermissions) {
        DirectoryInfo location = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(top, name,
            null, 0, null, null, new Date(), 1, null, true, null);
        FolderInfo subFolder = FolderInfoFactory.newFolder(location);
        return inheritsPermissions ? subFolder
            : FolderInfoFactory.changeInheritsPermissions(subFolder, false);
    }

    private java.nio.file.Path testFolderBaseDir(String name) {
        return TestHelper.getTestDir().resolve(getController().getMySelf().getNick() + "/" + name);
    }
}
