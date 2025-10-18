/*
 * Copyright 2004 - 2026 Christian Sprajc. All rights reserved.
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

package de.dal33t.powerfolder.search;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Manages a Lucene index for a specific PowerFolder Folder.
 * Handles metadata, full-text extraction (Tika), and optional OCR (TesseractOCR).
 */
public class LuceneIndexManager extends Loggable {

    private final Folder folder;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;
    private final Tika tika;
    private final TesseractOCR ocrEngine;

    private boolean extractContentEnabled = true;
    private boolean ocrEnabled = true;

    public LuceneIndexManager(Folder folder) throws IOException {
        super();
        this.folder = folder;

        this.indexPath = folder.getSystemSubDir().resolve("index");
        Files.createDirectories(indexPath);

        this.analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(FSDirectory.open(indexPath), config);

        this.tika = new Tika();
        this.ocrEngine = TesseractOCR.getInstance();

        logFine("Lucene index initialized for folder: " + folder.getName() +
                " at " + indexPath.toAbsolutePath());
    }

    // ------------------------------------------------------------------------
    // Configuration toggles
    // ------------------------------------------------------------------------

    public void setExtractContentEnabled(boolean enabled) {
        this.extractContentEnabled = enabled;
        logFine("Content extraction " + (enabled ? "enabled" : "disabled") +
                " for folder " + folder.getName());
    }

    public void setOcrEnabled(boolean enabled) {
        this.ocrEnabled = enabled;
        logFine("OCR " + (enabled ? "enabled" : "disabled") +
                " for folder " + folder.getName());
    }

    // ------------------------------------------------------------------------
    // Core indexing
    // ------------------------------------------------------------------------

    public void indexFile(FileInfo fileInfo) {
        try {
            String docId = buildDocId(fileInfo);
            Document doc = new Document();

            doc.add(new StringField("docId", docId, Field.Store.YES));
            doc.add(new StringField("folderId", folder.getId(), Field.Store.YES));
            doc.add(new StoredField("folderName", folder.getName()));
            doc.add(new TextField("name", fileInfo.getFilenameOnly(), Field.Store.YES));
            doc.add(new TextField("relativePath", fileInfo.getRelativeName(), Field.Store.YES));
            doc.add(new LongPoint("modified",
                    fileInfo.getModifiedDate() != null ? fileInfo.getModifiedDate().getTime() : 0));
            doc.add(new StoredField("size", fileInfo.getSize()));

            if (extractContentEnabled) {
                String content = extractContent(fileInfo);
                if (content != null && !content.isBlank()) {
                    doc.add(new TextField("content", content, Field.Store.NO));
                }
            }

            writer.updateDocument(new Term("docId", docId), doc);
            logInfo("Indexed file " + fileInfo);
        } catch (Exception e) {
            logWarning("Failed to index file " + fileInfo + " in folder " +
                    folder.getName() + ": " + e.getMessage());
        }
    }

    public void indexFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        try {
            for (FileInfo fileInfo : files) {
                indexFile(fileInfo);
            }
            writer.commit();
            logFine("Bulk indexed " + files.size() + " files in folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Bulk indexing failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    public void deleteFile(FileInfo fileInfo) {
        if (fileInfo == null) return;
        try {
            writer.deleteDocuments(new Term("docId", buildDocId(fileInfo)));
            writer.commit();
            logFine("Deleted file from index: " + fileInfo + " in folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Failed to delete file from index: " + fileInfo + " in folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    public void deleteFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        try {
            for (FileInfo fileInfo : files) {
                writer.deleteDocuments(new Term("docId", buildDocId(fileInfo)));
            }
            writer.commit();
            logFine("Bulk deleted " + files.size() + " files from index of folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Bulk delete failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    public void rebuildIndex(Collection<FileInfo> allFiles) {
        try {
            logFine("Rebuilding Lucene index for folder: " + folder.getName());
            writer.deleteAll();
            if (allFiles != null && !allFiles.isEmpty()) {
                for (FileInfo f : allFiles) {
                    if (!f.isDeleted()) {
                        indexFile(f);
                    } else {
                        deleteFile(f);
                    }
                }
            }
            writer.commit();
            logFine("Rebuild completed for folder " + folder.getName());
        } catch (Exception e) {
            logSevere("Failed to rebuild index for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Incremental update from ScanResult
    // ------------------------------------------------------------------------

    public void updateIndex(ScanResult scanResult) {
        Reject.ifNull(scanResult, "ScanResult");

        if (!scanResult.isChangeDetected()) {
            logFiner("No changes detected for folder " + folder.getName());
            return;
        }

        try {
            indexFiles(scanResult.getNewFiles());
            indexFiles(scanResult.getChangedFiles());
            indexFiles(scanResult.getRestoredFiles());
            deleteFiles(scanResult.getDeletedFiles());
            commit();

            logFine("Lucene index updated for folder " + folder.getName() +
                    " (new=" + safeSize(scanResult.getNewFiles()) +
                    ", changed=" + safeSize(scanResult.getChangedFiles()) +
                    ", restored=" + safeSize(scanResult.getRestoredFiles()) +
                    ", deleted=" + safeSize(scanResult.getDeletedFiles()) + ")");
        } catch (Exception e) {
            logWarning("Lucene index update failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    private int safeSize(Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    // ------------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------------

    public List<FileInfo> searchFiles(String queryText, int maxResults) {
        List<FileInfo> results = new ArrayList<>();
        if (queryText == null || queryText.isBlank()) return results;

        try {
            writer.commit(); // ensure latest changes visible

            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                String sanitized = queryText.trim().toLowerCase().replaceAll("[^a-z0-9äöüß]", " ");
                String wildcardQuery = "*" + sanitized + "*";

                MultiFieldQueryParser parser = new MultiFieldQueryParser(
                        new String[]{"name", "relativePath", "content"},
                        analyzer
                );
                parser.setAllowLeadingWildcard(true);
                Query query = parser.parse(wildcardQuery);

                TopDocs topDocs = searcher.search(query, maxResults);
                logInfo("Found " + topDocs.totalHits + " hits for query '" + queryText + "' in folder " + folder.getName());

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    String relPath = doc.get("relativePath");
                    if (relPath == null) continue;

                    FileInfo lookup = FileInfoFactory.lookupInstance(folder.getInfo(), relPath);
                    FileInfo fileInfo = folder.getFile(lookup);
                    if (fileInfo != null) {
                        results.add(fileInfo);
                    }
                }
            }
        } catch (Exception e) {
            logWarning("Lucene search failed for query '" + queryText + "' in folder " + folder.getName() + ": " + e.getMessage());
        }
        return results;
    }


    // ------------------------------------------------------------------------
    // Content extraction + OCR
    // ------------------------------------------------------------------------

    private String extractContent(FileInfo fileInfo) {
        if (!extractContentEnabled) return null;

        Path filePath = fileInfo.getDiskFile(folder.getController().getFolderRepository());
        if (filePath == null || !Files.exists(filePath)) return null;

        try (InputStream stream = Files.newInputStream(filePath)) {
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(stream, handler, metadata);
            return handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            if (ocrEnabled && filePath.toString().matches(".*\\.(png|jpg|jpeg|tif|tiff|bmp|pdf)$")) {
                String ocrText = ocrEngine.performOCR(filePath);
                if (ocrText != null && !ocrText.isBlank()) {
                    return ocrText;
                }
            }
            logFiner("No text extracted from " + fileInfo + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Utility + lifecycle
    // ------------------------------------------------------------------------

    public int getDocumentCount() {
        try {
            return writer.getDocStats().numDocs;
        } catch (Exception e) {
            logWarning("Failed to get document count: " + e.getMessage());
            return -1;
        }
    }

    private String buildDocId(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo");
        return folder.getId() + ":" + fileInfo.getRelativeName();
    }

    public void commit() {
        try {
            writer.commit();
        } catch (Exception e) {
            logWarning("Commit failed for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            writer.close();
            logFine("Lucene index closed for folder: " + folder.getName());
        } catch (Exception e) {
            logWarning("Failed to close Lucene index for folder " + folder.getName() + ": " + e.getMessage());
        }
    }
}
