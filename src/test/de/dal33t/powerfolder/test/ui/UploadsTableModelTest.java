/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.ui;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;

public class UploadsTableModelTest extends TwoControllerTestCase {
    private static final String BASEDIR1 = "build/test/controllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/controllerLisa/testFolder";

    private Folder folderBart;
    private Folder folderLisa;
    private UploadsTableModel bartModel;
    private UploadsTableModel lisaModel;

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("FileTransferTest.setUp()");
        super.setUp();

        bartModel = new UploadsTableModel(getContollerBart()
            .getTransferManager());
        lisaModel = new UploadsTableModel(getContollerLisa()
            .getTransferManager());

        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2),
            SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        folderBart = getContollerBart().getFolderRepository().getFolder(
            testFolder);
        folderLisa = getContollerLisa().getFolderRepository().getFolder(
            testFolder);
    }

    public void testSingleFileUpload() throws IOException {
        TestHelper.createRandomFile(folderBart.getLocalBase());

        folderBart.scanLocalFiles(true);

        // Copy
        TestHelper.waitMilliSeconds(1000);

        // No upload in tablemodel
        assertEquals(0, bartModel.getRowCount());
    }

    public void testRunningUpload() throws IOException {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);
        folderBart.scanLocalFiles(true);

        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });

        // 1 active uploads
        assertEquals(1, bartModel.getRowCount());

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitMilliSeconds(500);
    }

    public void testAbortUpload() throws IOException {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);
        folderBart.scanLocalFiles(true);

        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });

        // Abort
        Download download = getContollerLisa().getTransferManager()
            .getActiveDownloads()[0];
        download.abort();

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitMilliSeconds(500);
    }
}
