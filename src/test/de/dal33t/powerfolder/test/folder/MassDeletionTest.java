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
        massDeletion(false, 100);

        tearDown();
        setUp();

        // Check with protection
        massDeletion(true, 100);
    }

    public void testLargeMassDeletion() throws Exception {

        // Check with no protection
        massDeletion(false, 2000);

        tearDown();
        setUp();

        // Check with protection
        massDeletion(true, 2000);
    }

    public void massDeletion(boolean protection, final int size) throws Exception {

        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        ConfigurationEntry.MASS_DELETE_THRESHOLD.setValue(getFolderAtLisa()
            .getController(), 80);
        ConfigurationEntry.MASS_DELETE_PROTECTION.setValue(getFolderAtLisa()
            .getController(), protection);

        for (int i = 0; i < size; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }

        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(40, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtBart().getKnownItemCount() == size;
            }

            public String message() {
                return "Known files at bart: "
                    + getFolderAtBart().getKnownItemCount();
            }
        });

        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() == size;
            }
        });

        assertEquals(size, getFolderAtLisa().getKnownFiles().size());

        // Delete all Bart's files
        for (final File file : getFolderAtBart().getLocalBase().listFiles()) {
            if (!file.isDirectory()) {
                TestHelper.waitForCondition(40, new Condition() {
                    public boolean reached() {
                        return file.delete();
                    }
                });
            }
        }

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        if (protection) {
            // Files should survive and profile switch to HOST_FILE.
            TestHelper.waitForCondition(40, new ConditionWithMessage() {
                public boolean reached() {
                    return getFolderAtLisa().getLocalBase().listFiles().length == size + 1; // The
                    // files
                    // +
                    // .PowerFolder
                    // dir
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
                    return getFolderAtLisa().getLocalBase().listFiles().length == 1; // The
                    // .PowerFolder
                    // dir
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