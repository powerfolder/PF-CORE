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
package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.DragDropChecker;
import de.dal33t.powerfolder.util.test.TestHelper;

public class DragDropCheckerTest extends TestCase {

    public void testNothing() {
    }
    
    public void xtestDropFiles() {
        File testFile1 = TestHelper.createRandomFile(new File("build\\test"),
            "file1.ext");
        
        File testFile2 = TestHelper.createRandomFile(new File(
            "build/test/sub1/sub2/sub3"), "file2.ext");

        File testDirRoot = new File("build/test");
        File testDir1 = new File("build/test/sub1");
        File testDir2 = new File("build/test/sub1/sub2");
        File testDir3 = new File("build/test/sub1/sub2/sub3");

        List<File> dropList = new ArrayList<File>(2);

        // copy file onto itself: (should not be allowed)
        dropList.add(testFile1);
        assertFalse(DragDropChecker.allowDrop(dropList, testDirRoot));
        
        dropList.clear();
        // copy file from root to subdir
        dropList.add(testFile1);
        assertTrue(DragDropChecker.allowDrop(dropList, testDir1));

        dropList.clear();
        // copy file from root to deep subdir
        dropList.add(testFile1);
        assertTrue(DragDropChecker.allowDrop(dropList, testDir2));
        
        dropList.clear();
        // copy file from deep sub dir to root
        dropList.add(testFile2);
        assertTrue(DragDropChecker.allowDrop(dropList, testDirRoot));

        dropList.clear();
        // copy deep subdir dir to root
        dropList.add(testDir3);
        assertTrue(DragDropChecker.allowDrop(dropList, testDirRoot));

        dropList.clear();
        //copy root to subdir (should not be allowed)                
        dropList.add(testDirRoot);
        assertFalse(DragDropChecker.allowDrop(dropList, testDir3));
        
    }
}
