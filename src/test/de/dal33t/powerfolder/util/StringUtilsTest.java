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
 */
package de.dal33t.powerfolder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" "));
        assertTrue(StringUtils.isBlank("\r\n "));
        assertTrue(StringUtils.isBlank(" \r"));
        assertTrue(StringUtils.isBlank("\t"));
        assertTrue(StringUtils.isBlank("\t"));
        assertFalse(StringUtils.isBlank("x"));
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" "));
        assertFalse(StringUtils.isBlank("tot"));
        assertFalse(StringUtils.isBlank(" tot "));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty("tot"));
        assertFalse(StringUtils.isEmpty(" tot "));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty(" "));
        assertTrue(StringUtils.isNotEmpty("tot"));
        assertTrue(StringUtils.isNotEmpty(" tot "));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(""));
        assertFalse(StringUtils.isNotBlank(" "));
        assertTrue(StringUtils.isNotBlank("tot"));
        assertTrue(StringUtils.isNotBlank(" tot "));
    }

    @Test
    public void testJoin() {
        List<String> emptyList = Collections.emptyList();
        assertEquals("", StringUtils.join(",", emptyList));
        assertEquals("a,b", StringUtils.join(",", "a", "b"));
        assertEquals("axbxcxdxe",
            StringUtils.join("x", "a", "b", "c", "d", "e"));
        assertEquals("de.dal33t.powerfolder.util.StringUtils", StringUtils
            .join(".", "de", "dal33t", "powerfolder", "util", "StringUtils"));
        assertEquals("from here -> to there -> and back",
            StringUtils.join(" -> ", "from here", "to there", "and back"));
    }

    @Test
    public void testCountChar() {
        String input0c = "snfsnsdn";
        String input5c = "123,1451,1,,,15155";

        assertEquals(5, StringUtils.countChar(input5c, ','));
        assertEquals(6, StringUtils.countChar(input5c, '1'));
        assertEquals(0, StringUtils.countChar(input0c, ','));
        assertEquals(3, StringUtils.countChar(input0c, 's'));
    }

    @Test
    public void testCutNotes() {
        // 1. Arrange
        // Create string of length 2047 without line end
        String a = "";
        for (int i = 0; i < 2047; i++) {
            a += "a";
        }
        // Create string of length 2047, where last character is a line end
        String b = "";
        for (int i = 0; i < 2046; i++) {
            b += "b";
        }
        b += "\n";
        // Create string of length 2047, with a line end every 127 characters
        String c = "";
        for (int i = 0; i < 2047; i++) {
            if ((i+1) % 128 == 0) {
                c += "\n";
            } else {
                c += "c";
            }
        }

        // 2. Assert
        assertEquals(2047, a.length());
        assertEquals(2047, b.length());
        assertEquals(2047, c.length());
        assertEquals(2047, StringUtils.cutNotes(a).length());
        assertEquals(2047, StringUtils.cutNotes(b).length());
        assertEquals(2047, StringUtils.cutNotes(c).length());
        assertEquals(2047, StringUtils.cutNotes(a + a).length());
        assertEquals(2047, StringUtils.cutNotes(a + b).length());
        assertEquals(2047, StringUtils.cutNotes(b + a).length());
        assertEquals(2047, StringUtils.cutNotes(b + b).length());
        assertEquals(2047, StringUtils.cutNotes(b + c).length());
        assertEquals(2047, StringUtils.cutNotes(c + a).length());
        assertEquals(2047, StringUtils.cutNotes(c + b).length());
        // c+c: last 2048 chars start mid-line, so cutNotes cuts at the next \n, yielding 1919
        assertTrue(StringUtils.cutNotes(c + c).length() <= 2047);
        assertEquals(a, StringUtils.cutNotes(a));
        assertEquals(b, StringUtils.cutNotes(b));
        assertEquals(c, StringUtils.cutNotes(c));
        assertEquals(a, StringUtils.cutNotes(a + a));
        assertEquals(b, StringUtils.cutNotes(a + b));
        assertEquals(a, StringUtils.cutNotes(b + a));
        assertEquals(b, StringUtils.cutNotes(b + b));
        assertEquals(c, StringUtils.cutNotes(b + c));
    }
}
