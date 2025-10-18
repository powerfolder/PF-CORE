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
 *
 * $Id$
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Manages a Lucene index for a specific PowerFolder Folder.
 * <p>
 * Handles metadata, optional text extraction via Apache Tika,
 * and optional OCR via the shared TesseractOCR singleton.
 */
public class LuceneIndexManager extends Loggable {

    private final Folder folder;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;
    private final TesseractOCR ocrEngine;

    private boolean extractContentEnabled = true;
    private boolean ocrEnabled = true;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public LuceneIndexManager(Folder folder) throws IOException {
        super();
        this.folder = folder;

        this.indexPath = folder.getSystemSubDir().resolve("index");
        Files.createDirectories(indexPath);

        this.analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(FSDirectory.open(indexPath), config);

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
            doc.add(new StringField("folderName", folder.getName(), Field.Store.YES));
            doc.add(new StringField("name", fileInfo.getFilenameOnly(), Field.Store.YES));
            doc.add(new StringField("relativePath", fileInfo.getRelativeName(), Field.Store.YES));
            doc.add(new LongPoint("modified",
                    fileInfo.getModifiedDate() != null ? fileInfo.getModifiedDate().getTime() : 0));
            doc.add(new LongPoint("size", fileInfo.getSize()));

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
                    indexFile(f);
                }
            }
            writer.commit();
            logFine("Rebuild completed for folder " + folder.getName());
        } catch (Exception e) {
            logSevere("Failed to rebuild index for folder " + folder.getName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // ScanResult delta integration
    // ------------------------------------------------------------------------

    public void updateIndex(ScanResult scanResult) {
        Reject.ifNull(scanResult, "ScanResult");

        if (!scanResult.isChangeDetected()) {
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

    public List<FileInfo> searchFiles(String query, int maxResults) {
        Reject.ifBlank(query, "query");

        List<FileInfo> results = new ArrayList<>();

        try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Search both content and name fields
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setAllowLeadingWildcard(true);
            org.apache.lucene.search.Query contentQuery = parser.parse(query);

            Query nameQuery = new TermQuery(new Term("name", query.toLowerCase()));

            BooleanQuery combinedQuery = new BooleanQuery.Builder()
                    .add(contentQuery, BooleanClause.Occur.SHOULD)
                    .add(nameQuery, BooleanClause.Occur.SHOULD)
                    .build();

            TopDocs topDocs = searcher.search(combinedQuery, maxResults > 0 ? maxResults : 100);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                String relPath = doc.get("relativePath");

                if (relPath == null) continue;
                FileInfo lookup = FileInfoFactory.lookupInstance(folder.getInfo(), relPath);
                FileInfo fileInfo = folder.getFile(lookup);
                if (fileInfo != null) results.add(fileInfo);
            }

            logFine("Lucene search for '" + query + "' returned " + results.size() + " hits in folder " + folder.getName());
        } catch (Exception e) {
            logWarning("Lucene search failed for query '" + query + "' in folder " +
                    folder.getName() + ": " + e.getMessage());
        }

        return results;
    }

    private int safeSize(Collection<?> c) {
        return c == null ? 0 : c.size();
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
            BodyContentHandler handler = new BodyContentHandler(5 * 1024 * 1024);
            AutoDetectParser parser = new AutoDetectParser();  // thread-safe now
            parser.parse(stream, handler, metadata);

            String content = handler.toString();
            logFiner("Extracted text from " + fileInfo + " (" +
                    metadata.get(Metadata.CONTENT_TYPE) + ")");
            return content;

        } catch (IOException | SAXException | TikaException e) {
            // Fallback to OCR for supported binary formats
            if (ocrEnabled && filePath.toString().matches(".*\\.(png|jpg|jpeg|tif|tiff|bmp|pdf)$")) {
                String ocrText = ocrEngine.performOCR(filePath);
                if (ocrText != null && !ocrText.isBlank()) {
                    logFiner("Extracted OCR text from " + fileInfo);
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
