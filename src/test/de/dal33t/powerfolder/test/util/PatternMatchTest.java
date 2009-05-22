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
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.CompilingPatternMatch;

public class PatternMatchTest extends TestCase {

    public void testPatterns() {
        assertTrue(PatternMatch.isMatch("sdfgkjh", "sdfgkjh"));
        assertTrue(PatternMatch.isMatch("sdfgkjh", "SdFgKjH"));
        assertTrue(PatternMatch.isMatch("SdFgKjH", "sdfgkjh"));
        assertFalse(PatternMatch.isMatch("sdfxxxxxgkjh", "sdfgkjh"));
        assertFalse(PatternMatch.isMatch("sdfgkjh", "sdfxxxxxgkjh"));

        assertTrue(PatternMatch.isMatch("sdfxxxxxgkjh", "sdf*gkjh"));
        assertTrue(PatternMatch.isMatch("sdfgkjh", "sdf*gkjh"));
        assertTrue(PatternMatch.isMatch("sdfgkjh", "sdf*g*h"));
        assertTrue(PatternMatch.isMatch("", "*"));
        assertTrue(PatternMatch.isMatch(" ", "*"));

        assertTrue(PatternMatch.isMatch("c:/test/file.name", "*file*"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "*test*"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "*test*/*name"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "c*/*/*name"));
        assertFalse(PatternMatch.isMatch("c:/test/file.name", "c*/huh/*name"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "c:/*.name"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "c:/*"));
        assertTrue(PatternMatch.isMatch("c:/test/file.name", "*.name"));

        assertTrue(PatternMatch.isMatch("c:\\test\\file.name", "c:\\*"));
    }

    public void testCompiledPatterns() {
        assertTrue(new CompilingPatternMatch("sdfgkjh").isMatch("sdfgkjh"));
        assertTrue(new CompilingPatternMatch("sdfgkjh").isMatch("SdFgKjH"));
        assertTrue(new CompilingPatternMatch("SdFgKjH").isMatch("sdfgkjh"));
        assertFalse(new CompilingPatternMatch("sdfxxxxxgkjh").isMatch("sdfgkjh"));
        assertFalse(new CompilingPatternMatch("sdfgkjh").isMatch("sdfxxxxxgkjh"));

        assertTrue(new CompilingPatternMatch("sdf*gkjh").isMatch("sdfxxxxxgkjh"));
        assertTrue(new CompilingPatternMatch("sdf*gkjh").isMatch("sdfgkjh"));
        assertTrue(new CompilingPatternMatch("sdf*g*h").isMatch("sdfgkjh"));
        assertTrue(new CompilingPatternMatch("*").isMatch(""));
        assertTrue(new CompilingPatternMatch("*").isMatch(" "));

        assertTrue(new CompilingPatternMatch("*file*").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("*test*").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("*test*/*name").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("c*/*/*name").isMatch("c:/test/file.name"));
        assertFalse(new CompilingPatternMatch("c*/huh/*name").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("c:/*.name").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("c:/*").isMatch("c:/test/file.name"));
        assertTrue(new CompilingPatternMatch("*.name").isMatch("c:/test/file.name"));

        assertTrue(new CompilingPatternMatch("c:\\*").isMatch("c:\\test\\file.name"));
        assertTrue(new CompilingPatternMatch("x\\~!@#$%^-&*()_+={}][:';,.<>|y").isMatch("x\\~!@#$%^-&*()_+={}][:';,.<>|y"));
    }
}
