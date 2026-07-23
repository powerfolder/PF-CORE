package de.dal33t.powerfolder.util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class TagUtilTest extends TestCase {

    public void testParseJsonArray() {
        List<String> tags = TagUtil.parse("[\"Contract\",\"Project North\",\"2026\"]");
        assertEquals(Arrays.asList("Contract", "Project North", "2026"), tags);
    }

    public void testParseBlankReturnsEmpty() {
        assertTrue(TagUtil.parse(null).isEmpty());
        assertTrue(TagUtil.parse("").isEmpty());
        assertTrue(TagUtil.parse("   ").isEmpty());
    }

    public void testParseNonJsonFallsBackToSingleTag() {
        assertEquals(Arrays.asList("plainword"), TagUtil.parse("plainword"));
    }

    public void testNormalizeAllowsSpacesTrimsAndDedupesCaseInsensitive() {
        List<String> tags = TagUtil.normalize(
                Arrays.asList("  Project North  ", "project north", "Contract", "CONTRACT", ""));
        // First spelling wins, case-insensitive duplicates removed, spaces kept
        assertEquals(Arrays.asList("Project North", "Contract"), tags);
    }

    public void testNormalizeStripsControlChars() {
        List<String> tags = TagUtil.normalize(Arrays.asList("a\tb\nc"));
        assertEquals(Arrays.asList("a b c"), tags);
    }

    public void testToJsonRoundTrip() {
        String json = TagUtil.toJson(Arrays.asList("Contract", "Project North"));
        assertEquals(Arrays.asList("Contract", "Project North"), TagUtil.parse(json));
    }

    public void testToJsonEmptyReturnsNull() {
        assertNull(TagUtil.toJson(Arrays.asList("", "   ")));
        assertNull(TagUtil.toJson(null));
    }
}
