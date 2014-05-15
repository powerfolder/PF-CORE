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

import de.dal33t.powerfolder.util.Reject;

/**
 * Matching on any texts that end with the given pattern.
 */
public class EndMatchPattern extends AbstractPattern {

    private char[] matchLower;
    private char[] matchUpper;

    /**
     * Constructor.
     *
     * @param patternStringArg
     */
    public EndMatchPattern(String patternStringArg) {
        super(patternStringArg);
        Reject.ifFalse(patternStringArg.lastIndexOf("*") == 0,
            "Pattern must start with * and should not contain any other stars");
        matchLower = getPatternText().replaceAll("\\*", "").toLowerCase()
            .toCharArray();
        matchUpper = getPatternText().replaceAll("\\*", "").toUpperCase()
            .toCharArray();
    }

    public boolean isMatch(String matchString) {
        int matchIndex = matchString.length() - 1;
        if (matchIndex == -1) {
            // Special case. Match "*" to ""
            return matchLower.length == 0;
        }
        if (matchString.length() < matchLower.length) {
            // Impossible
            return false;
        }
        for (int i = matchLower.length - 1; i > 0; i--) {
            char cms = matchString.charAt(matchIndex);
            if (!equalChar(cms, matchLower[i], matchUpper[i])) {
                return false;
            }
            matchIndex--;
        }
        // MATCH!
        return true;
    }

}
