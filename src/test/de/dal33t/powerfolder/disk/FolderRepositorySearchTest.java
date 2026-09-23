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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A search over several folders answers for ALL of them. Stopping early - after so many hits, or so many
 * folders - would be decided by the order the folders happen to be in, and a file would then be findable
 * or not depending on where its folder sits in that order. Whatever is done about the time a search takes
 * (PFS-5863), this is what must not be traded away for it.
 */
public class FolderRepositorySearchTest extends ControllerTestCase {

    private static final int FOLDERS = 5;
    private static final int FILES_PER_FOLDER = 3;

    private final List<Folder> folders = new ArrayList<>();

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ConfigurationEntry.SEARCH_INDEX_ENABLED.setValue(getController(), false);
        /* Two folders at a time, so the search really does run out of permits and come back for more -
         * with every folder started at once there would never be one left to leave out. */
        ConfigurationEntry.SEARCH_CONCURRENCY.setValue(getController(), 2);

        setupTestFolder(SyncProfile.HOST_FILES);
        folders.add(getFolder());
        fill(getFolder());

        for (int i = 0; i < FOLDERS - 1; i++) {
            FolderInfo info = FolderInfoFactory.newTopFolderForTest("searched" + i);
            Path base = TestHelper.getTestDir().resolve("searched" + i);
            Folder folder = joinFolder(info, base, SyncProfile.HOST_FILES);
            fill(folder);
            folders.add(folder);
        }
    }

    /** The same files in every folder, all of them answering the same query. */
    private void fill(Folder folder) {
        for (int i = 0; i < FILES_PER_FOLDER; i++) {
            TestHelper.createRandomFile(folder.getLocalBase(), "Quartalsbericht_" + i + ".txt");
        }
        scanFolder(folder);
    }

    private FileInfoCriteria criteria() {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.setRecursive(true);
        criteria.addKeyWord("quartalsbericht");
        return criteria;
    }

    @Test
    public void testEveryFolderIsSearched() {
        List<FileInfo> hits = getController().getFolderRepository().searchFiles(folders, criteria());
        assertEquals(FOLDERS * FILES_PER_FOLDER, hits.size(), "Every folder answers, none is left out");
    }

    /** Two runs of one search must answer the same, or page 2 of a result does not continue page 1. */
    @Test
    public void testTheSameSearchAnswersTheSame() {
        FolderRepository repository = getController().getFolderRepository();
        List<FileInfo> first = repository.searchFiles(folders, criteria());
        List<FileInfo> second = repository.searchFiles(folders, criteria());
        assertEquals(first.size(), second.size(), "The same number of hits");
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getRelativeName(), second.get(i).getRelativeName(), "Hit " + i + " is the same file");
        }
    }
}
