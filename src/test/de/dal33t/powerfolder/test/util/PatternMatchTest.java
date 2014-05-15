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
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.pattern.CompiledPattern;
import de.dal33t.powerfolder.util.pattern.DefaultExcludes;
import de.dal33t.powerfolder.util.pattern.EndMatchPattern;
import de.dal33t.powerfolder.util.pattern.ExactMatchPattern;
import de.dal33t.powerfolder.util.pattern.OfficeTempFilesMatchPattern;
import de.dal33t.powerfolder.util.pattern.Pattern;
import de.dal33t.powerfolder.util.pattern.PatternFactory;
import de.dal33t.powerfolder.util.pattern.StartMatchPattern;

public class PatternMatchTest extends TestCase {

    public void testPatternFactory() {
        Pattern p = PatternFactory.createPattern("dsdkjskj");
        assertTrue(p instanceof ExactMatchPattern);
        p = PatternFactory.createPattern("dsdkjskj*");
        assertTrue(p instanceof StartMatchPattern);
        p = PatternFactory.createPattern("*dsdkjskj");
        assertTrue(p instanceof EndMatchPattern);
        p = PatternFactory.createPattern("dsd*kjskj");
        assertTrue(p instanceof CompiledPattern);
        p = PatternFactory.createPattern(DefaultExcludes.OFFICE_TEMP
            .getPattern());
        assertTrue(p instanceof OfficeTempFilesMatchPattern);
    }

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

    public void testEndMatchPattern() {
        try {
            new EndMatchPattern("sdfgkjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new EndMatchPattern("sdfgkjh*");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new EndMatchPattern("sdfg*kjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new EndMatchPattern("*sdfg*kjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }

        assertFalse(new EndMatchPattern("*sdfgkjh").isMatch("fgkjh"));
        assertFalse(new EndMatchPattern("*sdfgkjh").isMatch("dfgkjh"));
        assertTrue(new EndMatchPattern("*sdfgkjh").isMatch("sdfgkjh"));
        assertTrue(new EndMatchPattern("*sdfgkjh").isMatch("SdFgKjH"));
        assertTrue(new EndMatchPattern("*SdFgKjH").isMatch("sdfgkjh"));
        assertFalse(new EndMatchPattern("*sdfxxxxxgkjh").isMatch("sdfgkjh"));
        assertFalse(new EndMatchPattern("*sdfgkjh").isMatch("sdfxxxxxgkjh"));

        assertFalse(new EndMatchPattern("*sdfgkjh").isMatch("sdfxxxxxgkjh"));
        assertTrue(new EndMatchPattern("*sdfgkjh").isMatch("sdfgkjh"));
        assertFalse(new EndMatchPattern("*sdfgh").isMatch("sdfgkjh"));
        // Useless
        assertTrue(new EndMatchPattern("*").isMatch(""));

        assertFalse(new EndMatchPattern("*file").isMatch("c:/test/file.name"));
        assertFalse(new EndMatchPattern("*test").isMatch("c:/test/file.name"));
        assertFalse(new EndMatchPattern("*test/name")
            .isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*name").isMatch("c:/test/file.name"));
        assertFalse(new EndMatchPattern("*/huh/name")
            .isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*.name").isMatch("c:/test/file.name"));
        assertFalse(new EndMatchPattern("*c:/").isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*.name").isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*name").isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*name").isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*test/file.name")
            .isMatch("c:/test/file.name"));
        assertFalse(new EndMatchPattern("*test").isMatch("c:/test/file.name"));
        assertTrue(new EndMatchPattern("*thumbs.db")
            .isMatch("c:/test/file.name/Thumbs.db"));

        assertTrue(new EndMatchPattern("*x\\~!@#$%^-&()_+={}][:';,.<>|y")
            .isMatch("x\\~!@#$%^-&()_+={}][:';,.<>|y"));
        assertTrue(new EndMatchPattern("*aab")
            .isMatch("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab"));
        assertTrue(new EndMatchPattern("*.tmp")
            .isMatch("Eigene Musik/iTunes/iT 3.tmp"));
    }

    public void testStartMatchPattern() {
        try {
            new StartMatchPattern("sdfgkjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new StartMatchPattern("*sdfgkjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new StartMatchPattern("sdfg*kjh");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }
        try {
            new StartMatchPattern("sdfg*kjh*");
            fail("Illegal construction possible");
        } catch (Exception e) {
        }

        assertFalse(new StartMatchPattern("sdfgkjh*").isMatch("sdf"));
        assertTrue(new StartMatchPattern("sdfgkjh*").isMatch("sdfgkjh"));
        assertTrue(new StartMatchPattern("sdfgkjh*").isMatch("SdFgKjH"));
        assertTrue(new StartMatchPattern("SdFgKjH*").isMatch("sdfgkjh"));
        assertFalse(new StartMatchPattern("sdfxxxxxgkjh*").isMatch("sdfgkjh"));
        assertFalse(new StartMatchPattern("sdfgkjh*").isMatch("sdfxxxxxgkjh"));

        assertFalse(new StartMatchPattern("sdfgkjh*").isMatch("sdfxxxxxgkjh"));
        assertTrue(new StartMatchPattern("sdfgkjh*").isMatch("sdfgkjh"));
        assertFalse(new StartMatchPattern("sdfgh*").isMatch("sdfgkjh"));
        assertTrue(new StartMatchPattern("*").isMatch(""));

        assertFalse(new StartMatchPattern("file*").isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern("test*").isMatch("c:/test/file.name"));
        assertTrue(new StartMatchPattern("test/*").isMatch("test/file.name"));
        assertFalse(new StartMatchPattern("test/name*")
            .isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern("name*").isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern("/huh/name*")
            .isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern(".name*")
            .isMatch("c:/test/file.name"));
        assertTrue(new StartMatchPattern("c:/*").isMatch("c:/test/file.name"));
        assertTrue(new StartMatchPattern("c:/test/file.name*")
            .isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern("test*").isMatch("c:/test/file.name"));
        assertFalse(new StartMatchPattern("thumbs.db*")
            .isMatch("c:/test/file.name/Thumbs.db"));

        assertTrue(new StartMatchPattern("x\\~!@#$%^-&()_+={}][:';,.<>|y*")
            .isMatch("x\\~!@#$%^-&()_+={}][:';,.<>|y"));
        assertTrue(new StartMatchPattern("baaaa*")
            .isMatch("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    public void testPerformance() {
        Profiling.setEnabled(true);

        Pattern p1 = new CompiledPattern("SdFgKjH");
        Pattern p2 = new StartMatchPattern("sdfgh*");
        Pattern p3 = new CompiledPattern("*");
        Pattern p4 = new CompiledPattern("*test*/*name");
        Pattern p5 = new CompiledPattern("c*/huh/*name");
        Pattern p6 = new EndMatchPattern("*thumbs.db");
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

        int nPatterns = 10000000;
        pe = Profiling.start("CompiledPatternThumbsDB");
        Pattern pattern = new CompiledPattern(
            DefaultExcludes.THUMBS_DB.getPattern());
        for (int i = 0; i < nPatterns; i++) {
            assertTrue(pattern.isMatch("C:\\Programme\\test\\Thumbs.db"));
            assertFalse(pattern.isMatch("C:\\Programme\\test\\Testfile"));
        }
        Profiling.end(pe);

        pe = Profiling.start("EndMatchPatternThumbsDB");
        pattern = new EndMatchPattern(DefaultExcludes.THUMBS_DB.getPattern());
        for (int i = 0; i < nPatterns; i++) {
            assertTrue(pattern.isMatch("C:\\Programme\\test\\Thumbs.db"));
            assertFalse(pattern.isMatch("C:\\Programme\\test\\Testfile"));
        }
        Profiling.end(pe);

        pe = Profiling.start("StartMatchPatternThumbsDB");
        pattern = new StartMatchPattern("programme\\test\\*");
        for (int i = 0; i < nPatterns; i++) {
            assertTrue(pattern.isMatch("Programme\\test\\Thumbs.db"));
            assertFalse(pattern.isMatch("Users\\test\\Thumbs.db"));
        }
        Profiling.end(pe);

        pe = Profiling.start("OfficeTempMatchPatternThumbsDB");
        pattern = new OfficeTempFilesMatchPattern("~", "*.tmp");
        for (int i = 0; i < nPatterns; i++) {
            assertFalse(pattern.isMatch("Programme\\test\\Thumbs.db"));
            assertTrue(pattern.isMatch("Users\\test\\~W8833453.tmp"));
        }

        pe = Profiling.start("OfficeXTempMatchPatternThumbsDB");
        pattern = new OfficeTempFilesMatchPattern("~$", "*");
        for (int i = 0; i < nPatterns; i++) {
            assertFalse(pattern.isMatch("Programme\\test\\Thumbs.db"));
            assertTrue(pattern.isMatch("Users\\test\\~$LAST Quotation.xlsx"));
        }
        Profiling.end(pe);
        System.err.println(Profiling.dumpStats());
    }
}
