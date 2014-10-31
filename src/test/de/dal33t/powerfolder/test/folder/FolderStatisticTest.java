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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for FolderStatistic.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderStatisticTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PreferencesEntry.EXPERT_MODE.setValue(getContollerHomer(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerMarge(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerMaggie(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerBart(), true);
        PreferencesEntry.BEGINNER_MODE.setValue(getContollerHomer(), false);
        PreferencesEntry.BEGINNER_MODE.setValue(getContollerMarge(), false);
        PreferencesEntry.BEGINNER_MODE.setValue(getContollerLisa(), false);
        PreferencesEntry.BEGINNER_MODE.setValue(getContollerMaggie(), false);
        PreferencesEntry.BEGINNER_MODE.setValue(getContollerBart(), false);
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION, false);
    }

    /**
     * Tests the sync percentage with one file that gets updated
     */
    public void testOneFile() throws IOException {
        LoggingManager.setConsoleLogging(Level.FINE);
        forceStatsCals();
        assertHasLastSyncDate(false, false, false, false, false);

        assertTrue(tryToConnectSimpsons());
        forceStatsCals();
        assertHasLastSyncDate(true, true, true, true, true);

        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, Files.size(testFile));
        getFolderAtBart().getStatistic().calculate0();
        getFolderAtLisa().getStatistic().calculate0();
        getFolderAtHomer().getStatistic().calculate0();
        getFolderAtMarge().getStatistic().calculate0();
        getFolderAtMaggie().getStatistic().calculate0();
        assertHasLastSyncDate(true, true, true, true, true);

        setSyncProfile(SyncProfile.HOST_FILES);
        TestHelper.changeFile(testFile, 500);
        scanFolder(getFolderAtHomer());
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtMarge());
        scanFolder(getFolderAtLisa());
        scanFolder(getFolderAtMaggie());
        // Give the members time broadcast changes
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(500);
        assertTotalSyncPercentage(20);
        assertIncomingFiles(1, 0, 1, 1, 1);
        assertMemberSizesActual(1000, 500, 1000, 1000, 1000);
        assertSyncPercentages(0, 100, 0, 0, 0);

        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(2, 0, 2, 2, 2);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, Files.size(testFile));

        disconnectAll();
        connectAll();

        // Make sure that last sync is persisted.
        assertHasLastSyncDate(true, true, true, true, true);
    }

    /**
     * Tests the sync percentage with one file that gets updated
     */
    public void testOneFileSameVersion() {
        assertTrue(tryToConnectSimpsons());
        setSyncProfile(SyncProfile.HOST_FILES);
        Path testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());

        Path testFileLisa = TestHelper.createTestFile(getFolderAtLisa()
            .getLocalBase(), testFileBart.getFileName().toString(),
            "TEST CONTENT".getBytes());
        try {
            Files.setLastModifiedTime(testFileLisa, FileTime.fromMillis(System.currentTimeMillis() + 1000 * 20));
        } catch (IOException e) {
            fail(e.getMessage());
        }
        scanFolder(getFolderAtLisa());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(12);
        assertTotalSyncPercentage(20);
        assertMemberSizesActual(0, 1000, 0, 12, 0);
        assertMemberSizesInSync(0, 0, 0, 12, 0);
        assertSyncPercentages(0, 0, 0, 100, 0);

        // Bring them in sync
        // 1 latest
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(1, 1, 1, 0, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 12);
    }

    public void testMultipleFilesMultiple() throws Exception {
        for (int i = 0; i < 5; i++) {
            testMultipleFiles();
            tearDown();
            setUp();
        }
    }

    public void testInitialSync() throws IOException {
        assertTrue(tryToConnectSimpsons());
        setSyncProfile(SyncProfile.HOST_FILES);
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        Path testFileAtLisa = getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName());
        int lisaTestFileSize = 1750;
        Files.copy(testFile, testFileAtLisa);
        TestHelper.changeFile(testFileAtLisa, lisaTestFileSize);
        Files.setLastModifiedTime(testFileAtLisa, FileTime.fromMillis(Files
            .getLastModifiedTime(testFile).toMillis() - 1000L * 60));

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertEquals(Files.size(testFile), getFolderAtBart().getStatistic()
            .getTotalSize());
        assertEquals(Files.size(testFile), getFolderAtLisa().getStatistic()
            .getTotalSize());

        assertEquals(0, getFolderAtBart().getStatistic()
            .getIncomingFilesCount());
        assertEquals(1, getFolderAtLisa().getStatistic()
            .getIncomingFilesCount());
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertMemberSizesActual(0, 1000, 0, lisaTestFileSize, 0);

        assertEquals(1, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(1, getFolderAtLisa().getStatistic().getTotalFilesCount());

        assertEquals(20.0, getFolderAtBart().getStatistic()
            .getAverageSyncPercentage());
        assertEquals(20.0, getFolderAtLisa().getStatistic()
            .getAverageSyncPercentage());

        // Bring them in sync
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        // For whatever
        TestHelper.waitMilliSeconds(500);
        forceStatsCals();
        // 1 normal
        assertAllInSync(1, Files.size(testFile));
    }

    public void testMultipleFiles() throws IOException {
        assertTrue(tryToConnectSimpsons());
        int nFiles = 100;
        int totalSize = 0;
        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < nFiles; i++) {
            Path f = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase(), 100);
            files.add(f);
            totalSize += Files.size(f);
        }
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        waitForCompletedDownloads(nFiles, 0, nFiles, nFiles, nFiles);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(nFiles, totalSize);

        // Change folder and let sync
        for (Path file : files) {
            TestHelper.changeFile(file, 200);
        }
        scanFolder(getFolderAtBart());
        int dls = nFiles * 2;
        totalSize *= 2;
        waitForCompletedDownloads(dls, 0, dls, dls, dls);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(nFiles, totalSize);

        // Bart

        // assertTotalFileCount(nFiles);
        // assertTotalSize(totalSize);
        // assertIncomingFiles(0, 0, 0, 0, 0);
        // assertMemberSizesActual(totalSize, totalSize, totalSize, totalSize,
        // totalSize);
        // assertMemberSizesInSync(totalSize, totalSize, totalSize, totalSize,
        // totalSize);
        // assertTotalSyncPercentage(100);
        // assertSyncPercentages(100, 100, 100, 100, 100);

        // Disconnect them
        disconnectAll();
        connectAll();
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(nFiles, totalSize);
    }

    public void testIncomingFiles() throws IOException {
        assertTrue(tryToConnectSimpsons());
        setSyncProfile(SyncProfile.HOST_FILES);
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(Files.size(testFile));
        assertTotalSyncPercentage(20);
        assertMemberSizesActual(0, 1000, 0, 0, 0);
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertIncomingFiles(1, 0, 1, 1, 1);

        // Bring them in sync
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, Files.size(testFile));
    }

    public void testDeletedFiles() throws IOException {
        assertTrue(tryToConnectSimpsons());
        // 1) Sync ONE file to all simpsons
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        Path testFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());
        assertEquals(0, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertEquals(1000, getFolderAtBart().getKnownFiles().iterator().next()
            .getSize());
        assertFalse(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 1000);

        // 2) Switch to manual sync and delete the file at Bart
        setSyncProfile(SyncProfile.HOST_FILES);
        Files.delete(testFile1);
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertEquals(1000, getFolderAtBart().getKnownFiles().iterator().next()
            .getSize());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());

        // 3) Create a SECOND file at Bart. File does not get sync (Still
        // MANUAL DOWNLOAD)
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 500);
        scanFolder(getFolderAtBart());
        assertEquals(2, getFolderAtBart().getKnownItemCount());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(500);
        assertTotalSyncPercentage(20);
        assertMemberSizesInSync(0, 500, 0, 0, 0);
        // They have still the old file and not the new one.
        assertMemberSizesActual(1000, 500, 1000, 1000, 1000);
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertIncomingFiles(1, 0, 1, 1, 1);

        // 4) Let them sync all changes. 1 deleted file and 1 existing file
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(2, 0, 2, 2, 2);
        waitForFileListOnTestFolder();
        waitForDeletion(testFile1.getFileName().toString());
        forceStatsCals();
        assertAllInSync(1, 500);
    }

    public void testAutodownload() throws IOException {
        assertTrue(tryToConnectSimpsons());
        // 1) Sync ONE file to all simpsons
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        Path testFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 500);
        scanFolder(getFolderAtBart());
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 500);

        // 2) Set to auto-download and delete the file.
        setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        Files.delete(testFile1);
        scanFolder(getFolderAtBart());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(0);
        assertTotalSize(0);
        // 0 total files = 100% sync
        assertTotalSyncPercentage(100);
        assertMemberSizesInSync(0, 0, 0, 0, 0);
        // They have still the old file and not the new one.
        assertMemberSizesActual(500, 0, 500, 500, 500);
        assertSyncPercentages(100, 100, 100, 100, 100);
        // No incoming files! Newest is DELETED @ bart!
        assertIncomingFiles(0, 0, 0, 0, 0);

        // 3) Let them sync all changes.
        setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        waitForDeletion(testFile1.getFileName().toString());
        forceStatsCals();

        assertEquals(4, getContollerMarge().getNodeManager()
            .getConnectedNodes().size());
        assertEquals(1, getFolderAtMarge().getKnownItemCount());
        assertTrue(getFolderAtMarge().getKnownFiles().iterator().next()
            .isDeleted());
        assertAllInSync(0, 0);

        // 4) Disconnect them
        disconnectAll();
        connectAll();
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(0, 0);
    }

    /**
     * Test the sync calculation with multiple files and mixed sync profiles
     */
    public void testMultipleFilesComplex() throws IOException {
        assertTrue(tryToConnectSimpsons());
        setSyncProfile(SyncProfile.HOST_FILES);
        final int nFiles = 50;
        long totalSize = 0;
        for (int i = 0; i < nFiles; i++) {
            Path testFile = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase());
            totalSize += Files.size(testFile);
        }

        scanFolder(getFolderAtBart());
        getFolderAtHomer()
            .setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        connectSimpsons();
        // Give the members time broadcast changes
        waitForCompletedDownloads(nFiles, 0, 0, nFiles, 0);
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(nFiles);
        assertTotalSize(totalSize);
        assertTotalSyncPercentage(60);

        assertMemberSizesActual(totalSize, totalSize, 0, totalSize, 0);
        assertSyncPercentages(100, 100, 0, 100, 0);

        // FileUtils.recursiveDelete(getFolderAtBart().getLocalBase());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getFolderAtBart().getLocalBase())) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    Files.delete(path);
                }
            }
        }
        catch (IOException ioe) {
            throw ioe;
        }

        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());

        getFolderAtMarge()
            .setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtMaggie().setSyncProfile(
            SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        TestHelper.waitMilliSeconds(5000);
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertAllInSync(1, Files.size(testFile));
    }

    private final void forceStatsCals() {
        forceStatsCalc(getFolderAtHomer());
        forceStatsCalc(getFolderAtBart());
        forceStatsCalc(getFolderAtMarge());
        forceStatsCalc(getFolderAtLisa());
        forceStatsCalc(getFolderAtMaggie());

        try {
            // Give all controllers a chance to calculate their own stats,
            // otherwise we get occasional rate conditions.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

    }

    private static final void forceStatsCalc(Folder folder) {
        folder.getStatistic().calculate0();
    }

    private void assertMemberSizesInSync(long homer, long bart, long marge,
        long lisa, long maggie)
    {
        assertMemberSizesInSync(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertMemberSizesInSync(Folder folder, long homer, long bart,
        long marge, long lisa, long maggie)
    {
        assertEquals(homer,
            folder.getStatistic()
                .getSizeInSync(getContollerHomer().getMySelf()));
        assertEquals(bart,
            folder.getStatistic().getSizeInSync(getContollerBart().getMySelf()));
        assertEquals(marge,
            folder.getStatistic()
                .getSizeInSync(getContollerMarge().getMySelf()));
        assertEquals(lisa,
            folder.getStatistic().getSizeInSync(getContollerLisa().getMySelf()));
        assertEquals(
            maggie,
            folder.getStatistic().getSizeInSync(
                getContollerMaggie().getMySelf()));
    }

    private void assertMemberSizesActual(long homer, long bart, long marge,
        long lisa, long maggie)
    {
        assertMemberSizesActual(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertMemberSizesActual(Folder folder, long homer, long bart,
        long marge, long lisa, long maggie)
    {
        assertEquals(homer,
            folder.getStatistic().getSize(getContollerHomer().getMySelf()));
        assertEquals(bart,
            folder.getStatistic().getSize(getContollerBart().getMySelf()));
        assertEquals(marge,
            folder.getStatistic().getSize(getContollerMarge().getMySelf()));
        assertEquals(lisa,
            folder.getStatistic().getSize(getContollerLisa().getMySelf()));
        assertEquals(maggie,
            folder.getStatistic().getSize(getContollerMaggie().getMySelf()));
    }

    private void assertSyncPercentages(double homer, double bart, double marge,
        double lisa, double maggie)
    {
        assertSyncPercentages(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertHasLastSyncDate(boolean homer, boolean bart,
        boolean marge, boolean lisa, boolean maggie)
    {
        assertEquals(homer, getFolderAtHomer().getLastSyncDate() != null);
        assertEquals(bart, getFolderAtBart().getLastSyncDate() != null);
        assertEquals(marge, getFolderAtMarge().getLastSyncDate() != null);
        assertEquals(lisa, getFolderAtLisa().getLastSyncDate() != null);
        assertEquals(maggie, getFolderAtMaggie().getLastSyncDate() != null);
    }

    private void assertSyncPercentages(Folder folder, double homer,
        double bart, double marge, double lisa, double maggie)
    {
        assertEquals(
            homer,
            folder.getStatistic().getSyncPercentage(
                getContollerHomer().getMySelf()));
        assertEquals(
            bart,
            folder.getStatistic().getSyncPercentage(
                getContollerBart().getMySelf()));
        assertEquals(
            marge,
            folder.getStatistic().getSyncPercentage(
                getContollerMarge().getMySelf()));
        assertEquals(
            lisa,
            folder.getStatistic().getSyncPercentage(
                getContollerLisa().getMySelf()));
        assertEquals(
            maggie,
            folder.getStatistic().getSyncPercentage(
                getContollerMaggie().getMySelf()));
    }

    private void assertTotalSize(long totalSize) {
        assertEquals(totalSize, getFolderAtHomer().getStatistic()
            .getTotalSize());
        assertEquals(totalSize, getFolderAtBart().getStatistic().getTotalSize());
        assertEquals(totalSize, getFolderAtMarge().getStatistic()
            .getTotalSize());
        assertEquals(totalSize, getFolderAtLisa().getStatistic().getTotalSize());
        assertEquals(totalSize, getFolderAtMaggie().getStatistic()
            .getTotalSize());
    }

    private void assertTotalFileCount(int nFiles) {
        assertEquals(nFiles + " expected. got: "
            + getFolderAtHomer().getKnownFiles(), nFiles, getFolderAtHomer()
            .getStatistic().getTotalFilesCount());
        assertEquals(nFiles, getFolderAtBart().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtMarge().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtLisa().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtMaggie().getStatistic()
            .getTotalFilesCount());
    }

    private void assertTotalSyncPercentage(double totalSync) {
        assertEquals(totalSync, getFolderAtHomer().getStatistic()
            .getAverageSyncPercentage());
        assertEquals(totalSync, getFolderAtBart().getStatistic()
            .getAverageSyncPercentage());
        assertEquals(totalSync, getFolderAtMarge().getStatistic()
            .getAverageSyncPercentage());
        assertEquals(totalSync, getFolderAtLisa().getStatistic()
            .getAverageSyncPercentage());
        assertEquals(totalSync, getFolderAtMaggie().getStatistic()
            .getAverageSyncPercentage());
    }

    private void assertIncomingFiles(int homer, int bart, int marge, int lisa,
        int maggie)
    {
        assertEquals(homer, getFolderAtHomer().getStatistic()
            .getIncomingFilesCount());
        assertEquals(bart, getFolderAtBart().getStatistic()
            .getIncomingFilesCount());
        assertEquals(marge, getFolderAtMarge().getStatistic()
            .getIncomingFilesCount());
        assertEquals(lisa, getFolderAtLisa().getStatistic()
            .getIncomingFilesCount());
        assertEquals(maggie, getFolderAtMaggie().getStatistic()
            .getIncomingFilesCount());
    }

    private void waitForDeletion(final String filename) {
        TestHelper.waitForCondition(10, new Condition() {
            @Override
            public boolean reached() {
                Path fileAtHomer = getFolderAtHomer().getLocalBase().resolve(
                    filename);
                FileInfo fInfoHomer = getFolderAtHomer().getFile(
                    FileInfoFactory.lookupInstance(getFolderAtHomer(),
                        fileAtHomer));
                Path fileAtMarge = getFolderAtMarge().getLocalBase().resolve(
                    filename);
                FileInfo fInfoMarge = getFolderAtMarge().getFile(
                    FileInfoFactory.lookupInstance(getFolderAtMarge(),
                        fileAtMarge));
                Path fileAtBart = getFolderAtBart().getLocalBase().resolve(
                    filename);
                FileInfo fInfoBart = getFolderAtBart().getFile(
                    FileInfoFactory.lookupInstance(getFolderAtBart(),
                        fileAtBart));
                Path fileAtLisa = getFolderAtLisa().getLocalBase().resolve(
                    filename);
                FileInfo fInfoLisa = getFolderAtLisa().getFile(
                    FileInfoFactory.lookupInstance(getFolderAtLisa(),
                        fileAtLisa));
                Path fileAtMaggier = getFolderAtMaggie()
                    .getLocalBase().resolve(filename);
                FileInfo fInfoMaggie = getFolderAtMaggie().getFile(
                    FileInfoFactory.lookupInstance(getFolderAtMaggie(),
                        fileAtMaggier));
                return Files.notExists(fileAtHomer) && fInfoHomer.isDeleted()
                    && Files.notExists(fileAtMarge) && fInfoMarge.isDeleted()
                    && Files.notExists(fileAtBart) && fInfoBart.isDeleted()
                    && Files.notExists(fileAtLisa) && fInfoLisa.isDeleted()
                    && Files.notExists(fileAtMaggier) && fInfoMaggie.isDeleted();
            }
        });
    }

    private void waitForFileListOnTestFolder() {
        waitForFilelistReceived(getFolderAtHomer());
        waitForFilelistReceived(getFolderAtBart());
        waitForFilelistReceived(getFolderAtMarge());
        waitForFilelistReceived(getFolderAtLisa());
        waitForFilelistReceived(getFolderAtMaggie());
    }

    /**
     * Waits till all Simpsons received the filelist of the given folder.
     *
     * @param folder
     */
    private void waitForFilelistReceived(final Folder folder) {
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            Collection<FileInfo> filesOnHomer;
            Collection<FileInfo> filesOnBart;
            Collection<FileInfo> filesOnMarge;
            Collection<FileInfo> filesOnLisa;
            Collection<FileInfo> filesOnMaggie;

            @Override
            public String message() {
                Collection<FileInfo> filesLocal = folder.getKnownFiles();
                return "Not identical! Filelist of "
                    + folder.getController().getMySelf().getNick()
                    + " was not received by all members. Homer: "
                    + filesOnHomer.size() + " (same:"
                    + identicalFileList(filesLocal, filesOnHomer) + "), Bart: "
                    + filesOnBart.size() + " (same:"
                    + identicalFileList(filesLocal, filesOnBart) + "), Marge: "
                    + filesOnMarge.size() + " (same:"
                    + identicalFileList(filesLocal, filesOnMarge) + "), Lisa: "
                    + filesOnLisa.size() + " (same:"
                    + identicalFileList(filesLocal, filesOnLisa)
                    + "), Maggie: " + filesOnMaggie.size() + "(same:"
                    + identicalFileList(filesLocal, filesOnMaggie) + ')';
            }

            @Override
            public boolean reached() {
                if (folder.getConnectedMembersCount() != 4) {
                    throw new RuntimeException("Not all members connected on "
                        + folder + ". Members: "
                        + Arrays.asList(folder.getConnectedMembers()));
                }
                Collection<FileInfo> filesLocal = folder.getKnownFiles();
                MemberInfo source = folder.getController().getMySelf()
                    .getInfo();

                filesOnHomer = getFileList(getContollerHomer(),
                    folder.getInfo(), source);
                filesOnBart = getFileList(getContollerBart(), folder.getInfo(),
                    source);
                filesOnMarge = getFileList(getContollerMarge(),
                    folder.getInfo(), source);
                filesOnLisa = getFileList(getContollerLisa(), folder.getInfo(),
                    source);
                filesOnMaggie = getFileList(getContollerMaggie(),
                    folder.getInfo(), source);

                return identicalFileList(filesLocal, filesOnHomer)
                    && identicalFileList(filesLocal, filesOnBart)
                    && identicalFileList(filesLocal, filesOnMarge)
                    && identicalFileList(filesLocal, filesOnLisa)
                    && identicalFileList(filesLocal, filesOnMaggie);
            }
        });
    }

    private static Collection<FileInfo> getFileList(Controller controller,
        FolderInfo foInfo, MemberInfo source)
    {
        Folder folder = controller.getFolderRepository().getFolder(foInfo);
        return folder.getFilesAsCollection(source.getNode(controller, false));
    }

    private static boolean identicalFileList(Collection<FileInfo> filesA,
        Collection<FileInfo> filesB)
    {
        if (filesA == null || filesB == null) {
            return false;
        }
        if (filesA.size() != filesB.size()) {
            return false;
        }
        List<FileInfo> listA = new ArrayList<FileInfo>(filesA);
        List<FileInfo> listB = new ArrayList<FileInfo>(filesB);
        for (FileInfo fileB : listB) {
            int atA = listA.indexOf(fileB);
            if (atA < 0) {
                return false;
            }
            FileInfo fileA = listA.get(atA);
            if (!fileA.isVersionDateAndSizeIdentical(fileB)
                && fileA.getModifiedBy().equals(fileB.getModifiedBy()))
            {
                return false;
            }
        }
        return true;
    }

    private void assertAllInSync(int totalFiles, long totalSize) {
        assertTotalFileCount(totalFiles);
        assertTotalSize(totalSize);
        assertTotalSyncPercentage(100);
        assertMemberSizesInSync(totalSize, totalSize, totalSize, totalSize,
            totalSize);
        assertMemberSizesActual(totalSize, totalSize, totalSize, totalSize,
            totalSize);
        assertSyncPercentages(100, 100, 100, 100, 100);
        assertIncomingFiles(0, 0, 0, 0, 0);
    }
}
