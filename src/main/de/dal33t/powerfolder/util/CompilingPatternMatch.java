/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.util;

import java.util.Arrays;

/**
 * Compiling pattern matcher that uses compiled parts to match '*' characters to
 * any text. So 'a*c' would match 'ac', 'abc', 'asdfkhc', etc.
 */
public class CompilingPatternMatch {

    /** Precompiled parts to match on. */
    private String[] partsLower;
    private String[] partsUpper;

    /** True if pattern begins with '*'. */
    private boolean firstStar;

    /** True if pattern ends with '*'. */
    private boolean lastStar;

    /**
     * Original pattern text.
     */
    private final String patternText;

    /**
     * Constructor.
     * 
     * @param patternStringArg
     */
    public CompilingPatternMatch(String patternStringArg) {

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
        // System.out.println("Got parts: " + Arrays.asList(parts));

        patternText = patternString;
    }

    public boolean isMatch(String matchString) {
        int index = 0;
        for (int i = 0; i < partsLower.length; i++) {
            String part = partsLower[i];
            index = indexOf(matchString, i, index);
            // index = matchString.indexOf(part, index);
            boolean first = i == 0;
            boolean last = i + 1 == partsLower.length;
            if (index == -1) {
                return false;
            }
            if (first && !firstStar && index != 0) {
                return false;
            }
            if (last && !lastStar
                && index + part.length() != matchString.length())
            {
                return false;
            }
        }
        return index != -1;
    }

    int indexOf(String source, int partNo, int fromIndex) {
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

    private static boolean equalChar(char c1, char cl2, char cu2) {
        if (c1 == cl2) {
            return true;
        }
        if (c1 == cu2) {
            return true;
        }

        // // If characters don't match but case may be ignored,
        // // try converting both characters to uppercase.
        // // If the results match, then the comparison scan should
        // // continue.
        // char u1 = Character.toUpperCase(c1);
        // char u2 = Character.toUpperCase(c2);
        // if (u1 == u2) {
        // return true;
        // }
        // // Unfortunately, conversion to uppercase does not work properly
        // // for the Georgian alphabet, which has strange rules about case
        // // conversion. So we need to make one last check before
        // // exiting.
        // if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
        // return true;
        // }

        // if (Character.toLowerCase(c1) == c2) {
        // return true;
        // }

        return false;
    }

    public boolean isMatchOld(String matchStringArg) {
        String matchString = matchStringArg.toLowerCase();

        // Precalculate the length of the match string.
        int matchStringLength = matchString.length();

        // Initially this is the first part.
        boolean firstPart = true;

        // Point to the first character of the matchString.
        int matchPointer = 0;

        // Iterate the pattern parts.
        for (String part : partsLower) {

            // Check that we have enough characters left to match on.
            int partLength = part.length();

            // We can scan from this point if there was a '*' at the begining of
            // the pattern or if this is not the first part.
            boolean scanning = firstStar || !firstPart;

            // Check and scan forward until we get a match or cannot continue.
            do {

                // Current part is longer than the remaining section of the
                // matchString, so can not match.
                if (partLength > matchStringLength - matchPointer) {
                    return false;
                }

                // Look for match of part with current position of matchString.
                boolean partEqualsCurrentMatchPart;

                // int foundPartAt = matchString.indexOf(part, matchPointer);
                // partEqualsCurrentMatchPart = foundPartAt >= 0
                // && foundPartAt <= matchPointer + partLength;

                String currentMatchPart = matchString.substring(matchPointer,
                    matchPointer + partLength);
                partEqualsCurrentMatchPart = part.equals(currentMatchPart);

                if (partEqualsCurrentMatchPart) {

                    // Good so far, next part on the next section of the
                    // matchString.
                    matchPointer += partLength;

                    // Exit scan loop.
                    scanning = false;

                } else if (scanning) {

                    // Try the next text position
                    matchPointer++;

                } else {

                    // Mismatch and cannot scan. Overall match has failed.
                    return false;

                }

            } while (scanning);

            // Next part is not the first.
            firstPart = false;
        }

        // Match if finally pointing at end of matchString or if there was a '*'
        // at the end of the pattern.
        return lastStar || matchPointer == matchStringLength;
    }

    public String getRealPatternText() {
        return (firstStar ? "*" : "") + patternText + (lastStar ? "*" : "");
    }

    public boolean isFirstStar() {
        return firstStar;
    }

    public boolean isLastStar() {
        return lastStar;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CompilingPatternMatch that = (CompilingPatternMatch) obj;

        return !(patternText != null
            ? !patternText.equals(that.patternText)
            : that.patternText != null);
    }

    public int hashCode() {
        return patternText != null ? patternText.hashCode() : 0;
    }
}
