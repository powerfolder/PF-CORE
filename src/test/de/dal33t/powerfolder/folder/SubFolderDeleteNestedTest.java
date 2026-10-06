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

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PFC-3646: Deleting a share that lies inside an interrupted subfolder.
 * <p>
 * The web resolves a path to the folder that owns its place - inside an interrupted subfolder that is
 * the subfolder, not the top folder. Only the top folder dissolves shares, so each of these deletions
 * answered "deleted" and changed nothing.
 * <p>
 * Tree: {@code outer} (interrupted) holding {@code outer/s2} (inheriting share) and the plain directory
 * {@code outer/dd} with the interrupted share {@code outer/dd/s} inside it.
 */
public class SubFolderDeleteNestedTest extends ControllerTestCase {

    private Folder outer;
    private Folder s2;
    private Folder s;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        setupTestFolder(SyncProfile.HOST_FILES);

        Path base = getFolder().getPhysicalDir();
        TestHelper.createRandomFile(base.resolve("outer"), "Outer.txt");
        TestHelper.createRandomFile(base.resolve("outer/s2"), "InS2.txt");
        TestHelper.createRandomFile(base.resolve("outer/dd/s"), "InS.txt");
        scanFolder(getFolder());

        outer = getFolder().share(directory(getFolder(), "outer"));
        s2 = getFolder().share(directory(getFolder(), "outer/s2"));
        s = getFolder().share(directory(getFolder(), "outer/dd/s"));
        outer.setInheritsPermissions(false);
        s.setInheritsPermissions(false);
        assertTrue("Sanity: s2 inherits", s2.getInfo().inheritsPermissions());
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /** The inheriting share inside the interrupted subfolder, deleted on its own directory as the web does it. */
    public void testAnInheritingShareInsideARestrictedSubFolderIsDeleted() {
        Path dir = s2.getLocalBase();

        s2.removeFilesLocal((AccountInfo) null, s2.getBaseDirectoryInfo());

        assertDissolved(s2);
        assertFalse("outer/s2 is deleted on disk", Files.exists(dir));
        assertDeletedIn(outer, "s2");
    }

    /** A plain directory of the interrupted subfolder that holds an interrupted share. */
    public void testADirectoryHoldingARestrictedShareIsDeleted() {
        Path dir = outer.getLocalBase().resolve("dd");

        outer.removeFilesLocal((AccountInfo) null, directory(outer, "dd"));

        assertDissolved(s);
        assertFalse("outer/dd is deleted on disk", Files.exists(dir));
        assertDeletedIn(outer, "dd");
    }

    /** The interrupted share inside the interrupted subfolder, deleted on its own directory. */
    public void testARestrictedShareInsideARestrictedSubFolderIsDeleted() {
        Path dir = s.getLocalBase();

        s.removeFilesLocal((AccountInfo) null, s.getBaseDirectoryInfo());

        assertDissolved(s);
        assertFalse("outer/dd/s is deleted on disk", Files.exists(dir));
        assertDeletedIn(outer, "dd/s");
        assertTrue("The directory above it is untouched", Files.isDirectory(outer.getLocalBase().resolve("dd")));
    }

    private void assertDissolved(Folder share) {
        assertNull(share + " is dissolved",
            getController().getFolderRepository().findSubFolder(share.getInfo().getLocation()));
    }

    private static void assertDeletedIn(Folder folder, String relativeName) {
        FileInfo fInfo = folder.getFileInfo(relativeName);
        assertNotNull(folder + " knows " + relativeName, fInfo);
        assertTrue(folder + " reports " + relativeName + " as deleted", fInfo.isDeleted());
    }

    private static DirectoryInfo directory(Folder folder, String relativeName) {
        FileInfo fInfo = folder.getFileInfo(relativeName);
        assertNotNull(folder + ": no entry for " + relativeName, fInfo);
        assertTrue(relativeName + " is no directory", fInfo.isDiretory());
        return (DirectoryInfo) fInfo;
    }
}
