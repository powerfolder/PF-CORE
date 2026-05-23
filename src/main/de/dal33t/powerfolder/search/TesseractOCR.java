/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */

package de.dal33t.powerfolder.search;

import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;
import net.sourceforge.tess4j.Tesseract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    public static void initInstance(String localeCodes, int poolSize, long maxFileSizeMB) {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new TesseractOCR(localeCodes, poolSize, maxFileSizeMB);
                }
            }
        }
    }

    public static TesseractOCR getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    private static final String CLASSPATH_TESSDATA_PATH = "/tesseract_ocr_trainingdata";
    private static final String TEMP_TESSDATA_DIRNAME = "powerfolder_tesseract_ocr_trainingdata";

    private final long maxOcrFileSizeBytes;
    private final String languageConfig;
    private boolean ocrEnabled = true;

    /** Pool of Tesseract instances for concurrent access. */
    private final BlockingQueue<Tesseract> pool;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------

    private static final String[] LINUX_NATIVE_LIB_PATHS = {
            "/usr/lib/x86_64-linux-gnu",
            "/usr/lib64",
            "/usr/lib",
            "/usr/local/lib"
    };

    private TesseractOCR(String localeCodes, int poolSize, long maxFileSizeMB) {
        super();

        configureNativeLibraryPath();

        this.maxOcrFileSizeBytes = maxFileSizeMB * 1024 * 1024;
        this.languageConfig = resolveLanguageConfig(localeCodes);

        this.pool = new LinkedBlockingQueue<>(poolSize);

        if (!isNativeLibraryAvailable()) {
            logWarning("Tesseract/Leptonica native libraries not available — disabling OCR. "
                    + "Install with: apt-get install tesseract-ocr libleptonica-dev");
            this.ocrEnabled = false;
            return;
        }

        try {
            Path tessdataPath = prepareOrReuseTessdata();

            if (tessdataPath == null || !Files.exists(tessdataPath) || isDirEmpty(tessdataPath)) {
                logWarning("No OCR training data found — disabling OCR support.");
                this.ocrEnabled = false;
                return;
            }

            // Create pooled Tesseract instances
            for (int i = 0; i < poolSize; i++) {
                Tesseract tess = new Tesseract();
                tess.setDatapath(tessdataPath.toString());
                tess.setLanguage(languageConfig);
                tess.setOcrEngineMode(1);
                tess.setVariable("user_defined_dpi", "150");
                tess.setVariable("debug_file", OSUtil.isWindowsSystem() ? "NUL" : "/dev/null");
                pool.add(tess);
            }

            logInfo("Tesseract OCR initialized with " + poolSize + " pooled instances, languages: "
                    + languageConfig + ", maxFileSize: " + (maxOcrFileSizeBytes / (1024 * 1024)) + " MB");
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

        // Remove old traineddata files not in the current language config
        Set<String> configured = new HashSet<>(Arrays.asList(languageConfig.split("\\+")));
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(tessDir, "*.traineddata")) {
            for (Path existing : ds) {
                String name = existing.getFileName().toString().replace(".traineddata", "");
                if (!configured.contains(name)) {
                    Files.deleteIfExists(existing);
                    logFine("Removed unused tessdata: " + name);
                }
            }
        }

        int extracted = 0;
        for (String tessCode : languageConfig.split("\\+")) {
            String resourceName = CLASSPATH_TESSDATA_PATH + "/" + tessCode + ".traineddata";
            Path targetFile = tessDir.resolve(tessCode + ".traineddata");

            try (InputStream in = getClass().getResourceAsStream(resourceName)) {
                if (in != null) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    extracted++;
                    logFiner("Prepared tessdata " + resourceName);
                } else if (!Files.exists(targetFile)) {
                    logWarning("No OCR training data found for language: " + tessCode);
                }
            } catch (IOException e) {
                logWarning("Failed to prepare tessdata for " + tessCode + ": " + e.getMessage());
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
     * the instance. Runs inline on the calling thread (the indexing thread pool).
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

        try {
            long fileSize = Files.size(file);
            if (fileSize > maxOcrFileSizeBytes) {
                logInfo("Skipping OCR for " + file + " — file size " + (fileSize / (1024 * 1024))
                        + " MB exceeds limit of " + (maxOcrFileSizeBytes / (1024 * 1024)) + " MB");
                return null;
            }
        } catch (IOException e) {
            logWarning("Cannot determine file size for " + file.getFileName() + ": " + e.getMessage());
            return null;
        }

        Tesseract tess = null;
        try {
            tess = pool.poll(5, TimeUnit.SECONDS);
            if (tess == null) {
                logWarning("OCR pool exhausted — could not acquire Tesseract instance for " + file.getFileName());
                return null;
            }

            long start = System.currentTimeMillis();
            String result = tess.doOCR(file.toFile());
            long elapsed = System.currentTimeMillis() - start;
            pool.offer(tess);
            tess = null;
            if (result != null && !result.isBlank()) {
                if (isFine()) {
                    logFine("OCR completed for " + file.getFileName() + " (" + result.length()
                            + " chars, " + elapsed + " ms)");
                }
            }
            return result;
        } catch (NoClassDefFoundError | UnsatisfiedLinkError | ExceptionInInitializerError e) {
            logSevere("Native OCR library not available (" + e.getMessage() + ") — disabling OCR permanently");
            ocrEnabled = false;
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWarning("OCR interrupted for " + file.getFileName());
            return null;
        } catch (Exception e) {
            logFine("OCR failed for " + file.getFileName() + ": " + e.getMessage());
            return null;
        } finally {
            if (tess != null) {
                pool.offer(tess);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Native library path and detection
    // ------------------------------------------------------------------------

    private void configureNativeLibraryPath() {
        if (OSUtil.isWindowsSystem()) {
            return;
        }
        String existing = System.getProperty("jna.library.path", "");
        StringBuilder sb = new StringBuilder(existing);
        for (String candidate : LINUX_NATIVE_LIB_PATHS) {
            Path dir = Paths.get(candidate);
            if (Files.isDirectory(dir)) {
                if (sb.length() > 0) {
                    sb.append(java.io.File.pathSeparator);
                }
                sb.append(candidate);
            }
        }
        if (sb.length() > 0) {
            System.setProperty("jna.library.path", sb.toString());
            logFine("Set jna.library.path=" + sb);
        }
    }

    private boolean isNativeLibraryAvailable() {
        try {
            Class.forName("net.sourceforge.lept4j.Leptonica1");
            return true;
        } catch (NoClassDefFoundError | UnsatisfiedLinkError | ExceptionInInitializerError e) {
            logWarning("Leptonica native library check failed: " + e.getMessage());
            return false;
        } catch (ClassNotFoundException e) {
            logWarning("lept4j not on classpath: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    public String getLanguageConfig() {
        return languageConfig;
    }

    // ------------------------------------------------------------------------
    // Shutdown
    // ------------------------------------------------------------------------

    public void shutdown() {
        pool.clear();
        synchronized (LOCK) {
            INSTANCE = null;
        }
        logFine("TesseractOCR shut down.");
    }

    // ------------------------------------------------------------------------
    // Language mapping
    // ------------------------------------------------------------------------

    private static final String PRESET_FAST = "en,de";
    private static final String PRESET_EUROPE = "en,de,fr,es,it,pt,tr,ar,zh,ru";
    private static final String PRESET_ALL =
            "en,de,fr,es,it,pt,nl,pl,ru,tr,sv,no,da,fi,hu,cs,sk,sl,ro,bg,"
                    + "el,uk,sr,hr,bs,lt,lv,et,ar,fa,he,zh,ja,ko,vi,id,ms,hi,th,ga,mt";

    private static String resolveLanguageConfig(String input) {
        String resolved;
        switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "fast": resolved = PRESET_FAST; break;
            case "europe": resolved = PRESET_EUROPE; break;
            case "all": resolved = PRESET_ALL; break;
            default: resolved = input;
        }
        return Arrays.stream(resolved.split("[,;+\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> mapToTesseractLangCode(s.toLowerCase(Locale.ROOT)))
                .distinct()
                .collect(java.util.stream.Collectors.joining("+"));
    }

    private static String mapToTesseractLangCode(String lang) {
        switch (lang) {
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
            case "he": case "iw": return "heb";
            case "zh": return "chi_sim";
            case "ja": return "jpn";
            case "ko": return "kor";
            case "vi": return "vie";
            case "id": case "in": return "ind";
            case "ms": return "msa";
            case "hi": return "hin";
            case "th": return "tha";
            case "ga": return "gle";
            case "mt": return "mlt";
            default: return lang;
        }
    }
}
