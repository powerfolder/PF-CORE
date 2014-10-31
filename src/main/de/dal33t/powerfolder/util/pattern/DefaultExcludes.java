/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.util.pattern;

import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;

public enum DefaultExcludes {
    DESKTOP_INI("*" + PathUtils.DESKTOP_INI_FILENAME),
    THUMBS_DB("*thumbs.db"),
    OFFICE_TEMP("*~*.tmp"),
    OFFICEX_TEMP("*~$*"),
    DS_STORE("*.DS_Store"),
    TEMPORARY_ITEMS("*.Temporary Items"),
    TRASHES("*.Trashes"),
    FSEVENTD("*.fseventd"),
    APDISK("*.apdisk"),
    PARTS("*.part"),

    /**
     * Apples iPod thumb files. http://dotwhat.net/ithmb/9058/
     */
    ITHUMB("*.ithmb");

    String pattern;

    private DefaultExcludes(String pattern) {
        Reject.ifBlank(pattern, "Pattern");
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern.toLowerCase();
    }
}
