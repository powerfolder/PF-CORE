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
package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.Feature;
import junit.framework.TestCase;

public class FeatureTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        Feature.OS_CLIENT.enable();
        Feature.EXIT_ON_SHUTDOWN.enable();
    }

    public void testFeatureDefaults() {
        assertTrue(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());
    }

    public void testFeatureChange() {
        Feature.OS_CLIENT.disable();
        assertFalse(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());

        Feature.OS_CLIENT.enable();
        assertTrue(Feature.OS_CLIENT.isEnabled());
    }
}
