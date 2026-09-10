package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.TagUtil;
import junit.framework.TestCase;

import java.util.Arrays;
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

    /**
     * ID-0002-R: a search term reaches the tags as well. Clicking a tag inserts a "tag:" token and found
     * every tagged element; typing the very same words found none, because a keyword was matched against
     * the file name alone. The index searches name, path and tags alike, and without it the answer has to
     * be the same - on a server that mounts on demand the database answers most of the time.
     */
    public void testAKeyWordReachesTheTags() {
        FileInfo tagged = file("Protokolle/Sitzung.docx", "Wonderful Day", "Urgent");

        assertTrue("the tag itself", criteriaFor("wonderful day").matches(tagged));
        assertTrue("one word of it", criteriaFor("wonderful").matches(tagged));
        assertTrue("a second tag", criteriaFor("urgent").matches(tagged));
        assertTrue("case does not matter", criteriaFor("WONDERFUL").matches(tagged));
        assertFalse("a word that is nowhere", criteriaFor("terrible").matches(tagged));
    }

    /** The name and the path it sits in stay searchable, tags or no tags. */
    public void testAKeyWordStillReachesNameAndPath() {
        FileInfo plain = file("Protokolle/Sitzung.docx");

        assertTrue("the name", criteriaFor("sitzung").matches(plain));
        assertTrue("the directory above it", criteriaFor("protokolle").matches(plain));
        assertFalse(criteriaFor("wonderful").matches(plain));
    }

    /** Every keyword has to be met - by whichever of the three places (PFS-5653: they are ANDed). */
    public void testEveryKeyWordHasToBeMet() {
        FileInfo tagged = file("Protokolle/Sitzung.docx", "Wonderful Day");

        FileInfoCriteria both = new FileInfoCriteria();
        both.addKeyWord("sitzung");
        both.addKeyWord("wonderful");
        assertTrue("one from the name, one from the tag", both.matches(tagged));

        FileInfoCriteria withMiss = new FileInfoCriteria();
        withMiss.addKeyWord("sitzung");
        withMiss.addKeyWord("terrible");
        assertFalse(withMiss.matches(tagged));
    }

    /** The "tag:" operator keeps its exact-match semantics - it is not a substring search. */
    public void testTheTagOperatorStaysExact() {
        FileInfo tagged = file("Protokolle/Sitzung.docx", "Wonderful Day");

        FileInfoCriteria exact = new FileInfoCriteria();
        exact.addTag("Wonderful Day");
        assertTrue(exact.matches(tagged));

        FileInfoCriteria partial = new FileInfoCriteria();
        partial.addTag("Wonderful");
        assertFalse("half a tag is not that tag", partial.matches(tagged));
    }

    private static FileInfoCriteria criteriaFor(String query) {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addKeyWord(query);
        return criteria;
    }

    private static FileInfo file(String relativeName, String... tags) {
        FolderInfo folder = FolderInfoFactory.newTopFolderForTest("Workspace", "M_criteria_test");
        return FileInfoFactory.unmarshallExistingFile(folder, relativeName, null, 1024L, null, null,
            new Date(), 1, null, false, tags.length == 0 ? null : TagUtil.toJson(Arrays.asList(tags)));
    }
}
