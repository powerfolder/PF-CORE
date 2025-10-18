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

import de.dal33t.powerfolder.util.logging.Loggable;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * Handles embedded Tesseract OCR setup, language configuration,
 * and traineddata extraction from the JAR into the system temp directory.
 */
public class TesseractOCR extends Loggable {

    private static final String DEFAULT_TESSDATA_PATH = "/tessdata";
    private static final String DEFAULT_TESS_LANGUAGES = "eng+deu";
    private static final String TEMP_TESSDATA_PREFIX = "powerfolder_tessdata_";

    private final Tesseract tesseract;

    public TesseractOCR() throws IOException {
        super();
        this.tesseract = new Tesseract();
        Path tessdataPath = extractBundledTessdataToTemp();
        this.tesseract.setDatapath(tessdataPath.toString());
        this.tesseract.setLanguage(DEFAULT_TESS_LANGUAGES);

        logFine("Tesseract OCR initialized. Languages=" + DEFAULT_TESS_LANGUAGES +
                ", Path=" + tessdataPath);
    }

    /**
     * Extracts tessdata files embedded in the JAR to the system temp directory.
     */
    private Path extractBundledTessdataToTemp() throws IOException {
        Path tempDir = Files.createTempDirectory(TEMP_TESSDATA_PREFIX);
        Path tessDir = tempDir.resolve("tessdata");
        Files.createDirectories(tessDir);

        String[] languages = DEFAULT_TESS_LANGUAGES.split("\\+");
        for (String lang : languages) {
            String resourceName = DEFAULT_TESSDATA_PATH + "/" + lang + ".traineddata";
            Path targetFile = tessDir.resolve(lang + ".traineddata");

            try (InputStream in = getClass().getResourceAsStream(resourceName)) {
                if (in != null) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    logFine("Extracted " + resourceName + " → " + targetFile);
                } else {
                    logWarning("Missing tessdata resource: " + resourceName);
                }
            }
        }

        tempDir.toFile().deleteOnExit();
        tessDir.toFile().deleteOnExit();

        return tessDir;
    }

    /**
     * Performs OCR on a given file path.
     *
     * @param file Path to the image or PDF file.
     * @return Recognized text content, or null if OCR failed.
     */
    public String performOCR(Path file) {
        try {
            return tesseract.doOCR(file.toFile());
        } catch (TesseractException e) {
            logWarning("OCR failed for " + file + ": " + e.getMessage());
            return null;
        }
    }

    public Tesseract getTesseract() {
        return tesseract;
    }
}
