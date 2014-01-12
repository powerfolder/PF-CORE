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
package de.dal33t.powerfolder.test.folder;

import java.io.IOException;
import java.nio.file.Files;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test that members become known to nodes via the metafolder Members file
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 */
public class MetaFolderMembersTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test scenario: 1. Bart creates a folder and Homer joins. 2. Homer shuts
     * down. 3. Marge joins with Bart. Result: Marge should know about Bart and
     * Homer.
     */
    public void testMembersSync() throws IOException {
        // 1. Bart creates a folder and Homer joins.
        FolderInfo folderInfo = new FolderInfo("testFolder", "testFolder"
            + IdGenerator.makeFolderId());

        Controller controllerBart = getContollerBart();
        Files.createDirectories(TESTFOLDER_BASEDIR_BART);
        FolderSettings folderSettingsBart = new FolderSettings(
            TESTFOLDER_BASEDIR_BART, SyncProfile.AUTOMATIC_SYNCHRONIZATION, 0);
        final Folder folderBart = controllerBart.getFolderRepository()
            .createFolder(folderInfo, folderSettingsBart);

        Files.createDirectories(TESTFOLDER_BASEDIR_HOMER);
        FolderSettings folderSettingsHomer = new FolderSettings(
            TESTFOLDER_BASEDIR_HOMER, SyncProfile.AUTOMATIC_SYNCHRONIZATION, 0);
        Controller controllerHomer = getContollerHomer();
        final Folder folderHomer = controllerHomer.getFolderRepository()
            .createFolder(folderInfo, folderSettingsHomer);

        try {
            controllerBart.connect(controllerHomer.getConnectionListener()
                .getAddress());
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getMembersCount() == 2
                    && folderHomer.getMembersCount() == 2;
            }
        });

        // 2. Homer shuts down.
        controllerHomer.shutdown();

        // 3. Marge joins with Bart.
        Files.createDirectories(TESTFOLDER_BASEDIR_MARGE);
        FolderSettings folderSettingsMarge = new FolderSettings(
            TESTFOLDER_BASEDIR_MARGE, SyncProfile.AUTOMATIC_SYNCHRONIZATION, 0);
        Controller controllerMarge = getContollerMarge();
        final Folder folderMarge = controllerMarge.getFolderRepository()
            .createFolder(folderInfo, folderSettingsMarge);

        try {
            controllerBart.connect(controllerMarge.getConnectionListener()
                .getAddress());
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        // Result: Marge should know about Bart and Homer.
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getMembersCount() == 3
                    && folderMarge.getMembersCount() == 3;
            }
        });

        assertEquals("Bart has the wrong number of members", 3,
            folderBart.getMembersCount());
        assertEquals("Marge has the wrong number of members", 3,
            folderMarge.getMembersCount());

    }
}
