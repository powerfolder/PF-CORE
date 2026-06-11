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
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * @author sprajc TODO Find better name.
 */
public class PerformanceTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        makeFriends();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
    }

    @Test
    public void testManyFilelistDeltas() throws ConnectionException {
        FolderInfo foInfo = getFolderAtBart().getInfo();
        for (int i = 0; i < 100000; i++) {
            List<FileInfo> files = new ArrayList<FileInfo>();
            for (int j = 0; j < 1; j++) {
                FileInfo fInfo = createTestFileInfo(foInfo, j);
                files.add(fInfo);
            }
            Message[] msgs = FileList.create4Test(foInfo, files,
                new DiskItemFilter());
            Member lisaAtBart = getContollerBart().getNodeManager().getNode(
                getContollerLisa().getMySelf().getId());
            for (int j = 0; j < msgs.length; j++) {
                lisaAtBart.sendMessage(msgs[j]);
            }

        }

    }

    private FileInfo createTestFileInfo(FolderInfo foInfo, int i) {
        FileInfo fInfo = FileInfoFactory.unmarshallExistingFile(foInfo,
            "subdir/SUBDIR2withAlongName/Another_deep/Spreadsheet-" + i
                + ".xls", IdGenerator.makeFileId(), i, getContollerBart()
                .getMySelf().getInfo(), getContollerBart().getMySelf()
                .getAccountInfo(), new Date(), i, null, false, null);
        return fInfo;
    }
}
