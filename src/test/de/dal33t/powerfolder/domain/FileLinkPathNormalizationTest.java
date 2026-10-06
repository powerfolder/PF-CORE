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
package de.dal33t.powerfolder.domain;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.util.PathUtils;
public class FileLinkPathNormalizationTest {

    // ========================================================================
    // BASIC PATH NORMALIZATION TESTS
    // ========================================================================

    /**
     * Test 1: Simple path without special characters
     * Input: "a/b/c"
     * Expected: "a/b/c"
     */
    @Test
    public void testSimplePath() {
        assertEquals("a/b/c", PathUtils.normalizePath("a/b/c"));
    }

    /**
     * Test 2: Empty string
     * Input: ""
     * Expected: ""
     */
    @Test
    public void testEmptyString() {
        assertEquals("", PathUtils.normalizePath(""));
    }

    /**
     * Test 3: Null string
     * Input: null
     * Expected: ""
     */
    @Test
    public void testNullString() {
        assertEquals("", PathUtils.normalizePath(null));
    }

    /**
     * Test 4: Single directory
     * Input: "folder"
     * Expected: "folder"
     */
    @Test
    public void testSingleDirectory() {
        assertEquals("folder", PathUtils.normalizePath("folder"));
    }

    // ========================================================================
    // CURRENT DIRECTORY (.) TESTS
    // ========================================================================

    /**
     * Test 5: Current directory at start
     * Input: "./a/b"
     * Expected: "a/b"
     */
    @Test
    public void testCurrentDirectoryAtStart() {
        assertEquals("a/b", PathUtils.normalizePath("./a/b"));
    }

    /**
     * Test 6: Current directory in middle
     * Input: "a/./b"
     * Expected: "a/b"
     */
    @Test
    public void testCurrentDirectoryInMiddle() {
        assertEquals("a/b", PathUtils.normalizePath("a/./b"));
    }

    /**
     * Test 7: Current directory at end
     * Input: "a/b/."
     * Expected: "a/b"
     */
    @Test
    public void testCurrentDirectoryAtEnd() {
        assertEquals("a/b", PathUtils.normalizePath("a/b/."));
    }

    /**
     * Test 8: Multiple current directories
     * Input: "./a/././b/."
     * Expected: "a/b"
     */
    @Test
    public void testMultipleCurrentDirectories() {
        assertEquals("a/b", PathUtils.normalizePath("./a/././b/."));
    }

    /**
     * Test 9: Only current directory
     * Input: "."
     * Expected: ""
     */
    @Test
    public void testOnlyCurrentDirectory() {
        assertEquals("", PathUtils.normalizePath("."));
    }

    // ========================================================================
    // PARENT DIRECTORY (..) TESTS - VALID
    // ========================================================================

    /**
     * Test 10: Parent directory in middle (valid)
     * Input: "a/b/../c"
     * Expected: "a/c"
     */
    @Test
    public void testParentDirectoryValidInMiddle() {
        assertEquals("a/c", PathUtils.normalizePath("a/b/../c"));
    }

    /**
     * Test 11: Parent directory at end (valid)
     * Input: "a/b/.."
     * Expected: "a"
     */
    @Test
    public void testParentDirectoryValidAtEnd() {
        assertEquals("a", PathUtils.normalizePath("a/b/.."));
    }

    /**
     * Test 12: Multiple parent directories (valid)
     * Input: "a/b/c/../../d"
     * Expected: "a/d"
     */
    @Test
    public void testMultipleParentDirectoriesValid() {
        assertEquals("a/d", PathUtils.normalizePath("a/b/c/../../d"));
    }

    /**
     * Test 13: Complex traversal within bounds
     * Input: "a/b/c/../d/../e"
     * Expected: "a/b/e"
     */
    @Test
    public void testComplexTraversalWithinBounds() {
        assertEquals("a/b/e", PathUtils.normalizePath("a/b/c/../d/../e"));
    }

    // ========================================================================
    // PARENT DIRECTORY (..) TESTS - ROOT ESCAPE (Should return null)
    // ========================================================================

    /**
     * Test 14:  Parent directory escapes root at start
     * Input: "../a"
     * Expected: null (escape attempt)
     */
    @Test
    public void testParentDirectoryEscapesRootAtStart() {
        assertNull( PathUtils.normalizePath("../a"),"Should reject path escaping root");
    }

    /**
     * Test 15:  Multiple escapes at start
     * Input: "../../a"
     * Expected: null (escape attempt)
     */
    @Test
    public void testMultipleEscapesAtStart() {
        assertNull( PathUtils.normalizePath("../../a"),"Should reject multiple escapes");
    }

    /**
     * Test 16:  Only parent directory
     * Input: ".."
     * Expected: null (escape attempt)
     */
    @Test
    public void testOnlyParentDirectory() {
        assertNull( PathUtils.normalizePath(".."),"Should reject single ..");
    }

    /**
     * Test 17:  Multiple parent directories only
     * Input: "../.."
     * Expected: null (escape attempt)
     */
    @Test
    public void testOnlyMultipleParentDirectories() {
        assertNull( PathUtils.normalizePath("../.."),"Should reject multiple ..");
    }

    /**
     * Test 18:  Escape after valid path
     * Input: "a/../.."
     * Expected: null (escape attempt)
     */
    @Test
    public void testEscapeAfterValidPath() {
        assertNull( PathUtils.normalizePath("a/../.."),"Should reject escape after valid path");
    }

    /**
     * Test 19:  Deep escape attempt
     * Input: "../../../../etc/passwd"
     * Expected: null (escape attempt)
     */
    @Test
    public void testDeepEscapeAttempt() {
        assertNull( PathUtils.normalizePath("../../../../etc/passwd"),"Should reject deep escape");
    }

    // ========================================================================
    // DOUBLE/MULTIPLE SLASH TESTS
    // ========================================================================

    /**
     * Test 20: Double slash at start
     * Input: "//a/b"
     * Expected: "a/b"
     */
    @Test
    public void testDoubleSlashAtStart() {
        assertEquals("a/b", PathUtils.normalizePath("//a/b"));
    }

    /**
     * Test 21: Double slash in middle
     * Input: "a//b"
     * Expected: "a/b"
     */
    @Test
    public void testDoubleSlashInMiddle() {
        assertEquals("a/b", PathUtils.normalizePath("a//b"));
    }

    /**
     * Test 22: Multiple slashes
     * Input: "a///b////c"
     * Expected: "a/b/c"
     */
    @Test
    public void testMultipleSlashes() {
        assertEquals("a/b/c", PathUtils.normalizePath("a///b////c"));
    }

    /**
     * Test 23: Trailing slash
     * Input: "a/b/"
     * Expected: "a/b"
     */
    @Test
    public void testTrailingSlash() {
        assertEquals("a/b", PathUtils.normalizePath("a/b/"));
    }

    // ========================================================================
    // BACKSLASH NORMALIZATION TESTS (Windows compatibility)
    // ========================================================================

    /**
     * Test 24: Backslash instead of forward slash
     * Input: "a\\b\\c"
     * Expected: "a/b/c"
     */
    @Test
    public void testBackslashToForwardSlash() {
        assertEquals("a/b/c", PathUtils.normalizePath("a\\b\\c"));
    }

    /**
     * Test 25: Mixed slashes
     * Input: "a/b\\c"
     * Expected: "a/b/c"
     */
    @Test
    public void testMixedSlashes() {
        assertEquals("a/b/c", PathUtils.normalizePath("a/b\\c"));
    }

    /**
     * Test 26: Backslash with parent directory
     * Input: "a\\b\\..\\c"
     * Expected: "a/c"
     */
    @Test
    public void testBackslashWithParentDirectory() {
        assertEquals("a/c", PathUtils.normalizePath("a\\b\\..\\c"));
    }

    // ========================================================================
    // PATH TRAVERSAL ATTACK TESTS
    // ========================================================================

    /**
     * Test 27:  Classic sibling folder attack
     * Input: "sharedDir/../siblingDir/secret.txt"
     * Expected: "siblingDir/secret.txt" (will be blocked by check)
     */
    @Test
    public void testClassicSiblingFolderAttack() {
        assertEquals("siblingDir/secret.txt", PathUtils.normalizePath("sharedDir/../siblingDir/secret.txt"));
    }

    /**
     * Test 28:  Parent directory escape attempt
     * Input: "sharedDir/../../rootFile.txt"
     * Expected: null (trying to go above root - sharedDir/../.. means up 2 levels from only 1 level deep)
     */
    @Test
    public void testParentDirectoryEscape() {
        assertNull( PathUtils.normalizePath("sharedDir/../../rootFile.txt"),"Should reject path escaping root");
    }

    /**
     * Test 29: Complex path with mixed traversal
     * Input: "a/b/./c/../d/../e"
     * Expected: "a/b/e"
     */
    @Test
    public void testComplexMixedTraversal() {
        assertEquals("a/b/e", PathUtils.normalizePath("a/b/./c/../d/../e"));
    }

    /**
     * Test 30: Deep nesting with traversal
     * Input: "a/b/c/d/e/../../f"
     * Expected: "a/b/c/f"
     */
    @Test
    public void testDeepNestingWithTraversal() {
        assertEquals("a/b/c/f", PathUtils.normalizePath("a/b/c/d/e/../../f"));
    }

    // ========================================================================
    // REAL-WORLD ATTACK SCENARIOS
    // ========================================================================

    /**
     * Test 31: Attack from pentest report
     * FileLink for "test/mehrtest"
     * Attack: "test/mehrtest/../asd.docx"
     * Expected: "test/asd.docx" (will fail containment: not startsWith("test/mehrtest/"))
     */
    @Test
    public void testPentestScenario1() {
        assertEquals("test/asd.docx", PathUtils.normalizePath("test/mehrtest/../asd.docx"));
    }

    /**
     * Test 32: Top-level file access via link
     * FileLink for "test/mehrtest"
     * Attack: "test/mehrtest/../../geheimesdokument.txt"
     * Expected: "geheimesdokument.txt" (will fail containment)
     */
    @Test
    public void testPentestScenario2() {
        assertEquals("geheimesdokument.txt", PathUtils.normalizePath("test/mehrtest/../../geheimesdokument.txt"));
    }

    /**
     * Test 33: Legitimate access within shared folder
     * FileLink for "b"
     * Request: "b/test01_/../prices.csv"
     * Expected: "b/prices.csv" (valid - within b/)
     */
    @Test
    public void testLegitimateTraversalWithinSharedFolder() {
        assertEquals("b/prices.csv", PathUtils.normalizePath("b/test01_/../prices.csv"));
    }

    /**
     * Test 34: URL decoded traversal attack
     * Input: "b/../c/d.docx" (already URL decoded)
     * Expected: "c/d.docx" (will fail containment)
     */
    @Test
    public void testURLDecodedTraversal() {
        assertEquals("c/d.docx", PathUtils.normalizePath("b/../c/d.docx"));
    }

    // ========================================================================
    // SPECIAL CHARACTERS AND EDGE CASES
    // ========================================================================

    /**
     * Test 35: Path with spaces
     * Input: "a/b c/d"
     * Expected: "a/b c/d"
     */
    @Test
    public void testPathWithSpaces() {
        assertEquals("a/b c/d", PathUtils.normalizePath("a/b c/d"));
    }

    /**
     * Test 36: Path with special characters
     * Input: "a/b-test_123/c"
     * Expected: "a/b-test_123/c"
     */
    @Test
    public void testPathWithSpecialCharacters() {
        assertEquals("a/b-test_123/c", PathUtils.normalizePath("a/b-test_123/c"));
    }

    /**
     * Test 37: Empty components between slashes
     * Input: "a//b///c"
     * Expected: "a/b/c"
     */
    @Test
    public void testEmptyComponents() {
        assertEquals("a/b/c", PathUtils.normalizePath("a//b///c"));
    }

    /**
     * Test 38: Whitespace-only string
     * Input: "   "
     * Expected: ""
     */
    @Test
    public void testWhitespaceOnlyString() {
        assertEquals("", PathUtils.normalizePath("   "));
    }

    /**
     * Test 39: All resolved to empty
     * Input: "a/.."
     * Expected: ""
     */
    @Test
    public void testAllResolvedToEmpty() {
        assertEquals("", PathUtils.normalizePath("a/.."));
    }

    /**
     * Test 40: Complex real-world path
     * Input: "shared/documents/./2024/../2025/reports/./Q1/../Q2/file.pdf"
     * Expected: "shared/documents/2025/reports/Q2/file.pdf"
     */
    @Test
    public void testComplexRealWorldPath() {
        assertEquals("shared/documents/2025/reports/Q2/file.pdf",
                PathUtils.normalizePath("shared/documents/./2024/../2025/reports/./Q1/../Q2/file.pdf"));
    }
}
