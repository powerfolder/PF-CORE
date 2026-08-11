package de.dal33t.powerfolder.search;

import junit.framework.TestCase;

public class FileCategoryMapperTest extends TestCase {

    public void testKnownCategories() {
        assertEquals("image", FileCategoryMapper.categoryOf("jpg"));
        assertEquals("image", FileCategoryMapper.categoryOf("PNG"));
        assertEquals("video", FileCategoryMapper.categoryOf("mp4"));
        assertEquals("audio", FileCategoryMapper.categoryOf("mp3"));
        assertEquals("document", FileCategoryMapper.categoryOf("pdf"));
        assertEquals("document", FileCategoryMapper.categoryOf("docx"));
        assertEquals("spreadsheet", FileCategoryMapper.categoryOf("xlsx"));
        assertEquals("spreadsheet", FileCategoryMapper.categoryOf("ods"));
        assertEquals("a comma separated table is a spreadsheet, not prose",
                "spreadsheet", FileCategoryMapper.categoryOf("csv"));
        assertEquals("presentation", FileCategoryMapper.categoryOf("pptx"));
        assertEquals("presentation", FileCategoryMapper.categoryOf("ODP"));
        assertEquals("archive", FileCategoryMapper.categoryOf("zip"));
        assertEquals("text", FileCategoryMapper.categoryOf("txt"));
    }

    public void testUnknownAndEmpty() {
        assertEquals("other", FileCategoryMapper.categoryOf("xyz"));
        assertEquals("other", FileCategoryMapper.categoryOf(""));
        assertEquals("other", FileCategoryMapper.categoryOf(null));
    }
}
