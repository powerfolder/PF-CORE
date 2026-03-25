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
import de.dal33t.powerfolder.net.IOProvider;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Per-folder Lucene index manager. Maintains a full-text search index that
 * includes both live and deleted files (deleted files carry a flag and can
 * be searched separately for recycle-bin views).
 * <p>
 * Thread safety: all public methods that mutate the index acquire a write
 * lock; search methods acquire a read lock. The underlying {@link IndexWriter}
 * is itself thread-safe for writes, but we need to coordinate commit + refresh
 * sequences and protect the {@link SearcherManager} lifecycle.
 */
public class LuceneIndexManager extends Loggable {

    // -----------------------------------------------------------------------
    // Index versioning — bump when Lucene/Tika/OCR libs change in a way
    // that makes existing index data incompatible or stale.
    // -----------------------------------------------------------------------
    private static final String META_FILE_NAME = ".index_meta";

    /** Searchable fields used by all query methods. */
    private static final String[] SEARCH_FIELDS =
            {"name", "relativePath", "content"};

    private final Folder folder;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;
    private final TesseractOCR ocrEngine;

    /** Read-write lock: write-lock for mutations, read-lock for searches. */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile boolean extractContentEnabled = true;
    private volatile boolean ocrEnabled = true;
    private volatile boolean closed = false;

    // ------------------------------------------------------------------------
    // Construction — guarded so partial init cleans up
    // ------------------------------------------------------------------------

    public LuceneIndexManager(Folder folder) throws IOException {
        super();
        this.folder = folder;
        this.indexPath = folder.getSystemSubDir().resolve("index");
        Files.createDirectories(indexPath);

        this.analyzer = new StandardAnalyzer();

        // Guard: if anything after the writer creation throws, close the
        // writer so we don't leak file handles or lock files.
        IndexWriter w = null;
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            FSDirectory directory = FSDirectory.open(indexPath);
            w = new IndexWriter(directory, config);
            this.searcherManager =
                    new SearcherManager(w, true, true, null);
            this.writer = w;
        } catch (IOException e) {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }
            }
            throw e;
        }

        this.ocrEngine = TesseractOCR.getInstance();

        logFine(folder + ": Lucene index initialized at " + indexPath.toAbsolutePath());
    }

    // ------------------------------------------------------------------------
    // Configuration toggles
    // ------------------------------------------------------------------------

    public void setExtractContentEnabled(boolean enabled) {
        this.extractContentEnabled = enabled;
        logFine(folder + ": Content extraction " + (enabled ? "enabled" : "disabled"));
    }

    public void setOcrEnabled(boolean enabled) {
        this.ocrEnabled = enabled;
        logFine(folder + ": OCR " + (enabled ? "enabled" : "disabled"));
    }

    // ------------------------------------------------------------------------
    // Smart rebuild check
    // ------------------------------------------------------------------------

    /**
     * Determines whether the index needs a full rebuild. Checks:
     * <ol>
     *   <li>Index format version (detects Lucene/Tika/OCR library upgrades)</li>
     *   <li>File-count plausibility (stored count vs. actual file count)</li>
     *   <li>Index corruption (cannot open a reader)</li>
     * </ol>
     *
     * @param currentFileCount the number of files currently known to the folder
     * @return true if a rebuild is required
     */
    public boolean isRebuildRequired(int currentFileCount) {
        Path metaFile = indexPath.resolve(META_FILE_NAME);

        // 1) No meta file → first run or wiped index
        if (!Files.exists(metaFile)) {
            logFine(folder + ": No index meta file found — rebuild required");
            return true;
        }

        // 2) Parse meta
        try {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(metaFile)) {
                props.load(in);
            }

            int storedVersion = Integer.parseInt(
                    props.getProperty("formatVersion", "0"));
            int storedFileCount = Integer.parseInt(
                    props.getProperty("fileCount", "-1"));

            // Version mismatch → library upgrade
            if (storedVersion != INDEX_FORMAT_VERSION) {
                logFine(folder + ": Index format version mismatch (stored="
                        + storedVersion + ", current=" + INDEX_FORMAT_VERSION
                        + ") — rebuild required");
                return true;
            }

            // Gross file-count mismatch (>20% drift or negative stored)
            if (storedFileCount < 0
                    || Math.abs(storedFileCount - currentFileCount)
                    > Math.max(currentFileCount * 0.20, 50)) {
                logFine(folder + ":File count drift (stored=" + storedFileCount
                        + ", actual=" + currentFileCount
                        + ") — rebuild required");
                return true;
            }
        } catch (Exception e) {
            logWarning(folder + ":Failed to read index meta: " + e.getMessage() + " — rebuild required");
            return true;
        }

        // 3) Verify index is readable
        rwLock.readLock().lock();
        try {
            searcherManager.maybeRefreshBlocking();
            IndexSearcher searcher = searcherManager.acquire();
            try {
                searcher.getIndexReader().numDocs(); // smoke test
            } finally {
                searcherManager.release(searcher);
            }
        } catch (Exception e) {
            logWarning(folder + ":Index corrupt or unreadable : " + e.getMessage() + " — rebuild required");
            return true;
        } finally {
            rwLock.readLock().unlock();
        }

        logFine(folder + ":Index is up-to-date");
        return false;
    }

    /**
     * Persists index metadata after a successful rebuild or significant update.
     */
    private void writeIndexMeta(int fileCount) {
        Path metaFile = indexPath.resolve(META_FILE_NAME);
        Properties props = new Properties();
        props.setProperty("formatVersion",
                String.valueOf(INDEX_FORMAT_VERSION));
        props.setProperty("fileCount", String.valueOf(fileCount));
        props.setProperty("lastRebuilt",
                String.valueOf(System.currentTimeMillis()));

        try (var out = Files.newOutputStream(metaFile)) {
            props.store(out,
                    "PowerFolder Lucene index metadata — do not edit");
        } catch (IOException e) {
            logWarning(folder + ":Failed to write index meta: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Indexing
    // ------------------------------------------------------------------------

    /**
     * Indexes a single file. Deleted files are kept in the index with a
     * {@code deleted=true} flag so they remain searchable in the recycle bin.
     * <p>
     * Caller must hold the write lock.
     */

    private static final int INDEX_FORMAT_VERSION = 1;

    private void indexFile(FileInfo fileInfo) {
        try {
            String docId = buildDocId(fileInfo);
            Document doc = new Document();

            doc.add(new StringField("docId", docId, Field.Store.YES));
            doc.add(new StringField("folderId", folder.getId(), Field.Store.YES));
            doc.add(new StoredField("folderName", folder.getName()));
            doc.add(new TextField("name", fileInfo.getFilenameOnly(), Field.Store.YES));
            doc.add(new TextField("extension", fileInfo.getExtension(), Field.Store.YES));
            doc.add(new TextField("relativeName", fileInfo.getRelativeName(), Field.Store.YES));
            doc.add(new LongPoint("modified",
                    fileInfo.getModifiedDate() != null
                            ? fileInfo.getModifiedDate().getTime() : 0));
            doc.add(new StoredField("size", fileInfo.getSize()));

            // Deleted flag — stored and indexed for filtering
            boolean deleted = fileInfo.isDeleted();
            doc.add(new StringField("deleted",
                    deleted ? "true" : "false", Field.Store.YES));

            // Content extraction (skip for deleted files — content is gone)
            if (extractContentEnabled && !deleted) {
                String content = extractContent(fileInfo);
                if (content != null && !content.isBlank()) {
                    // StandardAnalyzer handles case-folding and tokenization
                    // on both index and query side — no manual toLowerCase.
                    doc.add(new TextField("content", content,
                            Field.Store.NO));
                }
            }

            writer.updateDocument(new Term("docId", docId), doc);
            if (isFiner()) {
                logFiner(folder + ": Indexed file (queued): " + fileInfo
                        + (deleted ? " [deleted]" : ""));
            }
        } catch (Exception e) {
            logWarning(folder + ": Failed to index file " + fileInfo + ": "
                    + e.getMessage());
        }
    }

    public void indexFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        rwLock.writeLock().lock();
        try {
            for (FileInfo fileInfo : files) {
                indexFile(fileInfo);
            }
            commitAndRefresh();
            logFine(folder + ": Bulk indexed " + files.size() + " files");
        } catch (Exception e) {
            logWarning(folder + ": Bulk indexing failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Marks files as deleted in the index (updates the deleted flag to true)
     * rather than removing them, so they remain searchable in the recycle bin.
     */
    public void markDeleted(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        rwLock.writeLock().lock();
        try {
            for (FileInfo fileInfo : files) {
                indexFile(fileInfo); // re-indexes with deleted=true
            }
            commitAndRefresh();
            logFine(folder + ": Marked " + files.size()
                    + " files as deleted in index");
        } catch (Exception e) {
            logWarning(folder + ": Mark-deleted failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Permanently removes documents from the index. Use only for purging
     * files that have been removed from the recycle bin / version history.
     */
    public void purgeFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        rwLock.writeLock().lock();
        try {
            for (FileInfo fileInfo : files) {
                writer.deleteDocuments(
                        new Term("docId", buildDocId(fileInfo)));
            }
            commitAndRefresh();
            logFine(folder + ": Purged " + files.size() + " files from index");
        } catch (Exception e) {
            logWarning(folder + ":Purge failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Rebuilds the entire index from scratch. Deleted files are indexed with
     * their deleted flag preserved.
     */
    private void rebuildIndex(Collection<FileInfo> allFiles) {
        rwLock.writeLock().lock();
        try {
            logInfo(folder + ": Rebuilding Lucene index with " + allFiles.size() + " items.");
            writer.deleteAll();

            int count = 0;
            if (allFiles != null && !allFiles.isEmpty()) {
                for (FileInfo f : allFiles) {
                    indexFile(f); // handles both live and deleted files
                    count++;
                }
            }

            commitAndRefresh();
            writeIndexMeta(count);
            logFine(folder + ": Rebuild completed (" + count + " files)");
        } catch (Exception e) {
            logWarning(folder + ": Failed to rebuild index: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Conditionally rebuilds the index only if {@link #isRebuildRequired}
     * determines it is necessary.
     *
     * @param allFiles all files known to the folder (live + deleted)
     * @return true if a rebuild was performed
     */
    public boolean rebuildIndexIfRequired(IOProvider ioProvider, Collection<FileInfo> allFiles) {
        int fileCount = allFiles != null ? allFiles.size() : 0;
        if (isRebuildRequired(fileCount)) {
            ioProvider.startIO(() -> rebuildIndex(allFiles));
            return true;
        }
        return false;
    }

    public void updateIndex(ScanResult scanResult) {
        Reject.ifNull(scanResult, "ScanResult");
        if (!scanResult.isChangeDetected()) {
            logFiner(folder + ": No changes detected");
            return;
        }
        rwLock.writeLock().lock();
        try {
            for (FileInfo f : safe(scanResult.getNewFiles())) {
                indexFile(f);
            }
            for (FileInfo f : safe(scanResult.getChangedFiles())) {
                indexFile(f);
            }
            for (FileInfo f : safe(scanResult.getRestoredFiles())) {
                indexFile(f);
            }
            // Mark deleted rather than remove — keeps them searchable
            // in recycle bin
            for (FileInfo f : safe(scanResult.getDeletedFiles())) {
                indexFile(f);
            }

            commitAndRefresh();

            // Update meta with approximate current count
            try {
                IndexSearcher s = searcherManager.acquire();
                try {
                    writeIndexMeta(s.getIndexReader().numDocs());
                } finally {
                    searcherManager.release(s);
                }
            } catch (Exception ignored) {
                // Non-critical — meta will be corrected on next rebuild
            }

            logFine(folder + ": Lucene index updated");
        } catch (Exception e) {
            logWarning(folder + ":Lucene index update failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ------------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------------

    /**
     * Searches for files matching the query text. By default, deleted files
     * are excluded from results.
     *
     * @param queryText  the user's search query
     * @param maxResults maximum number of results to return
     * @return matching FileInfo objects (non-deleted only)
     */
    public List<FileInfo> searchFiles(String queryText, int maxResults) {
        return doSearch(queryText, maxResults, DeletedFilter.EXCLUDE);
    }

    /**
     * Searches for files matching the query text, optionally including
     * deleted files (for recycle bin / version history views).
     *
     * @param queryText      the user's search query
     * @param maxResults     maximum number of results to return
     * @param includeDeleted if true, deleted files are included in results
     * @return matching FileInfo objects
     */
    public List<FileInfo> searchFiles(String queryText, int maxResults,
                                      boolean includeDeleted) {
        return doSearch(queryText, maxResults,
                includeDeleted ? DeletedFilter.INCLUDE
                        : DeletedFilter.EXCLUDE);
    }

    /**
     * Searches for deleted files only — intended for recycle bin views.
     */
    public List<FileInfo> searchDeletedFiles(String queryText,
                                             int maxResults) {
        return doSearch(queryText, maxResults, DeletedFilter.ONLY);
    }

    /** Controls how the deleted flag is handled in searches. */
    private enum DeletedFilter { INCLUDE, EXCLUDE, ONLY }

    /**
     * Core search implementation. Builds a query from the user input and
     * applies the requested deleted-file filter.
     * <p>
     * <b>Search strategy:</b> splits the sanitized input into individual
     * terms and, for each term, builds a per-field {@link BooleanQuery}
     * combining:
     * <ul>
     *   <li>An exact {@link TermQuery} (highest weight for exact matches)</li>
     *   <li>A {@link PrefixQuery} (matches terms starting with the input)</li>
     *   <li>A {@link WildcardQuery} {@code *term*} only for short terms
     *       (≤12 chars) to keep the cost bounded</li>
     * </ul>
     * This replaces the previous approach of wrapping the entire query in
     * leading+trailing wildcards, which bypassed Lucene's inverted index
     * and degraded as the index grew.
     */
    private List<FileInfo> doSearch(String queryText, int maxResults,
                                    DeletedFilter deletedFilter) {
        List<FileInfo> results = new ArrayList<>();
        if (queryText == null || queryText.isBlank()) return results;

        rwLock.readLock().lock();
        IndexSearcher searcher = null;
        try {
            searcherManager.maybeRefreshBlocking();
            searcher = searcherManager.acquire();

            Query contentQuery = buildQuery(queryText);
            if (contentQuery == null) return results;

            // Apply deleted-file filter
            BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
            bqBuilder.add(contentQuery, BooleanClause.Occur.MUST);

            switch (deletedFilter) {
                case EXCLUDE:
                    bqBuilder.add(
                            new TermQuery(new Term("deleted", "false")),
                            BooleanClause.Occur.MUST);
                    break;
                case ONLY:
                    bqBuilder.add(
                            new TermQuery(new Term("deleted", "true")),
                            BooleanClause.Occur.MUST);
                    break;
                case INCLUDE:
                default:
                    // no filter
                    break;
            }

            Query finalQuery = bqBuilder.build();
            TopDocs topDocs = searcher.search(finalQuery, maxResults);

            String filterLabel;
            switch (deletedFilter) {
                case ONLY:    filterLabel = " (deleted only)"; break;
                case INCLUDE: filterLabel = " (including deleted)"; break;
                default:      filterLabel = ""; break;
            }
            logInfo(folder + ": Found " + topDocs.totalHits + " hits for query '"
                    + queryText + "' " + filterLabel);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc =
                        searcher.storedFields().document(scoreDoc.doc);
                String relPath = doc.get("relativePath");
                if (relPath == null) continue;

                FileInfo lookup =
                        de.dal33t.powerfolder.light.FileInfoFactory
                                .lookupInstance(folder.getInfo(), relPath);
                FileInfo fileInfo = folder.getFile(lookup);
                if (fileInfo != null) {
                    results.add(fileInfo);
                }
            }
        } catch (Exception e) {
            logWarning(folder + ":Lucene search failed for '" + queryText + "': " + e.getMessage());
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) {
                    logWarning(folder + ":Failed to release searcher: "
                            + e.getMessage());
                }
            }
            rwLock.readLock().unlock();
        }
        return results;
    }

    /**
     * Builds a Lucene query from user input. For each sanitized token,
     * creates a per-field disjunction of exact / prefix / wildcard queries
     * with boosting to prefer exact matches. All tokens are combined with
     * AND semantics (every token must match in at least one field).
     *
     * @return the composed query, or null if input is empty after sanitization
     */
    private Query buildQuery(String queryText) {
        // Sanitize: keep Unicode letters, digits, and whitespace.
        String sanitized = queryText.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isEmpty()) return null;

        String[] tokens = sanitized.split("\\s+");

        // All tokens must match (AND)
        BooleanQuery.Builder allTokens = new BooleanQuery.Builder();

        for (String token : tokens) {
            // This token must match in at least one field (OR across fields)
            BooleanQuery.Builder fieldDisjunction =
                    new BooleanQuery.Builder();

            for (String field : SEARCH_FIELDS) {
                // Exact match — highest boost
                fieldDisjunction.add(
                        new BoostQuery(new TermQuery(
                                new Term(field, token)), 3.0f),
                        BooleanClause.Occur.SHOULD);

                // Prefix match — good for "invoice" matching "invoices"
                fieldDisjunction.add(
                        new BoostQuery(new PrefixQuery(
                                new Term(field, token)), 2.0f),
                        BooleanClause.Occur.SHOULD);

                // Substring wildcard — only for short tokens to keep
                // performance bounded. Leading wildcards are expensive
                // and should not be used on long terms.
                if (token.length() <= 12) {
                    fieldDisjunction.add(
                            new WildcardQuery(
                                    new Term(field, "*" + token + "*")),
                            BooleanClause.Occur.SHOULD);
                }
            }

            // At least one field must match this token
            fieldDisjunction.setMinimumNumberShouldMatch(1);
            allTokens.add(fieldDisjunction.build(),
                    BooleanClause.Occur.MUST);
        }

        return allTokens.build();
    }

    // ------------------------------------------------------------------------
    // Content extraction
    // ------------------------------------------------------------------------

    private String extractContent(FileInfo fileInfo) {
        if (!extractContentEnabled) return null;

        Path filePath = fileInfo.getDiskFile(folder);
        if (filePath == null || !Files.exists(filePath)) {
            logWarning(folder + ":extractContent: File not found "
                    + fileInfo.getRelativeName());
            return null;
        }

        logFine(folder + ": Extracting content from " + fileInfo.getFilenameOnly()
                + " (" + filePath + ")");
        long start = System.currentTimeMillis();

        // Tika extraction
        try (InputStream stream = Files.newInputStream(filePath)) {
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(stream, handler, metadata);
            String text = handler.toString();

            long duration = System.currentTimeMillis() - start;
            if (text != null && !text.isBlank()) {
                logFine(folder + ": Tika extracted " + text.length()
                        + " chars from " + fileInfo.getFilenameOnly()
                        + " in " + duration + " ms.");
                return text;
            } else {
                logFiner(folder + ": Tika found no text in "
                        + fileInfo.getFilenameOnly());
            }
        } catch (IOException | SAXException | TikaException e) {
            logFiner(folder + ": Tika extraction failed for "
                    + fileInfo.getFilenameOnly() + ": " + e.getMessage());
        }

        // OCR fallback for image-based files
        if (ocrEnabled && filePath.toString()
                .matches(".*\\.(png|jpg|jpeg|tif|tiff|bmp|pdf)$")) {
            try {
                logFine(folder + ": Running OCR fallback for "
                        + fileInfo.getFilenameOnly());
                String ocrText = ocrEngine.performOCR(filePath);
                if (ocrText != null && !ocrText.isBlank()) {
                    logFine(folder + ": OCR extracted " + ocrText.length()
                            + " chars from "
                            + fileInfo.getFilenameOnly());
                    return ocrText;
                } else {
                    logInfo(folder + ":OCR produced no text for "
                            + fileInfo.getFilenameOnly());
                }
            } catch (NoClassDefFoundError e) {
                logWarning(folder + ":OCR unavailable (missing jai-imageio-core or "
                        + "PDFBox mismatch). Skipping OCR for "
                        + fileInfo.getFilenameOnly());
            }
        }

        logFiner(folder + ":No extractable content found for "
                + fileInfo.getFilenameOnly());
        return null;
    }

    // ------------------------------------------------------------------------
    // Index statistics
    // ------------------------------------------------------------------------

    /**
     * Returns the total number of documents in the index (including deleted
     * files that are still indexed with a deleted flag).
     *
     * @return document count, or -1 if the count could not be determined
     */
    public int getIndexEntryCount() {
        rwLock.readLock().lock();
        IndexSearcher searcher = null;
        try {
            searcherManager.maybeRefreshBlocking();
            searcher = searcherManager.acquire();
            return searcher.getIndexReader().numDocs();
        } catch (Exception e) {
            logWarning(folder + ":Failed to get index entry count: " + e.getMessage());
            return -1;
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) {
                    logWarning(folder + ":Failed to release searcher: "
                            + e.getMessage());
                }
            }
            rwLock.readLock().unlock();
        }
    }

    // ------------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------------

    private String buildDocId(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo");
        return folder.getId() + ":" + fileInfo.getRelativeName();
    }

    /** Commits the writer and refreshes the searcher. Caller holds lock. */
    private void commitAndRefresh() {
        try {
            writer.commit();
        } catch (Exception e) {
            logWarning(folder + ":Commit failed: "
                    + e.getMessage());
        }
        try {
            searcherManager.maybeRefresh();
        } catch (IOException e) {
            logWarning(folder + ":Searcher refresh failed: " + e.getMessage());
        }
    }

    public void commit() {
        rwLock.writeLock().lock();
        try {
            writer.commit();
        } catch (Exception e) {
            logWarning(folder + ":Commit failed: "
                    + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void shutdown() {
        if (closed) return;
        rwLock.writeLock().lock();
        try {
            closed = true;
            try {
                writer.commit();
            } catch (Exception e) {
                logWarning(folder + ":Final commit failed: " + e.getMessage());
            }
            try {
                searcherManager.close();
            } catch (Exception e) {
                logWarning(folder + ":Failed to close SearcherManager: " + e.getMessage());
            }
            try {
                writer.close();
                logFine(folder + ": Lucene index closed");
            } catch (Exception e) {
                logWarning(folder + ": Failed to close Lucene index: " + e.getMessage());
            }
            ocrEngine.shutdown();
            logFine(folder + ": Shutdown complete");
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Null-safe wrapper for collections from ScanResult. */
    private static Collection<FileInfo> safe(Collection<FileInfo> c) {
        return c != null ? c : Collections.emptyList();
    }
}