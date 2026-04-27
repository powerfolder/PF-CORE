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

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.Loggable;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Singleton wrapper around the Tesseract OCR engine.
 * <p>
 * Manages a pool of Tesseract instances so multiple folders can perform OCR
 * concurrently without serializing behind a single lock. Each instance in
 * the pool is individually not thread-safe (Tesseract limitation), but the
 * pool itself is thread-safe via a {@link BlockingQueue}.
 * <p>
 * Automatically initializes based on PowerFolder's supported locales and
 * extracts bundled .traineddata files from /tesseract_ocr_trainingdata
 * in the JAR into a shared system temp directory. If no training data is
 * found, OCR is automatically disabled.
 * <p>
 * Configurable safety limits: maximum file size for OCR input and a
 * per-file timeout prevent unbounded resource consumption on large or
 * problematic files.
 */
public class TesseractOCR extends Loggable {

    // ------------------------------------------------------------------------
    // Static singleton handling
    // ------------------------------------------------------------------------

    private static volatile TesseractOCR INSTANCE;
    private static final Object LOCK = new Object();

    public static TesseractOCR getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new TesseractOCR();
                }
            }
        }
        return INSTANCE;
    }

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    private static final String CLASSPATH_TESSDATA_PATH = "/tesseract_ocr_trainingdata";
    private static final String TEMP_TESSDATA_DIRNAME = "powerfolder_tesseract_ocr_trainingdata";

    /**
     * Number of pooled Tesseract instances. Each instance consumes memory
     * for loaded language models, so this is kept modest. Increase if OCR
     * throughput across many folders is a bottleneck.
     */
    private static final int POOL_SIZE =
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4));

    /** Maximum file size (in bytes) that will be submitted for OCR.
     *  Files larger than this are skipped. Default: 100 MB. */
    private static final long MAX_OCR_FILE_SIZE_BYTES =
            Long.getLong("powerfolder.ocr.maxFileSizeBytes",
                    100L * 1024 * 1024);

    /** Per-file OCR timeout in seconds. Default: 15 seconds. */
    private static final long OCR_TIMEOUT_SECONDS =
            Long.getLong("powerfolder.ocr.timeoutSeconds", 15);

    private final List<Locale> supportedLocales;
    private final String languageConfig;
    private boolean ocrEnabled = true;

    /** Pool of Tesseract instances for concurrent access. */
    private final BlockingQueue<Tesseract> pool;

    /**
     * Executor for running OCR with a timeout. Uses daemon threads so it
     * won't prevent JVM shutdown.
     */
    private final ExecutorService ocrExecutor;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------

    private TesseractOCR() {
        super();

        this.supportedLocales = Translation.getSupportedLocales();
        this.languageConfig = supportedLocales.stream()
                .map(Locale::getLanguage)
                .map(TesseractOCR::mapToTesseractLangCode)
                .distinct()
                .collect(Collectors.joining("+"));

        this.pool = new LinkedBlockingQueue<>(POOL_SIZE);

        // Daemon-thread executor for timeout-wrapped OCR calls
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r, "pf-ocr-worker");
            t.setDaemon(true);
            return t;
        };
        this.ocrExecutor = Executors.newFixedThreadPool(POOL_SIZE, daemonFactory);

        try {
            Path tessdataPath = prepareOrReuseTessdata();

            if (tessdataPath == null || !Files.exists(tessdataPath) || isDirEmpty(tessdataPath)) {
                logWarning("No OCR training data found — disabling OCR support.");
                this.ocrEnabled = false;
                return;
            }

            // Create pooled Tesseract instances
            for (int i = 0; i < POOL_SIZE; i++) {
                Tesseract tess = new Tesseract();
                tess.setDatapath(tessdataPath.toString());
                tess.setLanguage(languageConfig);
                pool.add(tess);
            }

            logFine("Tesseract OCR initialized with " + POOL_SIZE
                    + " pooled instances, languages: " + languageConfig
                    + ", maxFileSize: " + (MAX_OCR_FILE_SIZE_BYTES / (1024 * 1024)) + " MB"
                    + ", timeout: " + OCR_TIMEOUT_SECONDS + "s");
        } catch (IOException e) {
            logSevere("OCR initialization failed: " + e.getMessage());
            this.ocrEnabled = false;
        }
    }

    // ------------------------------------------------------------------------
    // Tessdata extraction and management
    // ------------------------------------------------------------------------

    private Path prepareOrReuseTessdata() throws IOException {
        Path sysTemp = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tessDir = sysTemp.resolve(TEMP_TESSDATA_DIRNAME);

        Files.createDirectories(tessDir);

        int extracted = 0;
        for (Locale locale : supportedLocales) {
            String lang = locale.getLanguage();
            String tessCode = mapToTesseractLangCode(lang);
            String resourceName = CLASSPATH_TESSDATA_PATH + "/" + tessCode + ".traineddata";
            Path targetFile = tessDir.resolve(tessCode + ".traineddata");

            try (InputStream in = getClass().getResourceAsStream(resourceName)) {
                if (in != null) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    extracted++;
                    logFiner("Prepared tessdata " + resourceName);
                } else if (!Files.exists(targetFile)) {
                    String warningMsg = "No OCR training data found for supported language: "
                            + locale.getDisplayName() + " (" + tessCode + ")";
                    logWarning(warningMsg);
                }
            } catch (IOException e) {
                logWarning("Failed to prepare tessdata for " + lang + ": " + e.getMessage());
            }
        }

        if (extracted == 0 && isDirEmpty(tessDir)) {
            logWarning("No tessdata resources found or extracted in " + tessDir);
            return null;
        }

        logFine("Tessdata ready in: " + tessDir.toAbsolutePath());
        return tessDir;
    }

    /**
     * Checks if a directory is empty, properly closing the directory stream
     * to avoid file-handle leaks.
     */
    private static boolean isDirEmpty(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // OCR execution
    // ------------------------------------------------------------------------

    /**
     * Performs OCR on the given file, or returns null if OCR is disabled,
     * the file exceeds the size limit, or the operation times out.
     * <p>
     * Borrows a Tesseract instance from the pool, performs OCR, and returns
     * the instance. Multiple callers can OCR concurrently up to
     * {@link #POOL_SIZE}.
     *
     * @param file path to the image or PDF
     * @return recognized text content, or null if OCR is disabled, skipped,
     *         or fails.
     */
    public String performOCR(Path file) {
        if (!ocrEnabled) {
            logFiner("OCR disabled — skipping " + file.getFileName());
            return null;
        }

        // Check file size limit
        try {
            long fileSize = Files.size(file);
            if (fileSize > MAX_OCR_FILE_SIZE_BYTES) {
                logInfo("Skipping OCR for " + file
                        + " — file size " + (fileSize / (1024 * 1024))
                        + " MB exceeds limit of "
                        + (MAX_OCR_FILE_SIZE_BYTES / (1024 * 1024))
                        + " MB");
                return null;
            }
        } catch (IOException e) {
            logWarning("Cannot determine file size for " + file.getFileName() + ": " + e.getMessage());
            return null;
        }

        // Borrow a Tesseract instance from the pool
        Tesseract tess = null;
        try {
            tess = pool.poll(30, TimeUnit.SECONDS);
            if (tess == null) {
                logWarning("OCR pool exhausted — could not acquire "
                        + "Tesseract instance within 30s for "
                        + file.getFileName());
                return null;
            }

            // Run OCR with a timeout
            final Tesseract borrowed = tess;
            Future<String> future = ocrExecutor.submit(
                    () -> borrowed.doOCR(file.toFile()));

            try {
                return future.get(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                logWarning("OCR timed out after " + OCR_TIMEOUT_SECONDS
                        + "s for " + file);
                return null;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TesseractException) {
                    logWarning("OCR failed for " + file + ": "
                            + cause.getMessage());
                } else {
                    logWarning("OCR error for " + file + ": "
                            + cause.getMessage());
                }
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWarning("OCR interrupted for " + file.getFileName());
            return null;
        } finally {
            // Always return the instance to the pool
            if (tess != null) {
                pool.offer(tess);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    // ------------------------------------------------------------------------
    // Shutdown
    // ------------------------------------------------------------------------

    /**
     * Shuts down the OCR executor. Call on application exit.
     */
    public void shutdown() {
        ocrExecutor.shutdownNow();
        pool.clear();
        logFine("TesseractOCR shut down.");
    }

    // ------------------------------------------------------------------------
    // Language mapping
    // ------------------------------------------------------------------------

    /**
     * Maps standard ISO 639-1 language codes to Tesseract ISO 639-2 codes
     * (as used in tessdata_best). Supports all official Tesseract languages.
     */
    private static String mapToTesseractLangCode(String lang) {
        if (lang == null) return "eng";

        switch (lang.toLowerCase(Locale.ROOT)) {
            case "en": return "eng";
            case "de": return "deu";
            case "fr": return "fra";
            case "it": return "ita";
            case "es": return "spa";
            case "pt": return "por";
            case "nl": return "nld";
            case "pl": return "pol";
            case "ru": return "rus";
            case "tr": return "tur";
            case "sv": return "swe";
            case "no": return "nor";
            case "da": return "dan";
            case "fi": return "fin";
            case "hu": return "hun";
            case "cs": return "ces";
            case "sk": return "slk";
            case "sl": return "slv";
            case "ro": return "ron";
            case "bg": return "bul";
            case "el": return "ell";
            case "uk": return "ukr";
            case "sr": return "srp";
            case "hr": return "hrv";
            case "bs": return "bos";
            case "lt": return "lit";
            case "lv": return "lav";
            case "et": return "est";
            case "ar": return "ara";
            case "fa": return "fas";
            case "he": return "heb";
            case "zh": return "chi_sim";
            case "zh_cn": return "chi_sim";
            case "zh_tw": return "chi_tra";
            case "ja": return "jpn";
            case "ko": return "kor";
            case "vi": return "vie";
            case "id": return "ind";
            case "ms": return "msa";
            case "hi": return "hin";
            case "th": return "tha";
            case "ga": return "gle";
            case "mt": return "mlt";
            default: return lang.toLowerCase(Locale.ROOT);
        }
    }
}
