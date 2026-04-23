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
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.IOProvider;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.logging.Loggable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Per-folder Lucene index manager with asynchronous background indexing.
 * <p>
 * All indexing is non-blocking: callers add {@link FileInfo} objects to a
 * queue, and a single background worker (running on the shared
 * {@link IOProvider} thread pool) processes them one by one. Each file
 * gets full indexing: metadata, Tika text extraction, and OCR fallback
 * if applicable.
 * <p>
 * Searches are synchronous and run against the latest committed state.
 */
public class LuceneIndexManager extends Loggable {

    // -----------------------------------------------------------------------
    // Index versioning — bump when Lucene/Tika/OCR libs change in a way
    // that makes existing index data incompatible or stale.
    // -----------------------------------------------------------------------
    private static final int INDEX_FORMAT_VERSION = 3;
    private static final String META_FILE_NAME = ".index_meta";

    /** Searchable fields used by all query methods. */
    private static final String[] SEARCH_FIELDS =
            {"name", "relativeName", "content"};

    /** Pattern matching file extensions eligible for OCR fallback. */
    private static final Pattern OCR_ELIGIBLE_PATTERN =
            Pattern.compile(".*\\.(png|jpg|jpeg|tif|tiff|bmp|pdf)$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Files indexed before an automatic commit+refresh.
     * Override with {@code powerfolder.index.commitInterval}.
     */
    private static final int COMMIT_INTERVAL =
            Integer.getInteger("powerfolder.index.commitInterval", 50);

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private final Folder folder;
    private final IOProvider ioProvider;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;
    private final TesseractOCR ocrEngine;

    private final LinkedBlockingQueue<FileInfo> indexQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger uncommittedCount = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** True while the worker is running on the IO thread pool. */
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);

    private volatile boolean extractContentEnabled = true;
    private volatile boolean ocrEnabled = true;

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    public LuceneIndexManager(Folder folder, IOProvider ioProvider)
            throws IOException {
        super();
        Reject.ifNull(folder, "Folder");
        Reject.ifNull(ioProvider, "IOProvider");
        this.folder = folder;
        this.ioProvider = ioProvider;
        this.indexPath = folder.getSystemSubDir().resolve("index");
        Files.createDirectories(indexPath);

        this.analyzer = new StandardAnalyzer();

        IndexWriter w = null;
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            FSDirectory directory = FSDirectory.open(indexPath);
            w = new IndexWriter(directory, config);
        } catch (Exception e) {
            // Index incompatible (e.g. created by a newer/older Lucene
            // version) or corrupt. Wipe and start fresh.
            logWarning(folder
                    + ": Incompatible or corrupt index, rebuilding: "
                    + e.getMessage());
            if (w != null) {
                try { w.close(); } catch (Exception ignored) {}
                w = null;
            }
            deleteIndexFiles();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            FSDirectory directory = FSDirectory.open(indexPath);
            w = new IndexWriter(directory, config);
        }

        try {
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

    /**
     * Deletes all files in the index directory. Used when the existing
     * index is incompatible (e.g. created by a different Lucene major
     * version) or corrupt. Also removes the meta file so
     * {@link #rebuildIndexIfRequired()} detects the need for a full
     * rebuild on next call.
     */
    private void deleteIndexFiles() {
        try {
            if (Files.exists(indexPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexPath)) {
                    for (Path file : stream) {
                        Files.deleteIfExists(file);
                    }
                }
            }
            logFine(folder + ": Index files deleted");
        } catch (IOException e) {
            logWarning(folder + ": Failed to delete index files: "
                    + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Configuration toggles
    // ------------------------------------------------------------------------

    public void setExtractContentEnabled(boolean enabled) {
        this.extractContentEnabled = enabled;
    }

    public void setOcrEnabled(boolean enabled) {
        this.ocrEnabled = enabled;
    }

    // ------------------------------------------------------------------------
    // Smart rebuild check
    // ------------------------------------------------------------------------

    /**
     * Determines whether the index needs a full rebuild. Checks:
     * <ol>
     *   <li>Index format version (detects Lucene/Tika/OCR library
     *       upgrades)</li>
     *   <li>File-count plausibility (stored count vs. actual file
     *       count)</li>
     *   <li>Index corruption (cannot open a reader)</li>
     * </ol>
     *
     * @param currentFileCount the number of files currently known to
     *                         the folder
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
                logFine(folder + ": File count drift (stored="
                        + storedFileCount + ", actual="
                        + currentFileCount
                        + ") — rebuild required");
                return true;
            }
        } catch (Exception e) {
            logWarning(folder + ": Failed to read index meta: "
                    + e.getMessage() + " — rebuild required");
            return true;
        }

        // 3) Verify index is readable
        try {
            IndexSearcher s = searcherManager.acquire();
            try { s.getIndexReader().numDocs(); }
            finally { searcherManager.release(s); }
        } catch (Exception e) {
            logWarning(folder + ": Index corrupt — rebuild required");
            return true;
        }

        logFine(folder + ": Index is up-to-date");
        return false;
    }

    /**
     * Persists index metadata after a successful rebuild or significant
     * update.
     */
    private void writeIndexMeta(int fileCount) {
        Path metaFile = indexPath.resolve(META_FILE_NAME);
        Properties props = new Properties();
        props.setProperty("formatVersion",
                String.valueOf(INDEX_FORMAT_VERSION));
        props.setProperty("fileCount", String.valueOf(fileCount));
        props.setProperty("lastRebuilt",
                String.valueOf(System.currentTimeMillis()));

        try (OutputStream out = Files.newOutputStream(metaFile)) {
            props.store(out,
                    "PowerFolder Lucene index metadata — do not edit");
        } catch (IOException e) {
            logWarning(folder + ": Failed to write index meta: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Public API — non-blocking, queue-based
    // ------------------------------------------------------------------------

    /**
     * Queues files for background indexing. Returns immediately.
     */
    public void indexFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        indexQueue.addAll(files);
        ensureWorkerRunning();
        logFine(folder + ": Queued " + files.size() + " files");
    }

    /**
     * Queues deleted files for re-indexing with deleted flag.
     * Returns immediately. The worker reads {@code isDeleted()} and
     * sets the flag accordingly.
     */
    public void markDeleted(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        indexQueue.addAll(files);
        ensureWorkerRunning();
    }

    /**
     * Permanently removes files from the index. Synchronous — purge
     * must be immediate so recycle bin views don't show stale entries.
     */
    public void purgeFiles(Collection<FileInfo> files) {
        if (files == null || files.isEmpty()) return;
        try {
            for (FileInfo fileInfo : files) {
                writer.deleteDocuments(
                        new Term("docId", buildDocId(fileInfo)));
            }
            commitAndRefresh();
            logFine(folder + ": Purged " + files.size() + " files from index");
        } catch (Exception e) {
            logWarning(folder + ": Purge failed: " + e.getMessage());
        }
    }

    /**
     * Wipes the index synchronously and queues all files for
     * re-indexing in the background.
     */
    public void rebuildIndex(Collection<FileInfo> allFiles) {
        indexQueue.clear();

        try {
            writer.deleteAll();
            commitAndRefresh();
            logFine(folder + ": Index wiped for rebuild");
        } catch (Exception e) {
            logWarning(folder + ": Failed to wipe index: "
                    + e.getMessage());
            return;
        }

        int count = 0;
        if (allFiles != null) {
            indexQueue.addAll(allFiles);
            count = allFiles.size();
        }

        writeIndexMeta(count);
        ensureWorkerRunning();
        logInfo(folder + ": Rebuild queued — " + count + " files");
    }

    public boolean rebuildIndexIfRequired() {
        Collection<FileInfo> allItems = new ArrayList<>();
        allItems.addAll(folder.getKnownFiles());
        allItems.addAll(folder.getKnownDirectories());

        if (isRebuildRequired(allItems.size())) {
            rebuildIndex(allItems);
            return true;
        }
        return false;
    }

    public void updateIndex(ScanResult scanResult) {
        Reject.ifNull(scanResult, "ScanResult");
        if (!scanResult.isChangeDetected()) return;

        int count = 0;
        for (FileInfo f : safe(scanResult.getNewFiles())) {
            indexQueue.add(f); count++;
        }
        for (FileInfo f : safe(scanResult.getChangedFiles())) {
            indexQueue.add(f); count++;
        }
        for (FileInfo f : safe(scanResult.getRestoredFiles())) {
            indexQueue.add(f); count++;
        }
        for (FileInfo f : safe(scanResult.getDeletedFiles())) {
            indexQueue.add(f); count++;
        }

        ensureWorkerRunning();
        logFine(folder + ": Queued " + count + " index updates");
    }

    // ------------------------------------------------------------------------
    // Worker management
    // ------------------------------------------------------------------------

    /**
     * Ensures exactly one worker is running on the IO thread pool.
     * If the worker has already finished (queue was empty) and new
     * work arrives, it is restarted.
     */
    private void ensureWorkerRunning() {
        if (closed.get()) return;
        if (workerRunning.compareAndSet(false, true)) {
            ioProvider.startIO(this::workerLoop);
        }
    }

    /**
     * Single worker loop: polls FileInfo objects from the queue and
     * indexes them. Commits periodically. Exits when the queue is
     * empty — will be restarted by {@link #ensureWorkerRunning()}
     * when new work arrives.
     */
    private void workerLoop() {
        logFine(folder + ": Index worker started");

        try {
            while (!closed.get()) {
                FileInfo fileInfo = indexQueue.poll(100, TimeUnit.MILLISECONDS);

                if (fileInfo == null) {
                    // Queue empty — commit remaining and exit.
                    // Worker will be restarted when new work arrives.
                    if (uncommittedCount.get() > 0) {
                        commitAndRefresh();
                    }
                    break;
                }

                try {
                    doIndexFile(fileInfo);
                } catch (Exception e) {
                    logWarning(folder + ": Failed to index "
                            + fileInfo.getFilenameOnly() + ": "
                            + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Final commit for anything remaining
            if (uncommittedCount.get() > 0) {
                commitAndRefresh();
            }
            workerRunning.set(false);

            // Race guard: if items were added between the poll timeout
            // and workerRunning.set(false), restart the worker.
            if (!indexQueue.isEmpty() && !closed.get()) {
                ensureWorkerRunning();
            }

            logFine(folder + ": Index worker stopped (pending: "
                    + indexQueue.size() + ")");
        }
    }

    // ------------------------------------------------------------------------
    // Core indexing (called by worker)
    // ------------------------------------------------------------------------

    private void doIndexFile(FileInfo fileInfo) {
        try {
            String docId = buildDocId(fileInfo);
            Document doc = new Document();

            doc.add(new StringField("docId", docId, Field.Store.YES));
            doc.add(new StringField("folderId", folder.getId(), Field.Store.YES));
            doc.add(new StoredField("folderName", folder.getName()));
            doc.add(new TextField("name", fileInfo.getFilenameOnly(), Field.Store.YES));
            doc.add(new TextField("extension", fileInfo.getExtension(), Field.Store.YES));
            doc.add(new StringField("extensionExact", fileInfo.getExtension().toLowerCase(Locale.ROOT), Field.Store.NO));
            doc.add(new TextField("relativeName", fileInfo.getRelativeName(), Field.Store.YES));
            doc.add(new StringField("path", fileInfo.getRelativeName().toLowerCase(Locale.ROOT), Field.Store.NO));
            doc.add(new LongPoint("modified",
                    fileInfo.getModifiedDate() != null
                            ? fileInfo.getModifiedDate().getTime()
                            : 0));
            doc.add(new StoredField("size", fileInfo.getSize()));

            AccountInfo modAccount = fileInfo.getModifiedByAccount();
            if (modAccount != null) {
                String modifiedByValue = modAccount.getDisplayName() + " " + modAccount.getUsername();
                MemberInfo modDevice = fileInfo.getModifiedBy();
                if (modDevice != null && modDevice.nick != null) {
                    modifiedByValue += " " + modDevice.nick;
                }
                doc.add(new TextField("modifiedBy", modifiedByValue.toLowerCase(Locale.ROOT), Field.Store.YES));
            }

            boolean deleted = fileInfo.isDeleted();
            doc.add(new StringField("deleted",
                    deleted ? "true" : "false", Field.Store.YES));

            if (extractContentEnabled && !deleted) {
                String content = extractContent(fileInfo);
                if (content != null && !content.isBlank()) {
                    doc.add(new TextField("content", content,
                            Field.Store.NO));
                }
            }

            writer.updateDocument(new Term("docId", docId), doc);

            if (isFiner()) {
                logFiner(folder + ": Indexed: "
                        + fileInfo.getFilenameOnly()
                        + (deleted ? " [deleted]" : ""));
            }

            if (uncommittedCount.incrementAndGet() >= COMMIT_INTERVAL) {
                commitAndRefresh();
            }
        } catch (Exception e) {
            logWarning(folder + ": Index error for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Content extraction (Tika + OCR)
    // ------------------------------------------------------------------------

    private String extractContent(FileInfo fileInfo) {
        Path filePath = fileInfo.getDiskFile(folder);
        if (filePath == null || !Files.exists(filePath)) return null;

        // Tika
        try (InputStream stream = Files.newInputStream(filePath)) {
            BodyContentHandler handler =
                    new BodyContentHandler(-1);
            new AutoDetectParser().parse(stream, handler,
                    new Metadata());
            String text = handler.toString();
            if (text != null && !text.isBlank()) return text;
        } catch (IOException | SAXException | TikaException e) {
            logFiner(folder + ": Tika failed for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
        }

        // OCR fallback
        if (ocrEnabled && OCR_ELIGIBLE_PATTERN.matcher(
                filePath.toString()).matches()) {
            try {
                String ocrText = ocrEngine.performOCR(filePath);
                if (ocrText != null && !ocrText.isBlank()) {
                    logFine(folder + ": OCR extracted "
                            + ocrText.length() + " chars from "
                            + fileInfo.getFilenameOnly());
                    return ocrText;
                }
            } catch (NoClassDefFoundError e) {
                logWarning(folder + ": OCR unavailable for "
                        + fileInfo.getFilenameOnly());
            }
        }

        return null;
    }

    // ------------------------------------------------------------------------
    // Search (synchronous)
    // ------------------------------------------------------------------------

    /**
     * Searches for files matching the query text. By default, deleted
     * files are excluded from results.
     *
     * @param queryText  the user's search query
     * @param maxResults maximum number of results to return
     * @return matching FileInfo objects (non-deleted only)
     */
    public List<FileInfo> searchFiles(String queryText, int maxResults) {
        return doSearch(queryText, maxResults, DeletedFilter.EXCLUDE,
                null, null, null);
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
                includeDeleted ? DeletedFilter.INCLUDE : DeletedFilter.EXCLUDE,
                null, null, null);
    }

    /**
     * Searches for deleted files only — intended for recycle bin views.
     */
    public List<FileInfo> searchDeletedFiles(String queryText,
                                             int maxResults) {
        return doSearch(queryText, maxResults, DeletedFilter.ONLY,
                null, null, null);
    }

    /**
     * Searches for files matching the given {@link FileInfoCriteria}.
     * All filtering (keywords, deleted, directory path, extension,
     * modifiedBy) is performed by Lucene in a single query.
     *
     * @param criteria the search criteria
     * @return matching FileInfo objects
     */
    public List<FileInfo> searchFiles(FileInfoCriteria criteria) {
        Reject.ifNull(criteria, "FileInfoCriteria");

        int maxResults = criteria.getMaxResults() > 0 ? criteria.getMaxResults() : 10_000;
        DeletedFilter deletedFilter = criteria.includeDeleted() ? DeletedFilter.INCLUDE : DeletedFilter.EXCLUDE;
        String queryText = String.join(" ", criteria.getKeyWords());

        return doSearch(queryText, maxResults, deletedFilter,
                criteria.getPath(), criteria.getExtension(), criteria.getModifiedBy());
    }

    /** Controls how the deleted flag is handled in searches. */
    private enum DeletedFilter { INCLUDE, EXCLUDE, ONLY }

    /**
     * Core search implementation. Builds a combined Lucene query that
     * handles all filtering in one pass:
     * <ul>
     *   <li><b>Keywords:</b> per-token exact / prefix / wildcard
     *       queries across searchable fields</li>
     *   <li><b>Deleted filter:</b> include, exclude, or only deleted</li>
     *   <li><b>Directory:</b> {@link PrefixQuery} on path (non-analyzed)</li>
     *   <li><b>Extension:</b> exact {@link TermQuery} on extensionExact (non-analyzed)</li>
     *   <li><b>ModifiedBy:</b> {@link WildcardQuery} on the modifiedBy
     *       field (display name, username, device nick)</li>
     * </ul>
     * {@code TopDocs} returned by Lucene contains the final matching
     * items, which are then converted to FileInfo instances.
     *
     * @param queryText     the user's search keywords
     * @param maxResults    maximum number of results
     * @param deletedFilter how to handle the deleted flag
     * @param directory     restrict to files under this path (may be null)
     * @param extension     restrict to this file extension (may be null)
     * @param modifiedBy    restrict to files modified by this user (may be null)
     * @return matching FileInfo objects
     */
    private List<FileInfo> doSearch(String queryText, int maxResults,
                                    DeletedFilter deletedFilter,
                                    String directory, String extension,
                                    String modifiedBy) {
        List<FileInfo> results = new ArrayList<>();
        boolean hasKeywords = StringUtils.isNotBlank(queryText);

        IndexSearcher searcher = null;
        try {
            searcherManager.maybeRefreshBlocking();
            searcher = searcherManager.acquire();

            BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();

            if (hasKeywords) {
                Query contentQuery = buildQuery(queryText);
                if (contentQuery != null) {
                    bqBuilder.add(contentQuery, BooleanClause.Occur.MUST);
                }
            }

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
                    break;
            }

            if (directory != null && !directory.isEmpty() && !"/".equals(directory)) {
                String prefix = directory.endsWith("/") ? directory : directory + "/";
                bqBuilder.add(
                        new PrefixQuery(new Term("path", prefix.toLowerCase(Locale.ROOT))),
                        BooleanClause.Occur.MUST);
            }

            if (StringUtils.isNotBlank(extension)) {
                bqBuilder.add(
                        new TermQuery(new Term("extensionExact", extension.toLowerCase(Locale.ROOT))),
                        BooleanClause.Occur.MUST);
            }

            if (StringUtils.isNotBlank(modifiedBy)) {
                String term = modifiedBy.toLowerCase(Locale.ROOT).trim();
                bqBuilder.add(
                        new WildcardQuery(new Term("modifiedBy", "*" + term + "*")),
                        BooleanClause.Occur.MUST);
            }

            Query finalQuery = bqBuilder.build();
            TopDocs topDocs = searcher.search(finalQuery, maxResults);

            logInfo(folder + ": Found " + topDocs.totalHits
                    + " hits for query '" + queryText + "'");

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc =
                        searcher.storedFields().document(scoreDoc.doc);
                String relPath = doc.get("relativeName");
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
            logWarning(folder + ": Lucene search failed for '"
                    + queryText + "': " + e.getMessage());
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) {
                    logWarning(folder
                            + ": Failed to release searcher: "
                            + e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Builds a Lucene query from user input. For each sanitized token,
     * creates a per-field disjunction of exact / prefix / wildcard
     * queries with boosting to prefer exact matches. All tokens are
     * combined with AND semantics.
     *
     * @return the composed query, or null if input is empty after
     *         sanitization
     */
    private Query buildQuery(String queryText) {
        String sanitized = queryText.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isEmpty()) return null;

        String[] tokens = sanitized.split("\\s+");
        BooleanQuery.Builder allTokens = new BooleanQuery.Builder();

        for (String token : tokens) {
            BooleanQuery.Builder fieldDisjunction =
                    new BooleanQuery.Builder();

            for (String field : SEARCH_FIELDS) {
                fieldDisjunction.add(
                        new BoostQuery(new TermQuery(
                                new Term(field, token)), 3.0f),
                        BooleanClause.Occur.SHOULD);

                fieldDisjunction.add(
                        new BoostQuery(new PrefixQuery(
                                new Term(field, token)), 2.0f),
                        BooleanClause.Occur.SHOULD);

                if (token.length() <= 12) {
                    fieldDisjunction.add(
                            new WildcardQuery(new Term(field, "*" + token + "*")),
                            BooleanClause.Occur.SHOULD);
                }
            }

            fieldDisjunction.setMinimumNumberShouldMatch(1);
            allTokens.add(fieldDisjunction.build(),
                    BooleanClause.Occur.MUST);
        }

        return allTokens.build();
    }

    // ------------------------------------------------------------------------
    // Commit helper
    // ------------------------------------------------------------------------

    private void commitAndRefresh() {
        try {
            writer.commit();
            searcherManager.maybeRefresh();
            uncommittedCount.set(0);
        } catch (Exception e) {
            logWarning(folder + ": Commit failed: "
                    + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------------

    public int getIndexEntryCount() {
        try {
            IndexSearcher s = searcherManager.acquire();
            try { return s.getIndexReader().numDocs(); }
            finally { searcherManager.release(s); }
        } catch (Exception e) {
            return -1;
        }
    }

    /** Number of files waiting to be indexed. */
    public int getPendingCount() {
        return indexQueue.size();
    }

    // ------------------------------------------------------------------------
    // Shutdown
    // ------------------------------------------------------------------------

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) return;
        logFine(folder + ": Shutting down...");

        // Drain remaining queue items
        FileInfo f;
        while ((f = indexQueue.poll()) != null) {
            try { doIndexFile(f); }
            catch (Exception ignored) {}
        }

        commitAndRefresh();
        try { searcherManager.close(); }
        catch (Exception ignored) {}
        try { writer.close(); }
        catch (Exception ignored) {}
        ocrEngine.shutdown();

        logFine(folder + ": Shutdown complete");
    }

    // ------------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------------

    private String buildDocId(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo");
        return folder.getId() + ":" + fileInfo.getRelativeName();
    }

    private static Collection<FileInfo> safe(Collection<FileInfo> c) {
        return c != null ? c : Collections.emptyList();
    }
}