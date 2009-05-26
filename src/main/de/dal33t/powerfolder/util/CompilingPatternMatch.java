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

/**
 * Compiling pattern matcher that uses compiled parts to match '*' characters
 * to any text. So 'a*c' would match 'ac', 'abc', 'asdfkhc', etc.
 */
public class CompilingPatternMatch {

    /** Precompiled parts to match on. */
    private String[] parts;

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

        // If it starts with a '*', we can scan forward to find an initial match.
        if (patternString.startsWith("*")) {
            patternString = patternString.substring(1);
            firstStar = true;
        }

        // If it ends with a '*', there can be tail characters in the match string.
        if (patternString.endsWith("*")) {
            patternString = patternString.substring(0, patternString.length() - 1);
            lastStar = true;
        }

        // Precompile pattern into parts.
        parts = patternString.split("\\*");

        patternText = patternString;
    }

    public boolean isMatch(String matchStringArg) {

        // Everything is case-insensitive.
        String matchString = matchStringArg.toLowerCase().trim();

        // Precalculate the length of the match string.
        int matchStringLength = matchString.length();

        // Initially this is the first part.
        boolean firstPart = true;

        // Point to the first character of the matchString.
        int matchPointer = 0;

        // Iterate the pattern parts.
        for (String part : parts) {

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
                String currentMatchPart = matchString.substring(matchPointer,
                        matchPointer + partLength);
                if (part.equals(currentMatchPart)) {

                    // Good so far, next part on the next section of the matchString.
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


    public String getPatternText() {
        return patternText;
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
