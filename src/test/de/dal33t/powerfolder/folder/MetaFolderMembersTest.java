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
 *
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that members become known to nodes via the metafolder Members file
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 */
public class MetaFolderMembersTest extends FiveControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test scenario: 1. Bart creates a folder and Homer joins. 2. Homer shuts
     * down. 3. Marge joins with Bart. Result: Marge should know about Bart and
     * Homer.
     */
    @Test
    public void testMembersSync() throws IOException {
        // 1. Bart creates a folder and Homer joins.
        FolderInfo folderInfo = FolderInfoFactory.newTopFolderForTest("testFolder", "testFolder" + IdGenerator.makeFolderId());

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

        final String bartId = controllerBart.getMySelf().getId();
        final String homerId = controllerHomer.getMySelf().getId();
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return knowsMembers(folderBart, bartId, homerId)
                    && knowsMembers(folderHomer, bartId, homerId)
                    && membersFileIds(folderBart).contains(homerId);
            }

            public String message() {
                return "Homer did not reach Bart's members. Members at Bart: "
                    + folderBart.getMembersAsCollection()
                    + ", members at Homer: "
                    + folderHomer.getMembersAsCollection()
                    + ", members file at Bart: " + membersFileIds(folderBart);
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
        final String margeId = controllerMarge.getMySelf().getId();
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return knowsMembers(folderBart, bartId, homerId, margeId)
                    && knowsMembers(folderMarge, bartId, homerId, margeId);
            }

            public String message() {
                return "Members at Bart: "
                    + folderBart.getMembersAsCollection()
                    + ", members at Marge: "
                    + folderMarge.getMembersAsCollection();
            }
        });

        assertEquals( 3,
            folderBart.getMembersCount(),"Bart has the wrong number of members");
        assertEquals( 3,
            folderMarge.getMembersCount(),"Marge has the wrong number of members");

    }

    private static boolean knowsMembers(Folder folder, String... memberIds) {
        Set<String> known = new HashSet<String>();
        for (Member member : folder.getMembersAsCollection()) {
            known.add(member.getId());
        }
        for (String memberId : memberIds) {
            if (!known.contains(memberId)) {
                return false;
            }
        }
        return true;
    }

    private static Set<Object> membersFileIds(Folder folder) {
        Folder metaFolder = folder.getController().getFolderRepository()
            .getMetaFolder(folder.getInfo());
        if (metaFolder == null) {
            return Collections.emptySet();
        }
        Path membersFile = metaFolder.getLocalBase().resolve(
            Folder.METAFOLDER_MEMBERS);
        if (Files.notExists(membersFile)) {
            return Collections.emptySet();
        }
        try (ObjectInputStream in = new ObjectInputStream(
            new BufferedInputStream(Files.newInputStream(membersFile))))
        {
            Map<?, ?> members = (Map<?, ?>) in.readObject();
            return new HashSet<Object>(members.keySet());
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            return Collections.emptySet();
        }
    }
}
