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
 * $Id: FiveControllerTestCase.java 17501 2011-12-21 17:09:51Z tot $
 */
package de.dal33t.powerfolder.util.test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic testcase-setup with five controllers. Bart, Lisa, Homer, Marge
 * and Maggie.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that all controllers
 * are running. There are several utility methods to bring the test into a usual
 * state. To connect two controllers just call
 * <code>{@link #connect(Controller, Controller)}</code> in
 * <code>{@link #setUp()}</code>. After that all controllers are connected,
 * Lisa, Marge and Maggie run in normal node, Bart and Homer act as supernode.
 * <p>
 * You can access both controllers and do manupulating/testing stuff on them
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class FiveControllerTestCase extends MultipleControllerTestCase
{
    protected static final String BART_ID = "Bart";
    protected static final String HOMER_ID = "Homer";
    protected static final String MARGE_ID = "Marge";
    protected static final String LISA_ID = "Lisa";
    protected static final String MAGGIE_ID = "Maggie";

    // For the optional test folder.
    protected static final Path TESTFOLDER_BASEDIR_BART = TestHelper
        .getTestDir().resolve("ControllerBart/testFolder").toAbsolutePath();
    protected static final Path TESTFOLDER_BASEDIR_HOMER = TestHelper
        .getTestDir().resolve("ControllerHomer/testFolder").toAbsolutePath();
    protected static final Path TESTFOLDER_BASEDIR_MARGE = TestHelper
        .getTestDir().resolve("ControllerMarge/testFolder").toAbsolutePath();
    protected static final Path TESTFOLDER_BASEDIR_LISA = TestHelper
        .getTestDir().resolve("ControllerLisa/testFolder").toAbsolutePath();
    protected static final Path TESTFOLDER_BASEDIR_MAGGIE = TestHelper
        .getTestDir().resolve("ControllerMaggie/testFolder").toAbsolutePath();

    /**
     * The test folder info.
     */
    protected FolderInfo testFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Copy fresh configs
        startController(HOMER_ID);
        startController(BART_ID);
        startController(MARGE_ID);
        startController(LISA_ID);
        startController(MAGGIE_ID);
        System.out
            .println("-------------- Controllers started -----------------");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testFolder = null;
    }

    private void startController(String id) throws IOException {
        PathUtils.copyFile(Paths.get("src/test-resources/Controller" + id + ".config"),
            Paths.get("build/test/Controller" + id + "/PowerFolder.config"));
        Path miscDic = Controller.getMiscFilesLocation().resolve(
            "build/test/Controller" + id);
        PathUtils.recursiveDelete(miscDic);
        startController(id, "build/test/Controller" + id + "/PowerFolder");
    }

    // For subtest ************************************************************

    /**
     * Connects and waits for connection of the simpsons controllers
     * 
     * @throws RuntimeException
     *             if not all simpsons are connected afterwards.
     */
    protected void connectSimpsons() {
        if (!tryToConnectSimpsons()) {
            throw new RuntimeException("Unable to connect simpsons controllers");
        }
    }

    /**
     * Tries completely to connect the simpsons under each other.
     * 
     * @return true if succeeded
     */
    protected boolean tryToConnectSimpsons() {
        return connect(getContollerHomer(), getContollerBart())
            && connect(getContollerHomer(), getContollerMarge())
            && connect(getContollerMarge(), getContollerLisa())
            && connect(getContollerHomer(), getContollerLisa())
            && connect(getContollerHomer(), getContollerMaggie())
            && connect(getContollerBart(), getContollerMarge())
            && connect(getContollerBart(), getContollerLisa())
            && connect(getContollerBart(), getContollerMaggie())
            && connect(getContollerMarge(), getContollerMaggie())
            && connect(getContollerLisa(), getContollerMaggie());
    }

    protected Controller getContollerBart() {
        return getContoller(BART_ID);
    }

    protected Controller getContollerHomer() {
        return getContoller(HOMER_ID);
    }

    protected Controller getContollerMarge() {
        return getContoller(MARGE_ID);
    }

    protected Controller getContollerLisa() {
        return getContoller(LISA_ID);
    }

    protected Controller getContollerMaggie() {
        return getContoller(MAGGIE_ID);
    }

    protected Folder getFolderAtBart() {
        return getContollerBart().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtHomer() {
        return getContollerHomer().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtMarge() {
        return getContollerMarge().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtLisa() {
        return getContollerLisa().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtMaggie() {
        return getContollerMaggie().getFolderRepository().getFolder(testFolder);
    }

    /**
     * Sets the syncprofile on the testfolder of all simpsons.
     * 
     * @param profile
     *            the profile to set.
     */
    protected void setSyncProfile(SyncProfile profile) {
        getFolderAtHomer().setSyncProfile(profile);
        getFolderAtBart().setSyncProfile(profile);
        getFolderAtMarge().setSyncProfile(profile);
        getFolderAtLisa().setSyncProfile(profile);
        getFolderAtMaggie().setSyncProfile(profile);
    }

    /**
     * Lets the simpsons join all the same folder.
     * 
     * @param profile
     *            the profile to use
     */
    protected void joinTestFolder(SyncProfile profile) {
        joinTestFolder(profile, true);
    }

    /**
     * Lets the simpsons join all the same folder.
     * 
     * @param profile
     *            the profile to use
     */
    protected FolderInfo joinTestFolder(SyncProfile profile,
        boolean checkMemberships)
    {
        Reject.ifTrue(testFolder != null, "Reject already setup a testfolder!");
        // FIXME Waiting between join only because of race condition making join
        // fail.
        testFolder = new FolderInfo("testFolder", UUID.randomUUID().toString());
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, getContollerBart(),
            profile);
        TestHelper.waitMilliSeconds(100);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_HOMER, getContollerHomer(),
            profile);
        TestHelper.waitMilliSeconds(100);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_MARGE, getContollerMarge(),
            profile);
        TestHelper.waitMilliSeconds(100);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_LISA, getContollerLisa(),
            profile);
        TestHelper.waitMilliSeconds(100);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_MAGGIE, getContollerMaggie(),
            profile);
        if (checkMemberships) {
            try {
                // Give them time to join
                TestHelper.waitForCondition(5, new Condition() {
                    public boolean reached() {
                        return getFolderAtBart().getMembersCount() >= 5
                            && getFolderAtHomer().getMembersCount() >= 5
                            && getFolderAtMarge().getMembersCount() >= 5
                            && getFolderAtLisa().getMembersCount() >= 5
                            && getFolderAtMaggie().getMembersCount() >= 5;
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException("Homer: "
                    + getFolderAtHomer().getMembersCount() + ", Bart: "
                    + getFolderAtBart().getMembersCount() + ", Marge: "
                    + getFolderAtMarge().getMembersCount() + ", Lisa: "
                    + getFolderAtLisa().getMembersCount() + ", Maggie: "
                    + getFolderAtMaggie().getMembersCount() + ". Folder: "
                    + testFolder + " id: " + testFolder.id);
            }
        }
        return testFolder;
    }

    protected void clearCompletedDownloads() {
        clearCompletedDownloads(getContollerHomer());
        clearCompletedDownloads(getContollerBart());
        clearCompletedDownloads(getContollerMarge());
        clearCompletedDownloads(getContollerLisa());
        clearCompletedDownloads(getContollerMaggie());
    }

    private void clearCompletedDownloads(Controller controller) {
        for (DownloadManager dm : controller.getTransferManager()
            .getCompletedDownloadsCollection())
        {
            controller.getTransferManager().clearCompletedDownload(dm);
        }
    }

    protected void waitForCompletedDownloads(final int h, final int b,
        final int mar, final int l, final int mag)
    {
        TestHelper.waitForCondition(40, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerHomer().getTransferManager()
                    .getCompletedDownloadsCollection().size() == h
                    && getContollerBart().getTransferManager()
                        .getCompletedDownloadsCollection().size() == b
                    && getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size() == mar
                    && getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size() == l
                    && getContollerMaggie().getTransferManager()
                        .getCompletedDownloadsCollection().size() == mag;
            }

            public String message() {
                return "Completed downloads. Homer: "
                    + getContollerHomer().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + "/" + h
                    + ", Bart: "
                    + getContollerBart().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + "/" + b
                    + ", Marge: "
                    + getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + "/" + mar
                    + ", Lisa: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + "/" + l
                    + ", Maggie: "
                    + getContollerMaggie().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + "/" + mag;
            }
        });
    }
}
