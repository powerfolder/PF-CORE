/*
* Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
* $Id: OSUtilTest.java 0 2010-11-24 19:25:09Z akl $
*/
package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.os.OSUtil;

public class OSUtilTest extends TestCase {
    /**
     * Successful on OS X 10.6 only, therefore disabled by default.
     */
    public void testFailingtestIsMacOSSnowLeopardOrNewer() {
        if (!OSUtil.isMacOS()) {
            return;
        }
        boolean isMacOS = OSUtil.isMacOSSnowLeopardOrNewer();
        assertEquals(true, isMacOS);
    }
}
