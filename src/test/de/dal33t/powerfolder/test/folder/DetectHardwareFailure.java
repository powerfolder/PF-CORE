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
* $Id$
*/
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class DetectHardwareFailure extends ControllerTestCase {

    public void setUp() throws Exception {
        
        super.setUp();     

        setupTestFolder(SyncProfile.HOST_FILES);
        
        File localbase = getFolder().getLocalBase();
        // create 100 random files
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(localbase);
        }
        File sub = new File(localbase, "sub");
        sub.mkdir();

        // create 100 random files in sub folder
        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(sub);
        }
    }

    public void testHardwareFailure() throws IOException {
        scanFolder(getFolder());
        assertEquals(200, getFolder().getKnownFiles().size());
        // now delete the folder :-D
        FileUtils.deleteDirectory(getFolder().getLocalBase());
        
        scanFolder(getFolder());
        assertEquals(200, getFolder().getKnownFiles().size());
        // on hardware failure of deletion of folder of disk we don't want to
        // mark them as deleted. to prevent the los of files to spread over more
        // systems
        for (FileInfo fileInfo : getFolder().getKnownFiles()) {
            assertFalse(fileInfo.isDeleted());
        }
    }
}
