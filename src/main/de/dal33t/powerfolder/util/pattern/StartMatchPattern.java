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
public class StartMatchPattern extends AbstractPattern {

    private char[] matchLower;
    private char[] matchUpper;

    /**
     * Constructor.
     *
     * @param patternStringArg
     */
    public StartMatchPattern(String patternStringArg) {
        super(patternStringArg);
        Reject.ifFalse(patternStringArg.indexOf("*") == patternStringArg
            .length() - 1,
            "Pattern must end with * and should not contain any other stars");
        matchLower = getPatternText().replaceAll("\\*", "").toLowerCase()
            .toCharArray();
        matchUpper = getPatternText().replaceAll("\\*", "").toUpperCase()
            .toCharArray();
    }

    public boolean isMatch(String matchString) {
        if (matchString.length() < matchLower.length) {
            return false;
        }
        for (int i = 0; i < matchLower.length; i++) {
            char cms = matchString.charAt(i);
            if (!equalChar(cms, matchLower[i], matchUpper[i])) {
                return false;
            }
        }
        // MATCH!
        return true;
    }

}
