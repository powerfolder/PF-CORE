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
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-5884: a subfolder that inherits its permissions has no database of its own - it writes through a
 * {@link SubFolderFileInfoDAOProxy} into the database of the folder that holds its rows. Marking itself
 * as dirty instead of that holder meant nothing was ever written: the row served every request until
 * the tree was unmounted and was gone afterwards, while the directory stayed on disk.
 */
public class SubFolderPersistTest extends TwoControllerTestCase {

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Without the feature every subfolder reports "inherits", and nothing here could be interrupted.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * The chain of the customer's case: an inheriting subfolder inside an interrupted one. A directory
     * created in the inheriting subfolder has to be in the holder's database file once the tree is
     * unmounted, because that file is what the next mount reads.
     */
    @Test
    public void testADirectoryCreatedInAnInheritingSubFolderSurvivesTheUnmount() throws Exception {
        Folder topFolder = getFolderAtBart();
        Files.createDirectories(topFolder.getPhysicalDir().resolve("projects/reports/2026"));
        scanFolder(topFolder);

        // Sharing is only allowed from the top folder, so the nesting is built first and the outer
        // one interrupted afterwards - the state the migration leaves behind.
        Folder inheriting = subFolderAt(topFolder, "projects/reports/2026");
        Folder interrupted = subFolderAt(topFolder, "projects/reports");
        interrupted.setInheritsPermissions(false);
        assertTrue(inheriting.getDAO() instanceof SubFolderFileInfoDAOProxy, "Sanity: the inheriting subfolder borrows another folder's database");

        // Everything the setup produced goes to disk first, so only the new row can dirty the holder
        // again - otherwise it would be written along with the rest and prove nothing.
        flush(interrupted);

        createDirectory(inheriting, "Q3");
        assertNotNull(inheriting.getFileInfo("Q3"), "Sanity: the row is served while the tree is mounted");
        assertNotNull(interrupted.getFileInfo("2026/Q3"), "Sanity: the interrupted subfolder is the one holding it");

        // The order of a tree unmount: the subfolder, then the folder holding its rows.
        inheriting.shutdown();
        interrupted.shutdown();

        assertTrue(databaseOf(interrupted).contains("2026/Q3"), "The holder's database must carry the row - it is what the next mount reads."
            + " Found: " + databaseOf(interrupted));
    }

    /**
     * The other half: the inheriting subfolder must not write a database file of its own. Nothing reads
     * it back - {@code loadFolderDB} skips a borrowed database - and it was written empty, since the
     * rows live in the holder's.
     */
    @Test
    public void testAnInheritingSubFolderWritesNoDatabaseOfItsOwn() throws Exception {
        Folder topFolder = getFolderAtBart();
        Files.createDirectories(topFolder.getPhysicalDir().resolve("projects/reports/2026"));
        scanFolder(topFolder);

        Folder inheriting = subFolderAt(topFolder, "projects/reports/2026");
        Folder interrupted = subFolderAt(topFolder, "projects/reports");
        interrupted.setInheritsPermissions(false);

        createDirectory(inheriting, "Q4");
        inheriting.shutdown();

        Path ownDatabase = inheriting.getSystemSubDir().resolve(Constants.DB_FILENAME);
        assertFalse(Files.exists(ownDatabase), "A borrowed database is never read back, so writing one only loses rows: "
            + ownDatabase);
    }

    /**
     * Creates a directory the way the API does it - on disk, then scanned into the folder that was
     * addressed. Scanning the whole subfolder is not the same thing and is not what happens in
     * production.
     */
    private void createDirectory(Folder folder, String relativeName) throws Exception {
        Path newDir = folder.getPhysicalDir().resolve(relativeName);
        Files.createDirectories(newDir);
        FileInfo dirInfo = FileInfoFactory.newFile(folder, newDir, null,
            getContollerBart().getMySelf().getInfo(), null, null, true, null);
        folder.scanDirectory(dirInfo, newDir);
    }

    /** Shares the given directory as a subfolder. Only the top folder may do this. */
    private Folder subFolderAt(Folder topFolder, String relativeName) {
        FileInfo fInfo = topFolder.getFileInfo(relativeName);
        assertNotNull(fInfo, topFolder + ": no row for " + relativeName);
        return topFolder.share((DirectoryInfo) fInfo);
    }

    /**
     * Writes out what is pending. A pattern change schedules the persister, which stores the database
     * if the folder is dirty and clears the flag - there is no other way in from outside.
     */
    private static void flush(Folder folder) {
        folder.addPattern("zzz-flush-marker");
        TestHelper.waitMilliSeconds(2500);
        folder.removePattern("zzz-flush-marker");
    }

    /** The relative names in a folder's database file, as the next mount would read them. */
    private static List<String> databaseOf(Folder folder) throws Exception {
        Path dbFile = folder.getSystemSubDir().resolve(Constants.DB_FILENAME);
        assertTrue(Files.exists(dbFile), folder + ": no database file was written at all: " + dbFile);
        try (ObjectInputStream in = new ObjectInputStream(
            new BufferedInputStream(Files.newInputStream(dbFile))))
        {
            FileInfo[] files = (FileInfo[]) in.readObject();
            List<String> names = new ArrayList<>(files.length);
            for (FileInfo fInfo : files) {
                names.add(fInfo.getRelativeName());
            }
            return names;
        }
    }
}
