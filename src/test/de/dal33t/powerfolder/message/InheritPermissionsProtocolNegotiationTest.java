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
 *
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * PFC-3543: Live test that two connected controllers negotiate the correct
 * protocol version depending on whether the "interruption of permission
 * inheritance" feature is enabled.
 * <p>
 * The feature flag is process-wide, so both controllers necessarily share the
 * same state; this verifies the two endpoints of the negotiation:
 * <ul>
 *   <li>feature OFF -&gt; legacy protocol 114 (compatible with old servers/clients)</li>
 *   <li>feature ON  -&gt; protocol 115 (inheritsPermissions capable)</li>
 * </ul>
 * The mixed on/off case is covered deterministically by
 * {@code FolderInfoInheritsPermissionsTest}.
 */
public class InheritPermissionsProtocolNegotiationTest extends TwoControllerTestCase {

    @Override
    protected void tearDown() throws Exception {
        // The feature flag is process-wide - never leak it into other tests.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    public void testFeatureDisabledNegotiatesLegacyProtocol() {
        // setUp() already disabled all features via Feature.setupForTests().
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        connectBartAndLisa();
        assertNegotiatedProtocolVersion(Identity.PROTOCOL_VERSION_114);
    }

    public void testFeatureEnabledNegotiatesInheritanceProtocol() {
        // Enable BEFORE connecting so the exchanged Identity advertises 115.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        assertNegotiatedProtocolVersion(Identity.PROTOCOL_VERSION_115);
    }

    private void assertNegotiatedProtocolVersion(int expected) {
        final Member lisaAtBart = getContollerBart().getNodeManager()
            .getNode(getContollerLisa().getMySelf().getId());
        final Member bartAtLisa = getContollerLisa().getNodeManager()
            .getNode(getContollerBart().getMySelf().getId());
        assertNotNull("Lisa is not known at Bart", lisaAtBart);
        assertNotNull("Bart is not known at Lisa", bartAtLisa);

        // Wait until the handshake (incl. Identity exchange) has completed.
        TestHelper.waitForCondition(10, new Condition() {
            @Override
            public boolean reached() {
                return lisaAtBart.isConnected() && bartAtLisa.isConnected()
                    && lisaAtBart.getProtocolVersion() > 0
                    && bartAtLisa.getProtocolVersion() > 0;
            }
        });

        assertEquals("Unexpected protocol version negotiated at Bart",
            expected, lisaAtBart.getProtocolVersion());
        assertEquals("Unexpected protocol version negotiated at Lisa",
            expected, bartAtLisa.getProtocolVersion());
    }
}