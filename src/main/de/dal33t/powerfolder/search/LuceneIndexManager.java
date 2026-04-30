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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
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

import org.apache.tika.parser.ParseContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
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
 * queue, and a single background worker processes them. Workers run on a
 * dedicated bounded thread pool ({@code IOProvider.startIndexing()}) so
 * that concurrent indexing across all folders is capped, preventing
 * CPU/IO saturation during startup. PFS-5311.
 * <p>
 * Searches are synchronous and run against the latest committed state.
 */
public class LuceneIndexManager extends PFComponent {

    private static final String META_FILE_NAME = ".index_meta";

    /** Searchable fields used by all query methods. */
    private static final String[] SEARCH_FIELDS =
            {"fileName", "relativeName", "content"};

    /** Pattern matching file extensions eligible for OCR fallback. */
    private static final Pattern OCR_ELIGIBLE_PATTERN =
            Pattern.compile(".*\\.(png|jpg|jpeg|tif|tiff|bmp|pdf)$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Detects UTF-8 mojibake: multi-byte UTF-8 sequences that were decoded
     * as Latin-1/Windows-1252, producing telltale two-char sequences.
     * E.g. ä (UTF-8: C3 A4) becomes "Ã¤",
     *      ö becomes "Ã¶", ü becomes "Ã¼".
     * Covers Latin-1 Supplement (U+0080-U+00FF) and Latin Extended-A/B.
     */
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "[Â-Å][-¿]");

    private static final char REPLACEMENT_CHAR = '�';

    /**
     * Files smaller than this are content-extracted inline in phase 1
     * (Tika only, no OCR). Default: 32 KB — covers ~65% of files
     * in typical enterprise environments (log-normal distribution,
     * median ~4 KB). Keeps phase 1 fast (~5-15 ms/file via Tika)
     * so metadata search is available quickly.
     * <p>
     * Source: Douceur &amp; Bolosky, "A Large-Scale Study of
     * File-System Contents", Microsoft Research 1999, 140M files.
     */
    private static final long INLINE_EXTRACT_MAX_SIZE =
            Long.getLong("powerfolder.index.inlineExtractMaxSize",
                    32L * 1024);

    /**
     * Maximum extracted text length (characters) Tika is allowed to
     * produce. Prevents OOM on huge files (e.g. multi-GB CSVs).
     * Default: 10 MB of text.
     */
    private static final int TIKA_WRITE_LIMIT =
            Integer.getInteger("powerfolder.index.tikaWriteLimit",
                    10 * 1024 * 1024);

    /**
     * Files larger than this are skipped for content extraction
     * entirely. Default: 512 MB. Avoids feeding huge files into
     * Tika where extraction time and memory usage are excessive.
     * PFS-5487.
     */
    private static final long CONTENT_EXTRACT_MAX_SIZE =
            Long.getLong("powerfolder.index.contentExtractMaxSize",
                    512L * 1024 * 1024);

    /**
     * Throttle delay (ms) between phase 2 content extractions to
     * prevent CPU/IO saturation during large index builds.
     * Default: 5 ms. Set to 0 to disable throttling.
     * PFS-5487: Munin stats showed 100% CPU saturation on all
     * three production servers during initial index build.
     */
    private static final long CONTENT_EXTRACT_THROTTLE_MS =
            Long.getLong("powerfolder.index.contentExtractThrottleMs", 5L);

    /**
     * Files indexed before an automatic commit+refresh.
     * Override with {@code powerfolder.index.commitInterval}.
     */
    private static final int COMMIT_INTERVAL =
            Integer.getInteger("powerfolder.index.commitInterval", 50);

    // -----------------------------------------------------------------------
    // Shared Tika parser — expensive to construct, safe to reuse.
    // AutoDetectParser and the underlying CompositeParser are thread-safe
    // (Tika creates per-call SAX handler state, not per-parser state).
    // -----------------------------------------------------------------------
    private static volatile AutoDetectParser SHARED_PARSER;
    private static final Object PARSER_LOCK = new Object();

    private static final long STARTUP_DELAY_MS = 60_000L;

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private final Folder folder;
    private final Path indexPath;
    private final StandardAnalyzer analyzer;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;
    private final TesseractOCR ocrEngine;

    private final LinkedBlockingQueue<FileInfo> indexQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<FileInfo> contentQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger uncommittedCount = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** True while the worker is running on the IO thread pool. */
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);

    /** True while a full rebuild is in progress. */
    private final AtomicBoolean rebuilding = new AtomicBoolean(false);

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    public LuceneIndexManager(Controller controller, Folder folder)
            throws IOException {
        super(controller);
        Reject.ifNull(folder, "Folder");
        this.folder = folder;
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
            logWarning(folder + ": Failed to delete index files: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Configuration (live from ConfigurationEntry)
    // ------------------------------------------------------------------------

    private boolean isExtractContentEnabled() {
        return ConfigurationEntry.SEARCH_INDEX_CONTENT_EXTRACTION_ENABLED.getValueBoolean(getController());
    }

    private boolean isOcrEnabled() {
        return ConfigurationEntry.SEARCH_INDEX_OCR_ENABLED.getValueBoolean(getController());
    }

    private static AutoDetectParser getSharedParser() {
        if (SHARED_PARSER == null) {
            synchronized (PARSER_LOCK) {
                if (SHARED_PARSER == null) {
                    SHARED_PARSER = new AutoDetectParser();
                }
            }
        }
        return SHARED_PARSER;
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
     *   <li>Content extraction or OCR newly enabled — PFS-5311</li>
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
        Properties meta;
        try {
            meta = new Properties();
            try (InputStream in = Files.newInputStream(metaFile)) {
                meta.load(in);
            }

            int storedVersion = Integer.parseInt(
                    meta.getProperty("formatVersion", "0"));
            int storedFileCount = Integer.parseInt(
                    meta.getProperty("fileCount", "-1"));

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

        // 4) PFS-5311: content extraction was off and is now on
        boolean prevContentExtraction = Boolean.parseBoolean(
                meta.getProperty("contentExtraction.enabled", "false"));
        if (isExtractContentEnabled() && !prevContentExtraction) {
            logInfo(folder + ": Reindex required — content extraction enabled");
            return true;
        }

        // 5) PFS-5311: OCR was off and is now on
        boolean prevOcr = Boolean.parseBoolean(
                meta.getProperty("ocr.enabled", "false"));
        if (isOcrEnabled() && !prevOcr) {
            logInfo(folder + ": Reindex required — OCR enabled");
            return true;
        }

        logFine(folder + ": Index is up-to-date");
        return false;
    }

    /**
     * Persists index metadata and capabilities after a successful rebuild
     * or significant update.
     */
    private void writeIndexMeta(int fileCount) {
        Path metaFile = indexPath.resolve(META_FILE_NAME);
        Properties props = new Properties();
        props.setProperty("formatVersion",
                String.valueOf(INDEX_FORMAT_VERSION));
        props.setProperty("fileCount", String.valueOf(fileCount));
        props.setProperty("lastRebuilt",
                String.valueOf(System.currentTimeMillis()));
        props.setProperty("contentExtraction.enabled",
                String.valueOf(isExtractContentEnabled()));
        props.setProperty("ocr.enabled",
                String.valueOf(isOcrEnabled()));

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
        rebuilding.set(true);

        try {
            writer.deleteAll();
            commitAndRefresh();
            logFine(folder + ": Index wiped for rebuild");
        } catch (Exception e) {
            logWarning(folder + ": Failed to wipe index: "
                    + e.getMessage());
            rebuilding.set(false);
            return;
        }

        int count = 0;
        if (allFiles != null) {
            indexQueue.addAll(allFiles);
            count = allFiles.size();
        }

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
     * Ensures exactly one worker is running on the dedicated indexing
     * thread pool. If the worker has already finished (queue was empty)
     * and new work arrives, it is restarted.
     * <p>
     * PFS-5311: Uses {@code IOProvider.startIndexing()} which is
     * bounded, preventing hundreds of concurrent workers across folders.
     * Also respects the startup delay gate so indexing doesn't saturate
     * the node during boot.
     */
    private void ensureWorkerRunning() {
        if (closed.get()) return;
        if (workerRunning.compareAndSet(false, true)) {
            getController().getIOProvider().startIndexing(this::workerLoop);
        }
    }

    /**
     * PFS-5311: Waits until the server has been up for at least 60s
     * so indexing doesn't saturate the node during startup.
     *
     * @return true if the wait completed, false if interrupted
     */
    private boolean waitForStartup() {
        long uptime = getController().getUptime();
        if (uptime >= 0 && uptime < STARTUP_DELAY_MS) {
            long waitMs = STARTUP_DELAY_MS - uptime;
            logFine(folder + ": Indexing delayed " + (waitMs / 1000)
                    + "s (server uptime " + (uptime / 1000) + "s)");
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workerRunning.set(false);
                return false;
            }
        }
        return true;
    }

    /**
     * Single worker loop: polls FileInfo objects from the queue and
     * indexes them. Commits periodically. Exits when the queue is
     * empty — will be restarted by {@link #ensureWorkerRunning()}
     * when new work arrives.
     */
    private void workerLoop() {
        if (!waitForStartup()) {
            return;
        }

        logFine(folder + ": Index worker started");

        try {
            // Phase 1: index metadata (fast)
            while (!closed.get()) {
                FileInfo fileInfo = indexQueue.poll(100, TimeUnit.MILLISECONDS);
                if (fileInfo == null) {
                    if (uncommittedCount.get() > 0) {
                        commitAndRefresh();
                    }
                    break;
                }
                try {
                    doIndexFile(fileInfo);
                } catch (OutOfMemoryError oom) {
                    logSevere(folder + ": OOM while indexing "
                            + fileInfo.toDetailString() + ": "
                            + oom.getMessage());
                    throw oom;
                } catch (Exception e) {
                    logWarning(folder + ": Failed to index "
                            + fileInfo.getFilenameOnly() + ": "
                            + e.getMessage());
                }
            }

            rebuilding.set(false);

            // Phase 2: extract and index content (slow, Tika/OCR)
            while (!closed.get()) {
                FileInfo fileInfo = contentQueue.poll(100, TimeUnit.MILLISECONDS);
                if (fileInfo == null) {
                    if (uncommittedCount.get() > 0) {
                        commitAndRefresh();
                    }
                    // Both phases done — persist meta so rebuild isn't
                    // re-triggered on next startup.
                    writeIndexMeta(getIndexEntryCount());
                    break;
                }
                // New metadata items take priority
                if (!indexQueue.isEmpty()) {
                    contentQueue.add(fileInfo);
                    break;
                }
                try {
                    doIndexContent(fileInfo);
                } catch (OutOfMemoryError oom) {
                    logSevere(folder + ": OOM during content extraction of "
                            + fileInfo.toDetailString() + ": "
                            + oom.getMessage());
                    throw oom;
                } catch (Exception e) {
                    logWarning(folder + ": Content extraction failed for "
                            + fileInfo.getFilenameOnly() + ": "
                            + e.getMessage());
                }
                if (CONTENT_EXTRACT_THROTTLE_MS > 0) {
                    Thread.sleep(CONTENT_EXTRACT_THROTTLE_MS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (uncommittedCount.get() > 0) {
                commitAndRefresh();
            }
            workerRunning.set(false);

            if (!indexQueue.isEmpty() || !contentQueue.isEmpty()) {
                if (!closed.get()) {
                    ensureWorkerRunning();
                }
            }

            logFine(folder + ": Index worker stopped (metadata pending: "
                    + indexQueue.size() + ", content pending: "
                    + contentQueue.size() + ")");
        }
    }

    // ------------------------------------------------------------------------
    // Core indexing (called by worker)
    // ------------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Index versioning — bump when Lucene/Tika/OCR libs change in a way
    // that makes existing index data incompatible or stale.
    // -----------------------------------------------------------------------
    private static final int INDEX_FORMAT_VERSION = 11;

    private Document buildDocument(FileInfo fileInfo) {
        String docId = buildDocId(fileInfo);
        Document doc = new Document();

        String fileName = fileInfo.getFilenameOnly() != null
                ? fileInfo.getFilenameOnly() : "";
        String extension = fileInfo.getExtension() != null
                ? fileInfo.getExtension() : "";
        String relativeName = fileInfo.getRelativeName() != null
                ? fileInfo.getRelativeName() : "";

        doc.add(new StringField("docId", docId, Field.Store.YES));
        doc.add(new StringField("folderId", folder.getId(), Field.Store.YES));
        doc.add(new StoredField("folderName", folder.getName()));
        doc.add(new TextField("fileName", fileName, Field.Store.YES));
        doc.add(new StoredField("extension", extension));
        doc.add(new StringField("extensionExact", extension.toLowerCase(Locale.ROOT), Field.Store.NO));
        doc.add(new TextField("relativeName", relativeName, Field.Store.YES));
        doc.add(new StringField("relativeNameExact", relativeName.toLowerCase(Locale.ROOT), Field.Store.NO));
        long modifiedDate = fileInfo.getModifiedDate() != null ? fileInfo.getModifiedDate().getTime() : 0;
        doc.add(new LongPoint("modifiedDate", modifiedDate));
        doc.add(new StoredField("modifiedDate", modifiedDate));
        doc.add(new StoredField("size", fileInfo.getSize()));
        doc.add(new StoredField("version", fileInfo.getVersion()));
        if (StringUtils.isNotBlank(fileInfo.getOID())) {
            doc.add(new StoredField("oid", fileInfo.getOID()));
        }
        if (StringUtils.isNotBlank(fileInfo.getHashes())) {
            doc.add(new StoredField("hashes", fileInfo.getHashes()));
        }
        if (StringUtils.isNotBlank(fileInfo.getTags())) {
            doc.add(new StoredField("tags", fileInfo.getTags()));
        }

        AccountInfo modAccount = fileInfo.getModifiedByAccount();
        if (modAccount != null) {
            if (StringUtils.isNotBlank(modAccount.getOID())) {
                doc.add(new StringField("modifiedByAccountId", modAccount.getOID(), Field.Store.YES));
            }
            if (StringUtils.isNotBlank(modAccount.getDisplayName())) {
                doc.add(new TextField("modifiedByDisplayName", modAccount.getDisplayName(), Field.Store.YES));
            }
            if (StringUtils.isNotBlank(modAccount.getUsername())) {
                doc.add(new TextField("modifiedByUsername", modAccount.getUsername(), Field.Store.YES));
            }
        }
        MemberInfo modDevice = fileInfo.getModifiedBy();
        if (modDevice != null) {
            if (StringUtils.isNotBlank(modDevice.id)) {
                doc.add(new StringField("modifiedByDeviceId", modDevice.id, Field.Store.YES));
            }
            if (StringUtils.isNotBlank(modDevice.nick)) {
                doc.add(new TextField("modifiedByDeviceName", modDevice.nick, Field.Store.YES));
            }
        }

        doc.add(new StringField("deleted",
                fileInfo.isDeleted() ? "true" : "false", Field.Store.YES));

        return doc;
    }

    /**
     * Phase 1: indexes metadata. Small files get content extracted
     * inline (Tika only, no OCR) for immediate searchability.
     * Larger files are queued for phase 2 (full extraction with OCR).
     */
    private void doIndexFile(FileInfo fileInfo) {
        try {
            Document doc = buildDocument(fileInfo);

            if (!fileInfo.isDeleted() && !fileInfo.isDiretory() && isExtractContentEnabled()) {
                if (fileInfo.getSize() <= INLINE_EXTRACT_MAX_SIZE) {
                    String content = extractContentTikaOnly(fileInfo);
                    if (content != null && !content.isBlank()) {
                        doc.add(new TextField("content", content, Field.Store.NO));
                    } else {
                        contentQueue.add(fileInfo);
                    }
                } else {
                    contentQueue.add(fileInfo);
                }
            }

            writer.updateDocument(
                    new Term("docId", buildDocId(fileInfo)), doc);

            if (uncommittedCount.incrementAndGet() >= COMMIT_INTERVAL) {
                commitAndRefresh();
            }
        } catch (Exception e) {
            logWarning(folder + ": Index error for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
        }
    }

    /**
     * Phase 2: re-indexes with extracted content (Tika/OCR, slow).
     */
    private void doIndexContent(FileInfo fileInfo) {
        if (fileInfo.isDiretory()) return;
        try {
            String content = extractContent(fileInfo);
            if (content == null || content.isBlank()) return;

            Document doc = buildDocument(fileInfo);
            doc.add(new TextField("content", content, Field.Store.NO));
            writer.updateDocument(
                    new Term("docId", buildDocId(fileInfo)), doc);

            if (uncommittedCount.incrementAndGet() >= COMMIT_INTERVAL) {
                commitAndRefresh();
            }
        } catch (Exception e) {
            logWarning(folder + ": Content extraction error for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Content extraction (Tika + encoding repair + OCR)
    // ------------------------------------------------------------------------

    private String extractContentTikaOnly(FileInfo fileInfo) {
        Path filePath = fileInfo.getDiskFile(folder);
        if (filePath == null || !Files.exists(filePath)) {
            logFine(folder + ": File not found on disk for content extraction: "
                    + fileInfo.getRelativeName()
                    + " (resolved path: " + filePath + ")");
            return null;
        }

        try (InputStream stream = Files.newInputStream(filePath)) {
            BodyContentHandler handler = new BodyContentHandler(TIKA_WRITE_LIMIT);
            getSharedParser().parse(stream, handler, new Metadata(), new ParseContext());
            String text = handler.toString();
            if (text != null && !text.isBlank()) {
                if (!hasEncodingIssues(text)) return text;
                String repaired = repairEncoding(text);
                if (repaired != null && !hasEncodingIssues(repaired)) {
                    return repaired;
                }
                return stripEncodingArtifacts(text);
            }
        } catch (IOException | SAXException | TikaException e) {
            logFine(folder + ": Tika extraction failed for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
            return readPlainTextFallback(filePath);
        }
        return null;
    }

    private String readPlainTextFallback(Path filePath) {
        String name = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".txt") && !name.endsWith(".md")
                && !name.endsWith(".csv") && !name.endsWith(".log")) {
            return null;
        }
        try {
            long size = Files.size(filePath);
            if (size > INLINE_EXTRACT_MAX_SIZE) return null;
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            try {
                return Files.readString(filePath, StandardCharsets.ISO_8859_1);
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private String extractContent(FileInfo fileInfo) {
        Path filePath = fileInfo.getDiskFile(folder);
        if (filePath == null || !Files.exists(filePath)) return null;

        try {
            if (Files.size(filePath) > CONTENT_EXTRACT_MAX_SIZE) {
                logFine(folder + ": Skipping content extraction for "
                        + fileInfo.getFilenameOnly() + " (file too large)");
                return null;
            }
        } catch (IOException ignored) {}

        String tikaText = null;

        // 1) Tika text extraction (shared parser, no per-file init cost)
        try (InputStream stream = Files.newInputStream(filePath)) {
            BodyContentHandler handler =
                    new BodyContentHandler(TIKA_WRITE_LIMIT);
            getSharedParser().parse(stream, handler,
                    new Metadata(), new ParseContext());
            String text = handler.toString();
            if (text != null && !text.isBlank()) {
                tikaText = text;
            }
        } catch (IOException | SAXException | TikaException e) {
            logFiner(folder + ": Tika failed for "
                    + fileInfo.getFilenameOnly() + ": "
                    + e.getMessage());
        }

        // 2) Check quality and attempt encoding repair
        if (tikaText != null) {
            if (!hasEncodingIssues(tikaText)) {
                return tikaText;
            }

            String repaired = repairEncoding(tikaText);
            if (repaired != null && !hasEncodingIssues(repaired)) {
                logFine(folder + ": Repaired encoding for "
                        + fileInfo.getFilenameOnly());
                return repaired;
            }

            logFine(folder + ": Encoding issues in "
                    + fileInfo.getFilenameOnly()
                    + ", trying OCR fallback");
        }

        // 3) OCR fallback for broken encoding or missing text
        if (isOcrEnabled() && OCR_ELIGIBLE_PATTERN.matcher(
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

        // 4) Last resort: strip encoding artifacts so partial matching works
        if (tikaText != null) {
            return stripEncodingArtifacts(tikaText);
        }

        return null;
    }

    /**
     * Detects common encoding issues in extracted text:
     * <ul>
     *   <li>U+FFFD replacement characters (source bytes were invalid
     *       in the decoder's charset, e.g. Latin-1 bytes fed to a
     *       UTF-8 decoder)</li>
     *   <li>Mojibake sequences (UTF-8 multi-byte sequences misread as
     *       Latin-1/Windows-1252, e.g. ä appearing as
     *       "Ã¤")</li>
     * </ul>
     */
    private static boolean hasEncodingIssues(String text) {
        if (text.indexOf(REPLACEMENT_CHAR) >= 0) {
            return true;
        }
        return MOJIBAKE_PATTERN.matcher(text).find();
    }

    /**
     * Attempts to repair common encoding mismatches:
     * <ol>
     *   <li><b>UTF-8 as Latin-1:</b> Text was UTF-8 but decoded as
     *       ISO-8859-1, producing mojibake like "Ã¤" for
     *       ä. Fix: re-encode as Latin-1 bytes, decode as
     *       UTF-8.</li>
     *   <li><b>UTF-8 as Windows-1252:</b> Same principle but with
     *       Windows-1252 (superset of Latin-1 in 0x80-0x9F range,
     *       covers smart quotes, euro sign, etc.).</li>
     * </ol>
     * Returns null if no repair produces clean text.
     */
    private static String repairEncoding(String text) {
        if (text.indexOf(REPLACEMENT_CHAR) >= 0) {
            return null;
        }

        try {
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            String repaired = new String(bytes, StandardCharsets.UTF_8);
            if (repaired.indexOf(REPLACEMENT_CHAR) < 0
                    && !MOJIBAKE_PATTERN.matcher(repaired).find()) {
                return repaired;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Strips U+FFFD replacement characters from text so that the
     * remaining content is still searchable. E.g. "Meldebeh�rde"
     * becomes "Meldebehrde", which still matches prefix "meldebeh".
     */
    private static String stripEncodingArtifacts(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != REPLACEMENT_CHAR) {
                sb.append(c);
            }
        }
        return sb.toString();
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
                        new PrefixQuery(new Term("relativeNameExact", prefix.toLowerCase(Locale.ROOT))),
                        BooleanClause.Occur.MUST);
            }

            if (StringUtils.isNotBlank(extension)) {
                bqBuilder.add(
                        new TermQuery(new Term("extensionExact", extension.toLowerCase(Locale.ROOT))),
                        BooleanClause.Occur.MUST);
            }

            if (StringUtils.isNotBlank(modifiedBy)) {
                String term = modifiedBy.toLowerCase(Locale.ROOT).trim();
                String wildcard = "*" + term + "*";
                BooleanQuery.Builder modQuery = new BooleanQuery.Builder();
                modQuery.add(new WildcardQuery(new Term("modifiedByDisplayName", wildcard)), BooleanClause.Occur.SHOULD);
                modQuery.add(new WildcardQuery(new Term("modifiedByUsername", wildcard)), BooleanClause.Occur.SHOULD);
                modQuery.add(new WildcardQuery(new Term("modifiedByDeviceName", wildcard)), BooleanClause.Occur.SHOULD);
                bqBuilder.add(modQuery.build(), BooleanClause.Occur.MUST);
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
                .replaceAll("[^\\p{L}\\p{N}\\s._\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isEmpty()) return null;

        String[] tokens = sanitized.split("\\s+");
        BooleanQuery.Builder allTokens = new BooleanQuery.Builder();

        for (String token : tokens) {
            BooleanQuery.Builder fieldDisjunction =
                    new BooleanQuery.Builder();

            addTokenQueries(fieldDisjunction, token, 3.0f, 2.0f, 1.0f);

            // Accent-folded variant (ö -> o, ä -> a, etc.)
            String folded = foldAccents(token);
            if (!folded.equals(token) && !folded.isEmpty()) {
                addTokenQueries(fieldDisjunction, folded, 1.5f, 1.0f, 0.5f);
            }

            // Accent-stripped variant (ö removed entirely) — handles
            // documents where extraction lost characters completely
            String stripped = stripAccents(token);
            if (!stripped.equals(token) && !stripped.equals(folded)
                    && !stripped.isEmpty()) {
                addTokenQueries(fieldDisjunction, stripped, 1.0f, 0.5f, 0.25f);
            }

            fieldDisjunction.setMinimumNumberShouldMatch(1);
            allTokens.add(fieldDisjunction.build(),
                    BooleanClause.Occur.MUST);
        }

        return allTokens.build();
    }

    private void addTokenQueries(BooleanQuery.Builder builder,
                                 String token,
                                 float exactBoost, float prefixBoost,
                                 float wildcardBoost) {
        for (String field : SEARCH_FIELDS) {
            builder.add(
                    new BoostQuery(new TermQuery(
                            new Term(field, token)), exactBoost),
                    BooleanClause.Occur.SHOULD);

            builder.add(
                    new BoostQuery(new PrefixQuery(
                            new Term(field, token)), prefixBoost),
                    BooleanClause.Occur.SHOULD);

            if (token.length() <= 12) {
                builder.add(
                        new BoostQuery(new WildcardQuery(
                                new Term(field, "*" + token + "*")),
                                wildcardBoost),
                        BooleanClause.Occur.SHOULD);
            }
        }
    }

    /**
     * Folds accented characters to their ASCII base letter.
     * E.g. ö -> o, ä -> a, é -> e.
     */
    private static String foldAccents(String s) {
        String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    private static String stripAccents(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // Commit helper
    // ------------------------------------------------------------------------

    private void commitAndRefresh() {
        try {
            writer.commit();
            searcherManager.maybeRefreshBlocking();
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

    /**
     * Returns true while a full rebuild is in progress. Callers should
     * fall back to DAO-based search while this returns true, since the
     * index is incomplete.
     */
    public boolean isRebuilding() {
        return rebuilding.get();
    }

    // ------------------------------------------------------------------------
    // Shutdown
    // ------------------------------------------------------------------------

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) return;
        logFine(folder + ": Shutting down...");

        FileInfo f;
        while ((f = indexQueue.poll()) != null) {
            try { doIndexFile(f); }
            catch (Exception ignored) {}
        }
        contentQueue.clear();

        commitAndRefresh();
        try { searcherManager.close(); }
        catch (Exception ignored) {}
        try { writer.close(); }
        catch (Exception ignored) {}

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
