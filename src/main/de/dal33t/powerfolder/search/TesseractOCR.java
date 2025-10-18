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
import java.util.stream.Collectors;

/**
 * Singleton wrapper around the Tesseract OCR engine.
 * <p>
 * Automatically initializes based on PowerFolder's supported locales and
 * extracts bundled .traineddata files from /tesseract_ocr_trainingdata
 * in the JAR into a shared system temp directory.
 * <p>
 * If no training data is found, OCR is automatically disabled.
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

    private final Tesseract tesseract;
    private final List<Locale> supportedLocales;
    private final String languageConfig;
    private boolean ocrEnabled = true;

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

        this.tesseract = new Tesseract();

        try {
            Path tessdataPath = prepareOrReuseTessdata();

            if (tessdataPath == null || !Files.exists(tessdataPath) ||
                    Files.list(tessdataPath).findAny().isEmpty()) {
                logWarning("No OCR training data found — disabling OCR support.");
                this.ocrEnabled = false;
                return;
            }

            this.tesseract.setDatapath(tessdataPath.toString());
            this.tesseract.setLanguage(languageConfig);
            logFine("Tesseract OCR initialized with languages: " + languageConfig);
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
                    System.out.println(warningMsg);
                    logWarning(warningMsg);
                }
            } catch (IOException e) {
                logWarning("Failed to prepare tessdata for " + lang + ": " + e.getMessage());
            }
        }

        if (extracted == 0 && Files.list(tessDir).findAny().isEmpty()) {
            logWarning("No tessdata resources found or extracted in " + tessDir);
            return null;
        }

        logFine("Tessdata ready in: " + tessDir.toAbsolutePath());
        return tessDir;
    }


    // ------------------------------------------------------------------------
    // OCR execution
    // ------------------------------------------------------------------------

    /**
     * Performs OCR on the given file, or returns null if OCR is disabled.
     *
     * @param file path to the image or PDF
     * @return recognized text content, or null if OCR is disabled or fails.
     */
    public synchronized String performOCR(Path file) {
        if (!ocrEnabled) {
            logFiner("OCR disabled — skipping " + file.getFileName());
            return null;
        }

        try {
            return tesseract.doOCR(file.toFile());
        } catch (TesseractException e) {
            logWarning("OCR failed for " + file + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    public Tesseract getTesseract() {
        return tesseract;
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
            default: return lang.toLowerCase(Locale.ROOT);
        }
    }
}
