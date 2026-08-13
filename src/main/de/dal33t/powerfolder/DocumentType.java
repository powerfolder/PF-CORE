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
package de.dal33t.powerfolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum DocumentType {

    DOCUMENT("doc", "docx", "odt", "epub", "xps","rtf", "djvu"),
    SPREADSHEET("xls", "xlsx", "ods", "csv"),
    PRESENTATION("ppt", "pptx", "odp"),
    IMAGE("png", "jpg", "jpeg", "gif", "bmp","tif","tiff"),
    AUDIO("wav", "mp3", "ogg", "oga", "webma", "fla", "flac", "m3u8a", "rtmpa"),
    VIDEO("mp4", "flv", "rtmp", "rtmpv", "m4v", "ogv", "webmv", "m3uv", "m3u8v","mov","webm"),
    PDF("pdf"),
    TEXT("txt", "md", "log", "json", "xml", "yml", "yaml"),
    ARCHIVE("zip", "rar", "7z", "tar", "gz", "bz2");

    /** PFS-5653: the two categories a file can have without being any document type. */
    public static final String OTHER = "other";
    public static final String FOLDER = "folder";

    private static final Map<String, DocumentType> BY_EXTENSION = new HashMap<>();

    static {
        for (DocumentType type : values()) {
            for (String extension : type.extensions) {
                BY_EXTENSION.put(extension.toLowerCase(Locale.ROOT), type);
            }
        }
    }

    private Collection<String> extensions;

    DocumentType(String... extensions) {
        this.extensions = Collections.unmodifiableCollection(
            Arrays.asList(extensions));
    }

    /** @return the name a search filters by, e.g. "spreadsheet" for {@link #SPREADSHEET}. */
    public String getCategory() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * PFS-5653: the type of a file, by extension.
     *
     * @return the matching category, or {@link #OTHER} when no type claims the extension.
     */
    public static String categoryOf(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        DocumentType type = BY_EXTENSION.get(extension.toLowerCase(Locale.ROOT));
        return type == null ? OTHER : type.getCategory();
    }

    /**
     * @return every category a search can filter by, in the order a filter UI should offer them: the
     *         document types as declared, and folders last. {@link #OTHER} is not offered - "none of the
     *         above" is not a filter anyone asks for.
     */
    public static List<String> categories() {
        List<String> categories = new ArrayList<>();
        for (DocumentType type : values()) {
            categories.add(type.getCategory());
        }
        categories.add(FOLDER);
        return Collections.unmodifiableList(categories);
    }

    public Collection<String> getExtensions() {
        return extensions;
    }

    public String toRegExp() {
        StringBuilder sb = new StringBuilder();
        for (String ext:
             extensions) {
            sb.append(ext);
            sb.append("|");
        }
        String s = sb.toString();
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
