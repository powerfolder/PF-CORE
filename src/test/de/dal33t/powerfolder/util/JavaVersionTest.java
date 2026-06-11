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
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.util.JavaVersion;

/**
 * Test cases for JavaVersion.
 */
public class JavaVersionTest {

    /**
     * Test that we can read the java.runtime.version.
     */
    @Test
    public void testSystemVersion() {
        if (JavaVersion.systemVersion().isOpenJDK()) {
            return;
        }
        String runtimeVersion = System.getProperty("java.runtime.version");
        String expected = runtimeVersion.split("\\+")[0];
        assertEquals(expected,
                JavaVersion.systemVersion().toString(), "System Version");
    }

    /**
     * Test the constructors work.
     */
    @Test
    public void testConstructors() {
        JavaVersion version = new JavaVersion(1, 6, 22, 10, 123);
        assertEquals("1.6.22_10-b123", version.toString(), "Five parameters");

        version = new JavaVersion(1, 99, 2, 10);
        assertEquals("1.99.2_10", version.toString(), "Four parameters");

        version = new JavaVersion(111, 6, 2);
        assertEquals("111.6.2", version.toString(), "Three parameters");

        version = new JavaVersion(1, 786);
        assertEquals("1.786.0", version.toString(), "Two parameters");
    }

    /**
     * Test that parse works, and can handle bad text.
     */
    @Test
    public void testParse() {

        assertEquals("2.6.0", JavaVersion.parse("2.6").toString(), "Parse two");
        assertEquals("1.6.2", JavaVersion.parse("1.6.2").toString(), "Parse three");
        assertEquals("1.6.2_9", JavaVersion.parse("1.6.2_9").toString(), "Parse four");
        assertEquals("1.6.0_10-b12", JavaVersion.parse("1.6.0_10-b12").toString(), "Parse five");

        // Junk text
        assertEquals("0.0.0", JavaVersion.parse("complete junk").toString());
        assertEquals("0.0.0", JavaVersion.parse("").toString());
        assertEquals("0.0.0", JavaVersion.parse(null).toString());
        assertEquals("0.0.0", JavaVersion.parse("complete junk").toString());
    }

    /**
     * Test of compare, equals and hashCode.
     */
    @Test
    public void testCompareEqualsHash() {
        JavaVersion version = JavaVersion.parse("1.5.2_8-b03");
        JavaVersion versionNotEqual = JavaVersion.parse("1.5.2");
        JavaVersion versionEqual = JavaVersion.parse("1.5.2_8-b03");

        assertTrue( version.compareTo(versionNotEqual) > 0,"Compare ne");
        assertFalse( version.equals(versionNotEqual),"Equals ne");
        assertFalse( version.hashCode()
                == versionNotEqual.hashCode(),"HashCode ne");

        assertTrue( version.compareTo(versionEqual) == 0,"Compare eq");
        assertTrue( version.equals(versionEqual),"Equals eq");
        assertTrue( version.hashCode()
                == versionEqual.hashCode(),"HashCode eq");
    }

    /**
     * Test non-standard OpenJDK version format.
     */
    @Test
    public void testOpenJDKVersion() {
        // OpenJDK Client VM - like 1.6.0-b09
        JavaVersion openJDKVersion = JavaVersion.parse("1.6.0-b09");
        JavaVersion normalVersion = JavaVersion.parse("1.6.0");
        assertTrue( openJDKVersion.compareTo(normalVersion) > 0,"Compare eq");
        assertFalse( openJDKVersion.equals(normalVersion),"Equals eq");
        assertFalse( openJDKVersion.hashCode() ==
                normalVersion.hashCode(),"HashCode eq");
        JavaVersion v14 = JavaVersion.parse("14.0.1+7");
        assertEquals(14, v14.getMajor());
        assertEquals(0, v14.getMinor());
        assertEquals(1, v14.getRevision());
        JavaVersion v15 = JavaVersion.parse("15+36-1562");
        assertEquals(15, v15.getMajor());
        assertEquals(0, v15.getMinor());
        assertEquals(0, v15.getRevision());
    }

    /**
     * Test that the system version is a single instance.
     */
    @Test
    public void testSystemSingleton() {
        JavaVersion javaVersion1 = JavaVersion.systemVersion();
        JavaVersion javaVersion2 = JavaVersion.systemVersion();
        assertTrue( javaVersion1 == javaVersion2
                && javaVersion1.equals(javaVersion2),"Singleton");
    }
}
