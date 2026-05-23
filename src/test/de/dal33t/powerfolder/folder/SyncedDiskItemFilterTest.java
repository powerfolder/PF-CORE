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
package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.pattern.DefaultExcludes;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class SyncedDiskItemFilterTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().addDefaultExcludes();
        getFolderAtLisa().addDefaultExcludes();

        assertEquals(getFolderAtBart().getDiskItemFilter().getPatterns().toString(),
                DefaultExcludes.values().length, getFolderAtBart().getDiskItemFilter().getPatterns().size());
        assertEquals(getFolderAtLisa().getDiskItemFilter().getPatterns().toString(),
                DefaultExcludes.values().length, getFolderAtLisa().getDiskItemFilter().getPatterns().size());
    }

    public void testSyncExcludes() {
        final String testPattern = "xxx";
        getFolderAtBart().addPattern(testPattern);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getDiskItemFilter().getPatterns()
                    .contains(testPattern);
            }

            public String message() {
                return "Lisa did not sync ignore patterns: "
                    + getFolderAtLisa().getDiskItemFilter().getPatterns();
            }
        });
    }
}
