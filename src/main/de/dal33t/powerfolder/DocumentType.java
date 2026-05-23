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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum DocumentType {

    DOCUMENT("doc", "docx", "odt", "epub", "xps","rtf"),
    SPREADSHEET("xls", "xlsx", "ods", "csv"),
    PRESENTATION("ppt", "pptx", "odp"),
    IMAGE("png", "jpg", "jpeg", "gif", "bmp","tif","tiff"),
    AUDIO("wav", "mp3", "ogg", "oga", "webma", "fla", "flac", "m3u8a", "rtmpa", "djvu"),
    VIDEO("mp4", "flv", "rtmp", "rtmpv", "m4v", "ogv", "webmv", "m3uv", "m3u8v","mov","webm"),
    PDF("pdf"),
    TEXT("txt", "md");

    private Collection<String> extensions;

    DocumentType(String... extensions) {
        this.extensions = Collections.unmodifiableCollection(
            Arrays.asList(extensions));
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
