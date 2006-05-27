/* $Id: VersionCompareTest.java,v 1.1.2.1 2006/04/29 00:27:15 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.junit.util;

import de.dal33t.powerfolder.util.Util;
import junit.framework.TestCase;

/**
 * Test the version string compare of Util
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.1.2.1 $
 */
public class VersionCompareTest extends TestCase {
    public void testCompare() {
        System.out.println("VersionCompareTest");
        assertTrue(Util.compareVersions("1", "0.9.3"));
        assertTrue(Util.compareVersions("1 devel", "0.9.3"));
        assertFalse(Util.compareVersions("0.3 devel", "0.9.3"));
        assertFalse(Util.compareVersions("0.3.0", "0.9.3"));
        assertFalse(Util.compareVersions("0.3", "0.9.3"));
        assertFalse(Util.compareVersions("0", "0.9.3"));
        assertFalse(Util.compareVersions("0.9.3", "0.9.3"));
        assertTrue(Util.compareVersions("1.0.0", "0.9.3"));
        assertFalse(Util.compareVersions("0.9.3", "1.0.0"));
        
        assertTrue(Util.compareVersions("0.9.3", "0.9.3 devel"));
        assertTrue(Util.compareVersions("0.9.4", "0.9.3 devel"));
        assertFalse(Util.compareVersions("1.0.1 devel", "1.0.1"));
        assertTrue(Util.compareVersions("1.0.1", "1.0.0"));
    }
}
