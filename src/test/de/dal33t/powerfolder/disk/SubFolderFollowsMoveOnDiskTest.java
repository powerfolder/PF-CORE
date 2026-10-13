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
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * PFC-3645, follow-up of PFS-5926: a shared subfolder whose directory moved on disk - renamed, or moved with a parent
 * directory, by whatever way: web, WebDAV, desktop client, file system - follows its data. A scan used to report
 * the old place deleted and dissolve the share with its permissions; one rename of a parent directory cost the
 * customer seven restricted subfolders.
 * <p>
 * Every case moves the directory on disk and lets a scan find out, the way a change from outside arrives. Only a
 * real deletion may still dissolve.
 * <p>
 * The {@code ignore_} cases fail until PFC-3645 is implemented - enable them with it.
 */
public class SubFolderFollowsMoveOnDiskTest extends TwoControllerTestCase {

    private Folder top;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        top = getFolderAtBart();
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /** The customer's case: the directory around the subfolder is renamed. */
    public void ignore_testRenamingTheParentDirectoryKeepsTheSubFolder() throws IOException {
        FolderInfo restricted = interruptedSubFolder("area/old/restricted", "report.pdf");

        move("area/old", "area/new");
        scanFolder(top);

        assertFollowed(restricted, "area/new/restricted", "report.pdf");
    }

    /** The subfolder's own directory renamed outside the web interface (PFS-5850 covers only the web). */
    public void ignore_testRenamingTheSubFolderDirectoryItselfKeepsTheSubFolder() throws IOException {
        FolderInfo restricted = interruptedSubFolder("area/restricted", "report.pdf");

        move("area/restricted", "area/renamed");
        scanFolder(top);

        assertFollowed(restricted, "area/renamed", "report.pdf");
    }

    /** Moved into another branch of the tree, not only renamed in place. */
    public void ignore_testMovingTheParentDirectoryIntoAnotherBranchKeepsTheSubFolder() throws IOException {
        FolderInfo restricted = interruptedSubFolder("area/old/restricted", "report.pdf");
        Files.createDirectories(top.getLocalBase().resolve("archive/2026"));
        scanFolder(top);

        move("area/old", "archive/2026/old");
        scanFolder(top);

        assertFollowed(restricted, "archive/2026/old/restricted", "report.pdf");
    }

    /** A subfolder inside a subfolder moves with it, and both stay what they were. */
    public void ignore_testNestedSubFoldersBothFollow() throws IOException {
        Path outerDir = Files.createDirectories(top.getLocalBase().resolve("area/old/outer/inner"));
        TestHelper.createRandomFile(outerDir.getParent(), "a.txt");
        TestHelper.createRandomFile(outerDir, "b.txt");
        scanFolder(top);
        // Inner first: a share needs the directory's row, and the outer interruption takes it away from the top.
        Folder innerFolder = top.share((DirectoryInfo) top.getFileInfo("area/old/outer/inner"));
        Folder outerFolder = top.share((DirectoryInfo) top.getFileInfo("area/old/outer"));
        outerFolder.setInheritsPermissions(false);
        repository().getFolder(innerFolder.getInfo()).setInheritsPermissions(false);
        FolderInfo outer = repository().getFolder(outerFolder.getInfo()).getInfo();
        FolderInfo inner = repository().getFolder(innerFolder.getInfo()).getInfo();
        assertFalse(outer.inheritsPermissions(), "Sanity: outer interrupted");
        assertFalse(inner.inheritsPermissions(), "Sanity: inner interrupted");

        move("area/old", "area/new");
        scanFolder(top);

        assertFollowed(outer, "area/new/outer", "a.txt");
        assertFollowed(inner, "area/new/outer/inner", "b.txt");
    }

    /** The content of a followed subfolder stays out of the top folder's database - no stray rows. */
    public void ignore_testTheMovedContentDoesNotEnterTheTopFolder() throws IOException {
        interruptedSubFolder("area/old/restricted", "report.pdf");

        move("area/old", "area/new");
        scanFolder(top);

        FileInfo stray = top.getDAO().find(
            FileInfoFactory.lookupInstance(top.getInfo(), "area/new/restricted/report.pdf"), null);
        assertTrue(stray == null || stray.isDeleted(),
            "The top folder holds no row of the subfolder's content: " + stray);
    }

    /** Really deleted: then, and only then, the share is dissolved - as before. */
    @Test
    public void testDeletingTheParentDirectoryStillDissolvesTheSubFolder() throws IOException {
        FolderInfo restricted = interruptedSubFolder("area/old/restricted", "report.pdf");

        PathUtils.recursiveDelete(top.getLocalBase().resolve("area/old"));
        scanFolder(top);

        assertNull(repository().getFolder(restricted), "A deleted subfolder is dissolved");
    }

    // Helpers ****************************************************************

    /** Creates the directory with one file, shares it from the top folder and interrupts its inheritance. */
    private FolderInfo interruptedSubFolder(String location, String fileName) throws IOException {
        Path dir = Files.createDirectories(top.getLocalBase().resolve(location));
        TestHelper.createRandomFile(dir, fileName);
        scanFolder(top);
        FileInfo row = top.getFileInfo(location);
        assertNotNull(row, location + ": no row to share");
        Folder subFolder = top.share((DirectoryInfo) row);
        assertNotNull(subFolder, location + ": not shared");
        subFolder.setInheritsPermissions(false);
        assertFalse(subFolder.getInfo().inheritsPermissions(), "Sanity: " + location + " is interrupted");
        return subFolder.getInfo();
    }

    private void move(String from, String to) throws IOException {
        Path target = top.getLocalBase().resolve(to);
        Files.createDirectories(target.getParent());
        // Windows: a mounted folder's directory can be held open for a moment (watcher, database)
        for (int attempt = 1; ; attempt++) {
            try {
                Files.move(top.getLocalBase().resolve(from), target);
                return;
            } catch (AccessDeniedException e) {
                if (attempt == 20) {
                    throw e;
                }
                TestHelper.waitMilliSeconds(250);
            }
        }
    }

    private void assertFollowed(FolderInfo before, String newLocation, String fileName) {
        Folder followed = repository().getFolder(before);
        assertNotNull(followed,
            before.getLocalizedName() + ": the share is gone - it was dissolved instead of followed");
        assertEquals(newLocation, followed.getInfo().locationPath(), "Same folder at the new place");
        assertFalse(followed.getInfo().inheritsPermissions(), "Still interrupted");
        assertEquals(top.getLocalBase().resolve(newLocation).toAbsolutePath().normalize(),
            followed.getLocalBase().toAbsolutePath().normalize(),
            "Mounted at the new place");
        FileInfo content = followed.getFileInfo(fileName);
        assertNotNull(content, "It still holds its content");
        assertFalse(content.isDeleted());
    }

    private FolderRepository repository() {
        return getContollerBart().getFolderRepository();
    }
}
