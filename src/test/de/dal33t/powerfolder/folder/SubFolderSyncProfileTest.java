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

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

import java.nio.file.Files;

/**
 * PFS-5889: the subfolders of a tree follow its top folder's sync profile.
 * <p>
 * A subfolder is given the top folder's profile when it is shared and used to keep it from then on. The
 * migration shares its subfolders while it has the workspace on the manual profile, and restores only the
 * workspace afterwards - so thousands of subfolders stayed on "Manual Sync" for good and were never scanned
 * again, next to top folders that all scan.
 */
public class SubFolderSyncProfileTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    /** The migration's sequence: workspace on manual, share, workspace back - the subfolder has to come along. */
    public void testASubFolderSharedOnTheManualProfileFollowsTheRestore() throws Exception {
        Folder top = getFolder();
        top.setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);

        Folder sub = share("projects/reports");
        assertEquals("Sanity: a share takes the top folder's profile of the moment",
            SyncProfile.MANUAL_SYNCHRONIZATION, sub.getSyncProfile());

        top.setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        assertEquals("The subfolder follows the top folder", SyncProfile.AUTOMATIC_SYNCHRONIZATION, sub.getSyncProfile());
    }

    /** The other direction as well - a workspace put on manual takes its subfolders with it. */
    public void testASubFolderFollowsTheTopFolderOntoTheManualProfile() throws Exception {
        Folder top = getFolder();
        Folder sub = share("projects/reports");
        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, sub.getSyncProfile());

        top.setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);

        assertEquals(SyncProfile.MANUAL_SYNCHRONIZATION, sub.getSyncProfile());
    }

    /** A subfolder that is set on its own stays where it was put - only the top folder pulls the others. */
    public void testASubFolderSetOnItsOwnMovesNobodyElse() throws Exception {
        Folder top = getFolder();
        Folder first = share("projects/reports");
        Folder second = share("projects/minutes");

        first.setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);

        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, top.getSyncProfile());
        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, second.getSyncProfile());
        assertEquals(SyncProfile.MANUAL_SYNCHRONIZATION, first.getSyncProfile());
    }

    private Folder share(String relativeName) throws Exception {
        Files.createDirectories(getFolder().getPhysicalDir().resolve(relativeName));
        scanFolder(getFolder());
        DirectoryInfo dirInfo = (DirectoryInfo) getFolder().getFileInfo(relativeName);
        assertNotNull(relativeName + ": not scanned in", getFolder().getFile(dirInfo));
        Folder sub = getFolder().share(dirInfo);
        assertNotNull(relativeName + ": not shared", sub);
        return sub;
    }
}
