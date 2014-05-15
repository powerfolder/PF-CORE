/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * Compiling pattern matcher that uses compiled parts to match '*' characters to
 * any text. So 'a*c' would match 'ac', 'abc', 'asdfkhc', etc.
 */
public class CompiledPattern extends AbstractPattern {

    /** Precompiled parts to match on. */
    private String[] partsLower;
    private String[] partsUpper;

    /** True if pattern begins with '*'. */
    private boolean firstStar;

    /** True if pattern ends with '*'. */
    private boolean lastStar;

    /**
     * Constructor.
     *
     * @param patternStringArg
     */
    public CompiledPattern(String patternStringArg) {
        super(patternStringArg);

        // Everything is case-insensitive.
        String patternString = patternStringArg.toLowerCase().trim();

        // If it starts with a '*', we can scan forward to find an initial
        // match.
        if (patternString.startsWith("*")) {
            patternString = patternString.substring(1);
            firstStar = true;
        }

        // If it ends with a '*', there can be tail characters in the match
        // string.
        if (patternString.endsWith("*")) {
            patternString = patternString.substring(0,
                patternString.length() - 1);
            lastStar = true;
        }

        // Precompile pattern into parts.
        partsLower = patternString.split("\\*");
        partsUpper = new String[partsLower.length];
        for (int i = 0; i < partsLower.length; i++) {
            String partLower = partsLower[i];
            partsUpper[i] = partLower.toUpperCase();
        }
    }

    public boolean isMatch(String matchString) {
        int index = 0;
        for (int i = 0; i < partsLower.length; i++) {
            index = indexOf(matchString, i, index);
            boolean first = i == 0;
            boolean last = i + 1 == partsLower.length;
            if (index == -1) {
                return false;
            }
            if (first && !firstStar && index != 0) {
                return false;
            }
            String part = partsLower[i];
            if (last && !lastStar
                && index + part.length() != matchString.length())
            {
                return false;
            }
        }
        return index != -1;
    }

    private int indexOf(String source, int partNo, int fromIndex) {
        String partLower = partsLower[partNo];
        String partUpper = partsUpper[partNo];
        if (fromIndex >= source.length()) {
            return (partLower.length() == 0 ? source.length() : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (partLower.length() == 0) {
            return fromIndex;
        }

        char firstLower = partLower.charAt(0);
        char firstUpper = partUpper.charAt(0);
        int max = source.length() - partLower.length();

        for (int i = fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (!equalChar(source.charAt(i), firstLower, firstUpper)) {
                while (++i <= max
                    && !equalChar(source.charAt(i), firstLower, firstUpper));
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + partLower.length() - 1;
                for (int k = 1; j < end
                    && equalChar(source.charAt(j), partLower.charAt(k),
                        partUpper.charAt(k)); j++, k++);

                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }
        return -1;
    }

}
