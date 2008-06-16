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

import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.folder.FileFilterModel;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FileFilterChangedEvent;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

/**
 * Test class to test the FileFilterModel.
 */
public class FileFilerModelTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupTestFolder(SyncProfile.HOST_FILES);
    }

    public void testFiles() {

        // create 100 random files
        File localbase = getFolder().getLocalBase();
        File testFile = TestHelper.createRandomFile(localbase);
        for (int i = 0; i < 99; i++) {
            TestHelper.createRandomFile(localbase);
        }

        scanFolder(getFolder());
        assertEquals(100, getFolder().getKnownFiles().size());

        FileFilterModel ffm = new FileFilterModel(getController());
        List<DiskItem> diskItems = new ArrayList<DiskItem>();
        assertTrue(diskItems.addAll(Arrays.asList(getFolder().getKnowFilesAsArray())));
        ffm.setFiles(diskItems);

        ///////////////////////////////////////////////////////////
        // Test that all files survive filtering as local files. //
        ///////////////////////////////////////////////////////////

        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger localFiles = new AtomicInteger();
        final AtomicInteger incomingFiles = new AtomicInteger();
        final AtomicInteger deletedFiles = new AtomicInteger();
        final AtomicInteger recycledFiles = new AtomicInteger();
        ffm.addFileFilterChangeListener(new FileFilterChangeListener() {
            public void filterChanged(FileFilterChangedEvent event) {
                count.set(event.getFilteredList().size());
                localFiles.set(event.getLocalFiles());
                incomingFiles.set(event.getIncomingFiles());
                deletedFiles.set(event.getDeletedFiles());
                recycledFiles.set(event.getRecycledFiles());
            }
        });
        ffm.filter();

        // Sleep to allow filtering to happen.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        assertEquals(100, count.get());
        assertEquals(100, localFiles.get());
        assertEquals(0, incomingFiles.get());
        assertEquals(0, deletedFiles.get());
        assertEquals(0, recycledFiles.get());

        ////////////////////////
        // Filter some files. //
        ////////////////////////
        ValueModel vm = new ValueHolder();
        ffm.setSearchField(vm);
        vm.setValue("5");

        ffm.filter();

        // Sleep to allow filtering to happen.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        // About half the files will remain.
        assertTrue(count.get() < 100);
        assertTrue(count.get() > 0);
        assertEquals(count.get(), localFiles.get());
        assertEquals(0, incomingFiles.get());
        assertEquals(0, deletedFiles.get());
        assertEquals(0, recycledFiles.get());

        ///////////////
        // Unfilter. //
        ///////////////
        ffm.setSearchField(vm);
        vm.setValue("");

        ffm.filter();

        // Sleep to allow filtering to happen.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        assertEquals(100, count.get());
        assertEquals(100, localFiles.get());
        assertEquals(0, incomingFiles.get());
        assertEquals(0, deletedFiles.get());
        assertEquals(0, recycledFiles.get());

        ////////////////////
        // Delete a file. //
        ////////////////////

        testFile.delete();
        scanFolder(getFolder());

        ffm.filter();

        // Sleep to allow filtering to happen.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        assertEquals(99, count.get());
        assertEquals(99, localFiles.get());
        assertEquals(0, incomingFiles.get());
        assertEquals(1, deletedFiles.get());
        assertEquals(0, recycledFiles.get());

        ////////////////////////
        // Find deleted file. //
        ////////////////////////

        ffm.setMode(FileFilterModel.MODE_DELETED_PREVIOUS);
        ffm.filter();

        // Sleep to allow filtering to happen.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        assertEquals(1, count.get());
        assertEquals(99, localFiles.get());
        assertEquals(0, incomingFiles.get());
        assertEquals(1, deletedFiles.get());
        assertEquals(0, recycledFiles.get());
    }
}
