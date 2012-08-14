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
 * $Id: MassDeletionTest.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the correct response to mass deletions.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry</a>
 * @version $Revision: 4.0 $
 */
public class MassDeletionTest extends TwoControllerTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testSmallMassDeletion() throws Exception {

        // Check with no protection
        massDeletion(false, 100, true);

        tearDown();
        setUp();

        // Check with protection
        massDeletion(true, 100, true);
    }

    /**
     * Test that delete does not have mass delete protection if not expert mode.
     *
     * @throws Exception
     */
    public void testNonExpertMassDeletion() throws Exception {

        // Check with no protection
        massDeletion(false, 100, false);

        tearDown();
        setUp();

        // Check with protection
        try {
            massDeletion(true, 100, false);
            fail("Should not have mass delete protection in non-expert mode.");
        } catch (RuntimeException e) {
            // Assertion exception. Expecting this. No problem.
        }
    }

    public void testLargeMassDeletion() throws Exception {

        // Check with no protection
        massDeletion(false, 2000, true);

        tearDown();
        setUp();

        // Check with protection
        massDeletion(true, 2000, true);
    }

    /**
     * #1842
     * 
     * @throws Exception
     */
    public void testNotTriggerOnHistoricDeletion() throws Exception {

        // Check with no protection
        massDeletion(false, 1000, true);

        ConfigurationEntry.MASS_DELETE_THRESHOLD.setValue(getFolderAtLisa()
            .getController(), 10);
        ConfigurationEntry.MASS_DELETE_PROTECTION.setValue(getFolderAtLisa()
            .getController(), true);

        disconnectBartAndLisa();
        connectBartAndLisa();

        // Should be STILL correct sync profile. Mass deletion should not have
        // been triggered.
        assertEquals(getFolderAtLisa().getSyncProfile(),
            SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    public void massDeletion(boolean protection, final int nFiles,
                             boolean expert) throws Exception {

        PreferencesEntry.EXPERT_MODE.setValue(getContollerBart(), expert);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), expert);

        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        ConfigurationEntry.MASS_DELETE_THRESHOLD.setValue(getFolderAtLisa()
            .getController(), 80);
        ConfigurationEntry.MASS_DELETE_PROTECTION.setValue(getFolderAtLisa()
            .getController(), protection);

        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());
        assertEquals("Known files at bart: "
            + getFolderAtBart().getKnownItemCount(), nFiles, getFolderAtBart()
            .getKnownItemCount());
        scanFolder(getFolderAtLisa());
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() == nFiles;
            }
        });
        assertEquals(nFiles, getFolderAtLisa().getKnownFiles().size());

        // Delete all Bart's files
        for (final File file : getFolderAtBart().getLocalBase().listFiles()) {
            if (!file.isDirectory()) {
                assertTrue(file.delete());
            }
        }

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        if (protection) {
            // Files should survive and profile switch to HOST_FILE.
            TestHelper.waitForCondition(20, new ConditionWithMessage() {
                public boolean reached() {
                    return getFolderAtLisa().getLocalBase().listFiles().length == nFiles + 1;
                    // The files + .PowerFolder dir
                }

                public String message() {
                    return "Folder at lisa has files:"
                        + getFolderAtLisa().getLocalBase().listFiles().length;
                }
            });
            System.out.println("Protection: " + protection);
            System.out.println("Lisa's sync profile: "
                + getFolderAtLisa().getSyncProfile().getName());
            System.out.println("Required sync profile: "
                + SyncProfile.HOST_FILES.getName());
            TestHelper.waitForCondition(40, new ConditionWithMessage() {
                public boolean reached() {
                    return getFolderAtLisa().getSyncProfile().equals(
                        SyncProfile.HOST_FILES);
                }

                public String message() {
                    return "Sync profile was not auto-switched to Host files";
                }
            });
            assertEquals(getFolderAtLisa().getSyncProfile(),
                SyncProfile.HOST_FILES);
        } else {
            // Files should have been deleted and profile remains same.
            TestHelper.waitForCondition(40, new Condition() {
                public boolean reached() {
                    return getFolderAtLisa().getLocalBase().listFiles().length == 1;
                    // The .PowerFolder dir
                }
            });
            System.out.println("Protection: " + protection);
            System.out.println("Lisa's sync profile: "
                + getFolderAtLisa().getSyncProfile().getName());
            System.out.println("Required sync profile: "
                + SyncProfile.AUTOMATIC_SYNCHRONIZATION.getName());
            TestHelper.waitForCondition(40, new ConditionWithMessage() {
                public boolean reached() {
                    return getFolderAtLisa().getSyncProfile().equals(
                        SyncProfile.AUTOMATIC_SYNCHRONIZATION);
                }

                public String message() {
                    return "Sync profile was auto-switched from auto sync";
                }
            });
            assertEquals(getFolderAtLisa().getSyncProfile(),
                SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        }

    }

}