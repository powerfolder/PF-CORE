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
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * PFS-5911: A subfolder's tags are stored where its entry lives - on the directory while it inherits, on its
 * FolderInfo while interrupted. Only the switch between both copies them, and always the leading ones.
 * <p>
 * Sharing copies the directory's tags onto the FolderInfo, so an inheriting subfolder carries a second, stale copy
 * there once the directory is tagged again.
 */
public class SubFolderTagsLeadTest extends ControllerTestCase {

    private Folder sub;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        setupTestFolder(SyncProfile.HOST_FILES);

        TestHelper.createRandomFile(getFolder().getPhysicalDir().resolve("proj"), "Plan.txt");
        scanFolder(getFolder());
        tagDirectory("[\"Old\"]");
        sub = getFolder().share(directory());
        assertEquals(List.of("Old"), sub.getInfo().getTagsList(), "Sanity: sharing copies the tags");

        tagDirectory("[\"New\"]");
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    @Test
    public void testInterruptTakesTheTagsOfTheDirectory() {
        sub.setInheritsPermissions(false);

        assertEquals(List.of("New"), sub.getInfo().getTagsList());
    }

    @Test
    public void testInterruptOfAnUntaggedDirectoryLeavesTheFolderUntagged() {
        tagDirectory(null);

        sub.setInheritsPermissions(false);

        assertEquals(List.of(), sub.getInfo().getTagsList());
    }

    @Test
    public void testRestoreHandsTheTagsOfTheFolderBack() {
        sub.setInheritsPermissions(false);
        getController().getFolderRepository().renameFolder(
            FolderInfoFactory.changeTags(sub.getInfo(), "[\"Interrupted\"]"), false, null);

        sub.setInheritsPermissions(true);

        assertEquals(List.of("Interrupted"), directory().getTagsList());
    }

    @Test
    public void testUnshareKeepsTheTagsOfTheDirectory() {
        getFolder().unshare(sub.getInfo().getLocation());

        assertEquals(List.of("New"), directory().getTagsList());
    }

    @Test
    public void testUnshareOfAnInterruptedFolderKeepsItsTags() {
        sub.setInheritsPermissions(false);
        getController().getFolderRepository().renameFolder(
            FolderInfoFactory.changeTags(sub.getInfo(), "[\"Interrupted\"]"), false, null);

        getFolder().unshare(sub.getInfo().getLocation());

        assertEquals(List.of("Interrupted"), directory().getTagsList());
    }

    private void tagDirectory(String tagsJson) {
        assertNotNull(getFolder().updateTags(directory(), tagsJson));
    }

    private DirectoryInfo directory() {
        FileInfo fInfo = getFolder().getFileInfo("proj");
        assertNotNull(fInfo, "No entry for proj");
        assertTrue(fInfo.isDiretory(), "proj is no directory");
        return (DirectoryInfo) fInfo;
    }
}
