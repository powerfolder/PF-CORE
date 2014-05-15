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
 * $Id: JavaVersionTest.java 6419 2009-01-19 01:03:34Z tot $
 */
package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.JavaVersion;

/**
 * Test cases for JavaVersion.
 */
public class JavaVersionTest extends TestCase {

    /**
     * Test that we can read the java.runtime.version.
     */
    public void testSystemVersion() {
        assertEquals("System Version",
                JavaVersion.systemVersion().toString(),
                System.getProperty("java.runtime.version"));
    }

    /**
     * Test the constructors work.
     */
    public void testConstructors() {
        JavaVersion version = new JavaVersion(1, 6, 22, 10, 123);
        assertEquals("Five parameters", version.toString(), "1.6.22_10-b123");

        version = new JavaVersion(1, 99, 2, 10);
        assertEquals("Four parameters", version.toString(), "1.99.2_10");

        version = new JavaVersion(111, 6, 2);
        assertEquals("Three parameters", version.toString(), "111.6.2");

        version = new JavaVersion(1, 786);
        assertEquals("Two parameters", version.toString(), "1.786.0");
    }

    /**
     * Test that parse works, and can handle bad text.
     */
    public void testParse() {
        assertEquals("Parse five", JavaVersion.parse("1.6.0_10-b12").toString(),
                "1.6.0_10-b12");
        assertEquals("Parse four", JavaVersion.parse("1.6.2_9").toString(),
                "1.6.2_9");
        assertEquals("Parse three", JavaVersion.parse("1.6.6").toString(),
                "1.6.6");
        assertEquals("Parse two", JavaVersion.parse("2.6").toString(), "2.6.0");

        // Swap '_' and '-'
        boolean threwError = false;
        try {
            JavaVersion.parse("1.6.0-10_b12");
        } catch (Exception e) {
            threwError = true;
        }
        assertTrue("Throwing error - swap", threwError);

        // No 'b'
        threwError = false;
        try {
            JavaVersion.parse("1.6.0_10-12");
        } catch (Exception e) {
            threwError = true;
        }
        assertTrue("Throwing error - b", threwError);

        // Junk text
        threwError = false;
        try {
            JavaVersion.parse("complete junk");
        } catch (Exception e) {
            threwError = true;
        }
        assertTrue("Throwing error - junk", threwError);

        // Empty text
        threwError = false;
        try {
            JavaVersion.parse("");
        } catch (Exception e) {
            threwError = true;
        }
        assertTrue("Throwing error - empty", threwError);

        // Null text
        threwError = false;
        try {
            JavaVersion.parse(null);
        } catch (Exception e) {
            threwError = true;
        }
        assertTrue("Throwing error - null", threwError);
    }

    /**
     * Test of compare, equals and hashCode.
     */
    public void testCompareEqualsHash() {
        JavaVersion version = JavaVersion.parse("1.5.2_8-b03");
        JavaVersion versionNotEqual = JavaVersion.parse("1.5.2");
        JavaVersion versionEqual = JavaVersion.parse("1.5.2_8-b03");

        assertTrue("Compare ne", version.compareTo(versionNotEqual) > 0);
        assertFalse("Equals ne", version.equals(versionNotEqual));
        assertFalse("HashCode ne", version.hashCode()
                == versionNotEqual.hashCode());

        assertTrue("Compare eq", version.compareTo(versionEqual) == 0);
        assertTrue("Equals eq", version.equals(versionEqual));
        assertTrue("HashCode eq", version.hashCode()
                == versionEqual.hashCode());
    }

    /**
     * Test non-standard OpenJDK version format.
     */
    public void testOpenJDKVersion() {
        // OpenJDK Client VM - like 1.6.0-b09
        JavaVersion openJDKVersion = JavaVersion.parse("1.6.0-b09");
        JavaVersion normalVersion = JavaVersion.parse("1.6.0");
        assertTrue("Compare eq", openJDKVersion.compareTo(normalVersion) == 0);
        assertTrue("Equals eq", openJDKVersion.equals(normalVersion));
        assertTrue("HashCode eq", openJDKVersion.hashCode() ==
                normalVersion.hashCode());
    }

    /**
     * Test that the system version is a single instance.
     */
    public void testSystemSingleton() {
        JavaVersion javaVersion1 = JavaVersion.systemVersion();
        JavaVersion javaVersion2 = JavaVersion.systemVersion();
        assertTrue("Singleton", javaVersion1 == javaVersion2
                && javaVersion1.equals(javaVersion2));
    }
}
