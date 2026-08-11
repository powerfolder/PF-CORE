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
 */
package de.dal33t.powerfolder.search;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FileCategoryMapper {

    public static final String IMAGE = "image";
    public static final String DOCUMENT = "document";
    public static final String SPREADSHEET = "spreadsheet";
    public static final String PRESENTATION = "presentation";
    public static final String VIDEO = "video";
    public static final String AUDIO = "audio";
    public static final String ARCHIVE = "archive";
    public static final String TEXT = "text";
    public static final String OTHER = "other";
    public static final String FOLDER = "folder";

    private static final Map<String, String> BY_EXTENSION = new HashMap<>();

    static {
        put(IMAGE, "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "webp", "heic", "svg");
        put(VIDEO, "mp4", "mov", "avi", "mkv", "wmv", "webm", "m4v");
        put(AUDIO, "mp3", "wav", "flac", "aac", "ogg", "m4a");
        put(DOCUMENT, "pdf", "doc", "docx", "odt", "rtf");
        put(SPREADSHEET, "xls", "xlsx", "ods", "csv");
        put(PRESENTATION, "ppt", "pptx", "odp");
        put(ARCHIVE, "zip", "rar", "7z", "tar", "gz", "bz2");
        put(TEXT, "txt", "md", "log", "json", "xml", "yml", "yaml");
    }

    private FileCategoryMapper() {
    }

    private static void put(String category, String... extensions) {
        for (String extension : extensions) {
            BY_EXTENSION.put(extension, category);
        }
    }

    public static String categoryOf(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        return BY_EXTENSION.getOrDefault(extension.toLowerCase(Locale.ROOT), OTHER);
    }
}
