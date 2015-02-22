/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Util;

public class UtilTest extends TestCase {

    public void testBetweenVersions() {
        assertFalse(Util.betweenVersion("8.1.0", "8.0.0", "8.2.20"));
        assertTrue(Util.betweenVersion("8.1.0", "8.1.0", "8.2.20"));
        assertTrue(Util.betweenVersion("8.1.0", "8.2.11", "8.2.20"));
        assertFalse(Util.betweenVersion("8.1.0", "8.3.0", "8.2.20"));

        assertFalse(Util.betweenVersion("8.1.0", "8.2.1", "8.2.0"));
    }

}
