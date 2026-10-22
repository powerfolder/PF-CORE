package de.dal33t.powerfolder.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TagUtilTest {

    @Test
    public void testParseJsonArray() {
        List<String> tags = TagUtil.parse("[\"Contract\",\"Project North\",\"2026\"]");
        assertEquals(Arrays.asList("Contract", "Project North", "2026"), tags);
    }

    @Test
    public void testParseBlankReturnsEmpty() {
        assertTrue(TagUtil.parse(null).isEmpty());
        assertTrue(TagUtil.parse("").isEmpty());
        assertTrue(TagUtil.parse("   ").isEmpty());
    }

    @Test
    public void testParseNonJsonFallsBackToSingleTag() {
        assertEquals(Arrays.asList("plainword"), TagUtil.parse("plainword"));
    }

    @Test
    public void testNormalizeAllowsSpacesTrimsAndDedupesCaseInsensitive() {
        List<String> tags = TagUtil.normalize(
                Arrays.asList("  Project North  ", "project north", "Contract", "CONTRACT", ""));
        // First spelling wins, case-insensitive duplicates removed, spaces kept
        assertEquals(Arrays.asList("Project North", "Contract"), tags);
    }

    @Test
    public void testNormalizeStripsControlChars() {
        List<String> tags = TagUtil.normalize(Arrays.asList("a\tb\nc"));
        assertEquals(Arrays.asList("a b c"), tags);
    }

    @Test
    public void testToJsonRoundTrip() {
        String json = TagUtil.toJson(Arrays.asList("Contract", "Project North"));
        assertEquals(Arrays.asList("Contract", "Project North"), TagUtil.parse(json));
    }

    @Test
    public void testToJsonEmptyReturnsNull() {
        assertNull(TagUtil.toJson(Arrays.asList("", "   ")));
        assertNull(TagUtil.toJson(null));
    }
}
