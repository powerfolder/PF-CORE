package de.dal33t.powerfolder;

import junit.framework.TestCase;

import java.util.List;

/**
 * PFS-5653: DocumentType is the single source of what kind of file an extension belongs to - both for the
 * inline viewer and for the category filter of the search.
 */
public class DocumentTypeTest extends TestCase {

    public void testCategoryOfKnownExtensions() {
        assertEquals("document", DocumentType.categoryOf("docx"));
        assertEquals("document", DocumentType.categoryOf("epub"));
        assertEquals("spreadsheet", DocumentType.categoryOf("xlsx"));
        assertEquals("a comma separated table is a spreadsheet, not prose",
                "spreadsheet", DocumentType.categoryOf("csv"));
        assertEquals("presentation", DocumentType.categoryOf("pptx"));
        assertEquals("image", DocumentType.categoryOf("jpg"));
        assertEquals("audio", DocumentType.categoryOf("flac"));
        assertEquals("video", DocumentType.categoryOf("mov"));
        assertEquals("a pdf is its own kind of file", "pdf", DocumentType.categoryOf("pdf"));
        assertEquals("text", DocumentType.categoryOf("txt"));
        assertEquals("archive", DocumentType.categoryOf("zip"));
        assertEquals("a scanned document is a document, not audio", "document",
                DocumentType.categoryOf("djvu"));
    }

    public void testCategoryOfIsCaseInsensitive() {
        assertEquals("image", DocumentType.categoryOf("PNG"));
        assertEquals("presentation", DocumentType.categoryOf("ODP"));
    }

    public void testUnknownAndEmpty() {
        assertEquals("other", DocumentType.categoryOf("xyz"));
        assertEquals("other", DocumentType.categoryOf(""));
        assertEquals("other", DocumentType.categoryOf(null));
    }

    public void testCategoriesAreOfferedDocumentsFirstAndFoldersLast() {
        List<String> categories = DocumentType.categories();
        assertEquals(DocumentType.values().length + 1, categories.size());
        assertEquals("document", categories.get(0));
        assertEquals("folder", categories.get(categories.size() - 1));
        assertFalse("\"none of the above\" is no filter", categories.contains("other"));
    }

    public void testEveryTypeHasACategoryAndExtensions() {
        for (DocumentType type : DocumentType.values()) {
            assertEquals(type.name().toLowerCase(), type.getCategory());
            assertFalse(type + " has no extensions", type.getExtensions().isEmpty());
        }
    }
}
