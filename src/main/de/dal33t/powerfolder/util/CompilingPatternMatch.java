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

import java.util.regex.Pattern;

/**
 * Compiling pattern matcher that uses regex Pattern to match '*' characters
 * to any text. So 'a*c' would match 'ac', 'abc', 'asdfkhc', etc.
 */
public class CompilingPatternMatch {

    /**
     * True if pattern ends with '*'.
     */
    private Pattern pattern;

    /**
     * Constructor.
     *
     * @param patternStringArg
     */
    public CompilingPatternMatch(String patternStringArg) {

        // Everything is case-insensitive.
        String patternString = patternStringArg.toLowerCase().trim();

        // Escape metacharacters with '\'
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < patternString.length(); i++) {
            switch (patternString.charAt(i)) {
                case '*':
                    // Match any text
                    builder.append(".*");
                    break;
                case '\\':
                case '^':
                case '$':
                case '[':
                case ']':
                case '(':
                case ')':
                case '?':
                case '+':
                case '.':
                case '|':
                    // Escape metacharacters
                    builder.append('\\').append(patternString.charAt(i));
                    break;
                default:
                    builder.append(patternString.charAt(i));
            }
        }

        // Match it
        pattern = Pattern.compile(builder.toString());
    }

    /**
     * Does this string match the pattern?
     *
     * @param matchStringArg
     * @return
     */
    public boolean isMatch(String matchStringArg) {
        return pattern.matcher(matchStringArg.toLowerCase().trim()).matches();
    }

}
