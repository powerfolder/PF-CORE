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
 * $Id: PatternMatchTest.java 8022 2009-05-21 07:46:07Z harry $
 */
package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.pattern.CompiledPattern;

public class PatternMatchTest extends TestCase {

    public void testPatterns() {
        assertTrue(new CompiledPattern("sdfgkjh").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("sdfgkjh").isMatch("SdFgKjH"));
        assertTrue(new CompiledPattern("SdFgKjH").isMatch("sdfgkjh"));
        assertFalse(new CompiledPattern("sdfxxxxxgkjh").isMatch("sdfgkjh"));
        assertFalse(new CompiledPattern("sdfgkjh").isMatch("sdfxxxxxgkjh"));

        assertTrue(new CompiledPattern("sdf*gkjh").isMatch("sdfxxxxxgkjh"));
        assertTrue(new CompiledPattern("sdf*gkjh").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("sdf*g*h").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("*").isMatch(""));

        assertTrue(new CompiledPattern("*file*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*test*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*test*/*name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c*/*/*name")
            .isMatch("c:/test/file.name"));
        assertFalse(new CompiledPattern("c*/huh/*name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c:/*.name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c:/*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*.name").isMatch("c:/test/file.name"));

        assertTrue(new CompiledPattern("c:\\*").isMatch("c:\\test\\file.name"));
    }

    public void testCompiledPatterns() {
        assertTrue(new CompiledPattern("sdfgkjh").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("sdfgkjh").isMatch("SdFgKjH"));
        assertTrue(new CompiledPattern("SdFgKjH").isMatch("sdfgkjh"));
        assertFalse(new CompiledPattern("sdfxxxxxgkjh").isMatch("sdfgkjh"));
        assertFalse(new CompiledPattern("sdfgkjh").isMatch("sdfxxxxxgkjh"));

        assertTrue(new CompiledPattern("sdf*gkjh").isMatch("sdfxxxxxgkjh"));
        assertTrue(new CompiledPattern("sdf*gkjh").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("sdf*g*h").isMatch("sdfgkjh"));
        assertTrue(new CompiledPattern("*").isMatch(""));
        // Disabled since filenames usually DON'T start/end with spaces #1705

        // assertTrue(new CompilingPatternMatch("*").isMatch(" "));

        assertTrue(new CompiledPattern("*file*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*test*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*test*/*name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c*/*/*name")
            .isMatch("c:/test/file.name"));
        assertFalse(new CompiledPattern("c*/huh/*name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c:/*.name")
            .isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c:/*").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*.name").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*name").isMatch("c:/test/file.name"));
        assertFalse(new CompiledPattern("name").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("c:/test*").isMatch("c:/test/file.name"));
        assertFalse(new CompiledPattern("c:/test").isMatch("c:/test/file.name"));
        assertTrue(new CompiledPattern("*thumbs.db")
            .isMatch("c:/test/file.name/Thumbs.db"));

        assertTrue(new CompiledPattern("c:\\*").isMatch("c:\\test\\file.name"));
        assertTrue(new CompiledPattern("x\\~!@#$%^-&()_+={}][:';,.<>|y")
            .isMatch("x\\~!@#$%^-&()_+={}][:';,.<>|y"));
        assertTrue(new CompiledPattern(
            "a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*a*ab")
            .isMatch("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab"));
    }

    public void testPerformance() {
        Profiling.setEnabled(true);

        CompiledPattern p1 = new CompiledPattern("SdFgKjH");
        CompiledPattern p2 = new CompiledPattern("sdf*g*h");
        CompiledPattern p3 = new CompiledPattern("*");
        CompiledPattern p4 = new CompiledPattern("*test*/*name");
        CompiledPattern p5 = new CompiledPattern("c*/huh/*name");
        CompiledPattern p6 = new CompiledPattern("*thumbs.db");
        ProfilingEntry pe = Profiling.start("CompiledPattern");
        for (int i = 0; i < 1000000; i++) {
            p1.isMatch("C:\\Programme\\test\\Thumbs.db");
            p2.isMatch("C:\\Programme\\test\\Thumbs.db");
            p3.isMatch("C:\\Programme\\test\\Thumbs.db");
            p4.isMatch("C:\\Programme\\test\\Thumbs.db");
            p5.isMatch("C:\\Programme\\test\\Thumbs.db");
            p6.isMatch("C:\\Programme\\test\\Thumbs.db");
        }
        Profiling.end(pe);

        pe = Profiling.start("CompiledPatternThumbsDB");
        CompiledPattern cpThumbs = new CompiledPattern(Folder.THUMBS_DB);
        for (int i = 0; i < 10000000; i++) {
            assertTrue(cpThumbs.isMatch("C:\\Programme\\test\\Thumbs.db"));
            assertFalse(cpThumbs.isMatch("C:\\Programme\\test\\Testfile"));
        }
        Profiling.end(pe);

        System.err.println(Profiling.dumpStats());
    }
}
