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
 * PFC-3543: {@code Folder.correctTopAndSubfolderRelation()} promotes a subfolder to a top folder when
 * it finds no mounted folder above its local base. For a subfolder with INTERRUPTED permission
 * inheritance that auto-correction is destructive: dropping the parent silently discards the
 * permission barrier, the location and the index entry - and the version-bumped change replicates.
 * An empty ancestor chain is no proof of relocation either: the method runs in the Folder
 * CONSTRUCTOR, i.e. during mounting, before the top folder has to be registered (mount order), and
 * any path bug looks the same - the storage-path check was one and promoted 150 of them.
 * <p>
 * The guard keeps the hierarchy of an interrupted subfolder unchanged and logs SEVERE instead;
 * inheriting subfolders keep the promotion behavior.
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

    /**
     * Unchanged behavior for an INHERITING subfolder: without a mounted folder above it, the
     * correction still promotes it to a top folder. Mounted below its top first, then the top is
     * removed and the correction re-run - the same sequence the repository startup path takes.
     */
    @Test
    public void testStillPromotesInheritingSubFolderWithoutMountedTop() {
        setupTestFolder(SyncProfile.HOST_FILES);
        Folder topFolder = getFolder();

        FolderInfo inheriting = newSubFolderInfo(topFolder.getInfo(), "plain", true);
        Folder mounted = joinFolder(inheriting, topFolder.getLocalBase().resolve("plain"),
            SyncProfile.HOST_FILES);
        assertTrue(mounted.getInfo().isSubFolder());
        int versionBefore = mounted.getInfo().getVersion();

        getController().getFolderRepository().removeFolder(topFolder, false);

        assertTrue(mounted.correctTopAndSubfolderRelation(),
            "Inheriting subfolder without a mounted folder above is still promoted");
        assertTrue(mounted.getInfo().isTopFolder());
        assertEquals(versionBefore + 1, mounted.getInfo().getVersion(),
            "Promotion is a version-bumped change");
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
