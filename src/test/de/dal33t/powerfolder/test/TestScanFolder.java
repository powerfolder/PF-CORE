/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test;

import java.nio.file.Paths;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * Scan took: 49301 Scan took: 21651 Scan took: 21892 Scan took: 21651 Scan
 * took: 21421
 */
public class TestScanFolder extends ControllerTestCase {

    private TestScanFolder() {
        try {
            setUp();
            doTest();
            tearDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.exit(0);
    }
    private final String location = "f:/test";
    private Folder folder;

    private void doTest() throws Exception {
        FolderInfo testFolder = new FolderInfo("testFolder",
            IdGenerator.makeFolderId());
        FolderSettings folderSettings = new FolderSettings(Paths.get(location),
            SyncProfile.HOST_FILES, 0);
        folder = getController().getFolderRepository().createFolder(testFolder,
            folderSettings);

        // long started =System.currentTimeMillis();
        scanFolder(folder);
        // FolderScanner scanner = new FolderScanner(getController());
        // new Thread(scanner).start();
        // scanner.scan(folder);

        // while(scanner.isScanning()) {
        // Thread.sleep(1000);
        // }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new TestScanFolder();

    }

}
