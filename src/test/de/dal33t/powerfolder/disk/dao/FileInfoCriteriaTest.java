package de.dal33t.powerfolder.disk.dao;

import junit.framework.TestCase;

import java.util.Date;
import java.util.List;

/**
 * PFS-5653: the two decisions {@link FileInfoCriteria} makes on its own - how the value of a name-like
 * filter is cut into words, and whether a query can be answered by a file only.
 */
public class FileInfoCriteriaTest extends TestCase {

    public void testNoValueLeavesNoWord() {
        assertTrue(FileInfoCriteria.nameWords(null).isEmpty());
        assertTrue(FileInfoCriteria.nameWords("").isEmpty());
        assertTrue(FileInfoCriteria.nameWords("   ").isEmpty());
    }

    /** The index tokenizes on everything that is neither a letter nor a digit, and so does the value. */
    public void testPunctuationAroundAWordIsDropped() {
        assertEquals(List.of("urgent"), FileInfoCriteria.nameWords("!urgent!"));
        assertEquals(List.of("urgent"), FileInfoCriteria.nameWords("(urgent)"));
        assertEquals(List.of("q3", "2026"), FileInfoCriteria.nameWords("!Q3! [2026]"));
    }

    /** A value of nothing but punctuation leaves no word, which means it filters nothing at all. */
    public void testPunctuationOnlyLeavesNoWord() {
        assertTrue(FileInfoCriteria.nameWords("!!!").isEmpty());
        assertTrue(FileInfoCriteria.nameWords("+++ ///").isEmpty());
        assertTrue("a hyphen or a dot stays inside a word, but is none on its own",
                FileInfoCriteria.nameWords("--- ...").isEmpty());
    }

    public void testWordsAreLowerCasedAndSplitOnBlanks() {
        assertEquals(List.of("annual", "report"), FileInfoCriteria.nameWords("Annual Report"));
        assertEquals(List.of("annual", "report"), FileInfoCriteria.nameWords("  Annual   REPORT  "));
    }

    /** Accents belong to the word - the index keeps them too, folding happens on the keyword side. */
    public void testAccentsSurvive() {
        assertEquals(List.of("müller"), FileInfoCriteria.nameWords("Müller"));
    }

    /** Dot, underscore and hyphen sit inside names often enough to stay part of the word. */
    public void testInnerPunctuationStays() {
        assertEquals(List.of("report.v2"), FileInfoCriteria.nameWords("report.v2"));
        assertEquals(List.of("test_pf-1"), FileInfoCriteria.nameWords("Test_PF-1"));
    }

    /** PFS-5306: the tokenizer keeps a dot only between alphanumerics, so a trailing one has to go. */
    public void testTrailingDotsAreCutOff() {
        assertEquals(List.of("29.7"), FileInfoCriteria.nameWords("29.7."));
        assertEquals(List.of("list", "29.7"), FileInfoCriteria.nameWords("List 29.7.."));
    }

    public void testEmptyCriteriaDescribeNothing() {
        assertFalse(new FileInfoCriteria().describesFilesOnly());
    }

    /** A name or a tag says nothing about files: folders carry both, so the folder rows stay. */
    public void testNameAndTagDoNotDescribeFilesOnly() {
        FileInfoCriteria byName = new FileInfoCriteria();
        byName.setFileName("report");
        assertFalse(byName.describesFilesOnly());

        FileInfoCriteria byTag = new FileInfoCriteria();
        byTag.addTag("contract");
        assertFalse(byTag.describesFilesOnly());

        FileInfoCriteria byKeyword = new FileInfoCriteria();
        byKeyword.addKeyWord("report");
        assertFalse(byKeyword.describesFilesOnly());
    }

    public void testAKindOfFileDescribesFilesOnly() {
        FileInfoCriteria pdfs = new FileInfoCriteria();
        pdfs.addCategory("pdf");
        assertTrue(pdfs.describesFilesOnly());

        FileInfoCriteria folders = new FileInfoCriteria();
        folders.addCategory("folder");
        assertFalse("a folder is what a folder row is", folders.describesFilesOnly());

        FileInfoCriteria both = new FileInfoCriteria();
        both.addCategory("folder");
        both.addCategory("pdf");
        assertFalse("asking for folders as well keeps them", both.describesFilesOnly());
    }

    public void testEveryFileOnlyCriterion() {
        FileInfoCriteria byExtension = new FileInfoCriteria();
        byExtension.addExtension("pdf");
        assertTrue(byExtension.describesFilesOnly());

        FileInfoCriteria bySmallest = new FileInfoCriteria();
        bySmallest.setMinSize(1024L);
        assertTrue(bySmallest.describesFilesOnly());

        FileInfoCriteria byLargest = new FileInfoCriteria();
        byLargest.setMaxSize(1024L);
        assertTrue(byLargest.describesFilesOnly());

        FileInfoCriteria byStart = new FileInfoCriteria();
        byStart.setModifiedAfter(new Date());
        assertTrue(byStart.describesFilesOnly());

        FileInfoCriteria byEnd = new FileInfoCriteria();
        byEnd.setModifiedBefore(new Date());
        assertTrue(byEnd.describesFilesOnly());

        FileInfoCriteria byEditor = new FileInfoCriteria();
        byEditor.setModifiedBy("jane");
        assertTrue(byEditor.describesFilesOnly());

        FileInfoCriteria byDevice = new FileInfoCriteria();
        byDevice.setModifiedByDeviceName("laptop");
        assertTrue(byDevice.describesFilesOnly());
    }
}
