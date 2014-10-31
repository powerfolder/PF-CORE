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
 *
 * $Id: CompilingPatternMatch.java 8022 2009-05-21 07:46:07Z harry $
 */
package de.dal33t.powerfolder.util.pattern;

/**
 * Matching on any texts that ends with the given pattern
 */
public class OfficeTempFilesMatchPattern extends EndMatchPattern {

    private boolean matchEnd;
    private String startStr;

    /**
     * Constructor.
     *
     * @param startChar
     *            e.g. "~"
     * @param endPattern
     *            e.g. ".tmp"
     */
    public OfficeTempFilesMatchPattern(String startStr, String endPattern) {
        super(endPattern);
        this.startStr = startStr;
        this.matchEnd = !endPattern.equals("*");
    }

    public boolean isMatch(String matchString) {
        if (matchEnd && !super.isMatch(matchString)) {
            return false;
        }
        return matchString.indexOf(startStr) >= 0;
    }

    public String getPatternText() {
        // startstring: ~
        // superpattern: *.tmp
        // Resultpattern: *~*.tmp
        return "*" + startStr + super.getPatternText();
        // startstring: ~$
        // superpattern: *
        // Resultpattern: *~$*
    }

}
