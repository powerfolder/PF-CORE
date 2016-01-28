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
    public void testReplace() {
        assertEquals(null, StringUtils.replace(null, "a", "b"));
        assertEquals("", StringUtils.replace("", "a", "b"));
        assertEquals("any", StringUtils.replace("any", null, "b"));
        assertEquals("any", StringUtils.replace("any", "a", null));
        assertEquals("any", StringUtils.replace("any", "", "b"));
        assertEquals("aba", StringUtils.replace("aba", "a", null));
        assertEquals("b", StringUtils.replace("aba", "a", ""));
        assertEquals("zbz", StringUtils.replace("aba", "a", "z"));
    }

    @Test
    public void testReplaceWithLimit() {
        assertEquals(null, StringUtils.replace(null, "a", "b", 10));
        assertEquals("", StringUtils.replace("", "a", "b", 20));
        assertEquals("any", StringUtils.replace("any", null, "a", -12));
        assertEquals("any", StringUtils.replace("any", "a", null, 2));
        assertEquals("any", StringUtils.replace("any", "", "a", 5));
        assertEquals("any", StringUtils.replace("any", "a", "b", 0));
        assertEquals("abaa", StringUtils.replace("abaa", "a", null, -1));
        assertEquals("b", StringUtils.replace("abaa", "a", "", -1));
        assertEquals("abaa", StringUtils.replace("abaa", "a", "z", 0));
        assertEquals("zbaa", StringUtils.replace("abaa", "a", "z", 1));
        assertEquals("zbza", StringUtils.replace("abaa", "a", "z", 2));
        assertEquals("zbzz", StringUtils.replace("abaa", "a", "z", -1));
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
        // Create string of length 1024 without line end
        String a = "";
        for (int i = 0; i < 1024; i++) {
            a += "a";
        }
        // Create string of length 1024, where last character is a line end
        String b = "";
        for (int i = 0; i < 1023; i++) {
            b += "b";
        }
        b += "\n";
        // Create string of length 1024, with a line end every 127 characters
        String c = "";
        for (int i = 0; i < 1024; i++) {
            if ((i+1) % 128 == 0) {
                c += "\n";
            } else {
                c += "c";
            }
        }

        // 2. Assert
        assertEquals(1024, a.length());
        assertEquals(1024, b.length());
        assertEquals(1024, c.length());
        assertEquals(1024, StringUtils.cutNotes(a).length());
        assertEquals(1024, StringUtils.cutNotes(b).length());
        assertEquals(1024, StringUtils.cutNotes(c).length());
        assertEquals(1024, StringUtils.cutNotes(a + a).length());
        assertEquals(1024, StringUtils.cutNotes(a + b).length());
        assertEquals(1024, StringUtils.cutNotes(b + a).length());
        assertEquals(1024, StringUtils.cutNotes(b + b).length());
        assertEquals(1024, StringUtils.cutNotes(b + c).length());
        assertEquals(1024, StringUtils.cutNotes(c + a).length());
        assertEquals(1024, StringUtils.cutNotes(c + b).length());
        assertEquals(1024, StringUtils.cutNotes(c + c).length());
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
