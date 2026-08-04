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
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.InterruptedSubFolderIndex;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PFS-5510 / PFC-3543: A subfolder with interrupted permission inheritance is a BARRIER for the
 * effective permission resolution on {@link Account} and {@link Group} - a top-folder permission must
 * not reach into the decoupled subtree.
 * <p>
 * The interesting case is a barrier the evaluated account holds NO permission on: such a subfolder is
 * invisible in the account's own permission structures, so it cannot be found among its candidates.
 * The barriers therefore come from the process-wide
 * {@link de.dal33t.powerfolder.disk.InterruptedSubFolderIndex}, which the
 * {@link de.dal33t.powerfolder.disk.FolderRepository} keeps up to date - hence a real controller with
 * real mounted folders here instead of a plain unit test.
 * <p>
 * The purely structural resolution is covered by {@code FolderInfoTest#findEnclosingSubFolder*},
 * the database migration that goes with an interruption by
 * {@code SubFolderInterruptInheritanceTest}.
 */
public class InterruptedSubFolderAccessTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // The feature is process-wide and off by default in production; enable it for these tests and
        // always clear it again in tearDown so it never leaks into other tests.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * The core regression: an account with a top-folder permission and NO permission on the
     * interrupted subfolder must lose all access inside it - and keep it everywhere else.
     */
    public void testEffectiveAccessStopsAtInterruptedSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderInfo topInfo = topFolder.getInfo();

        /*
         * /top
         * ├── secret               (shared subfolder, interrupted below - NO permission for hans)
         * │   └── inside
         * ├── secretly             (sibling starting with the barrier's name)
         * └── open
         */
        Path root = topFolder.getPhysicalDir();
        Files.createDirectories(root.resolve("secret/inside"));
        Files.createDirectories(root.resolve("secretly"));
        Files.createDirectories(root.resolve("open"));
        TestHelper.scanFolder(topFolder);

        Folder secret = topFolder.share((DirectoryInfo) topFolder.getFileInfo("secret"));
        assertNotNull(secret);

        Account hans = new Account();
        hans.grant(FolderPermission.readWrite(topInfo));

        // While the subfolder still inherits, the top-folder permission applies inside it.
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret/inside/x.txt"));

        // --- Interrupt: the subtree is decoupled, the top permission must not reach into it ---
        secret.setInheritsPermissions(false);

        assertEquals(AccessMode.NO_ACCESS, hans.getAllowedAccess(topInfo, "secret"));
        assertEquals(AccessMode.NO_ACCESS, hans.getAllowedAccess(topInfo, "secret/inside/x.txt"));
        assertFalse(hans.hasReadPermissions(topInfo, "secret/inside/x.txt"));
        assertFalse(hans.hasWritePermissions(topInfo, "secret/inside/x.txt"));

        // Everything next to the barrier is unaffected - including the sibling whose name starts with
        // the barrier's name (matching is segment-exact).
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, ""));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "open/x.txt"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secretly/x.txt"));

        // --- Restore: the barrier is gone, the top permission applies again ---
        secret.setInheritsPermissions(true);
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret/inside/x.txt"));
    }

    /**
     * A permission granted DIRECTLY on the interrupted subfolder - or on a shared subfolder BELOW it -
     * still counts; only the INHERITED permission stops at the barrier.
     */
    public void testEffectiveAccessGrantedAtOrBelowInterruptedSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderInfo topInfo = topFolder.getInfo();

        /*
         * /top
         * └── secret               (shared, interrupted -> BARRIER)
         *     └── shared           (shared subfolder BELOW the barrier)
         *         └── deep
         */
        Path root = topFolder.getPhysicalDir();
        Files.createDirectories(root.resolve("secret/shared/deep"));
        TestHelper.scanFolder(topFolder);

        Folder secret = topFolder.share((DirectoryInfo) topFolder.getFileInfo("secret"));
        Folder secretShared = topFolder.share((DirectoryInfo) topFolder.getFileInfo("secret/shared"));
        assertNotNull(secret);
        assertNotNull(secretShared);
        secret.setInheritsPermissions(false);

        // A permission directly ON the barrier keeps working - it is not an inherited one.
        Account direct = new Account();
        direct.grant(FolderPermission.read(secret.getInfo()));
        assertEquals(AccessMode.READ, direct.getAllowedAccess(topInfo, "secret"));
        assertEquals(AccessMode.READ, direct.getAllowedAccess(topInfo, "secret/inside/x.txt"));

        // A permission on a subfolder BELOW the barrier wins there, while the barrier still blocks the
        // top-folder permission above it: the innermost match decides.
        Group writers = new Group("Writers");
        writers.grant(FolderPermission.readWrite(secretShared.getInfo()));
        Account hans = new Account();
        hans.grant(FolderPermission.readWrite(topInfo));
        hans.addGroup(writers);

        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret/shared"));
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret/shared/deep/x.txt"));
        assertEquals(AccessMode.NO_ACCESS, hans.getAllowedAccess(topInfo, "secret"));
        assertEquals(AccessMode.NO_ACCESS, hans.getAllowedAccess(topInfo, "secret/other/x.txt"));

        // The group resolves through the same primitive, so it must agree.
        assertEquals(AccessMode.READ_WRITE, writers.getAllowedAccess(topInfo, "secret/shared/deep/x.txt"));
        assertEquals(AccessMode.NO_ACCESS, writers.getAllowedAccess(topInfo, "secret/other/x.txt"));

        // Addressing the barrier itself as the folder must resolve identically.
        assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(secret.getInfo(), "shared/deep/x.txt"));
        assertEquals(AccessMode.NO_ACCESS, hans.getAllowedAccess(secret.getInfo(), "other/x.txt"));
    }

    /**
     * The barrier lookup must not create anything: {@code getAllowedAccess(folder, path)} sits on the web
     * hot path (every file listing, upload and editor open), so it may cost no more than a volatile read
     * of a pre-merged snapshot. Object IDENTITY across calls is the direct evidence: if anything were
     * built per call - a list, an iterator, an empty stand-in - the returned snapshot could not be the
     * very same instance every time.
     */
    public void testBarrierLookupCreatesNothing() throws IOException {
        Folder topFolder = getFolderAtBart();

        Files.createDirectories(topFolder.getPhysicalDir().resolve("secret"));
        TestHelper.scanFolder(topFolder);
        Folder secret = topFolder.share((DirectoryInfo) topFolder.getFileInfo("secret"));

        // Nothing interrupted -> the shared empty snapshot, identical on every call.
        assertEquals(0, InterruptedSubFolderIndex.barriers().length);
        assertSame(InterruptedSubFolderIndex.barriers(), InterruptedSubFolderIndex.barriers());

        // With a barrier the snapshot is shared as well - it is rebuilt on structural changes only.
        secret.setInheritsPermissions(false);
        assertEquals(1, InterruptedSubFolderIndex.barriers().length);
        assertSame(InterruptedSubFolderIndex.barriers(), InterruptedSubFolderIndex.barriers());

        // Restoring inheritance empties it again, back to the shared constant.
        secret.setInheritsPermissions(true);
        assertEquals(0, InterruptedSubFolderIndex.barriers().length);
        assertSame(InterruptedSubFolderIndex.barriers(), InterruptedSubFolderIndex.barriers());
    }

    /**
     * With the feature switched off - the production default, it is a startup switch - an interruption
     * never becomes a barrier, and the effective access behaves exactly as before PFC-3543. The snapshot
     * needs no feature check of its own for that: {@code refresh} selects its entries via
     * {@link de.dal33t.powerfolder.light.FolderInfo#inheritsPermissions()}, which reports "inherits"
     * while the feature is off, so nothing is ever recorded.
     */
    public void testEffectiveAccessIgnoresInterruptionWhenFeatureDisabled() throws IOException {
        // setUp enabled the feature - this test needs it off from the start, like a production server
        // that never got the switch. tearDown disables it anyway, so nothing leaks either way.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        try {
            Folder topFolder = getFolderAtBart();
            FolderInfo topInfo = topFolder.getInfo();

            Path root = topFolder.getPhysicalDir();
            Files.createDirectories(root.resolve("secret/inside"));
            TestHelper.scanFolder(topFolder);

            Folder secret = topFolder.share((DirectoryInfo) topFolder.getFileInfo("secret"));
            secret.setInheritsPermissions(false);
            assertEquals(0, InterruptedSubFolderIndex.barriers().length);

            Account hans = new Account();
            hans.grant(FolderPermission.readWrite(topInfo));
            assertEquals(AccessMode.READ_WRITE, hans.getAllowedAccess(topInfo, "secret/inside/x.txt"));
        } finally {
            Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        }
    }
}
