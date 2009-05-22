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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Compiling pattern matcher that uses regex Pattern to match '*' characters
 * to any text. So 'a*c' would match 'ac', 'abc', 'asdfkhc', etc.
 */
public class CompilingPatternMatch {

    private static final Logger log = Logger.getLogger(CompilingPatternMatch.class.getName());

    /**
     * True if pattern ends with '*'.
     */
    private final Pattern pattern;

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

        patternText = patternStringArg;

        // Do a test now. Better now than when checking a file name...
        pattern.matcher("24y98&TB(*&^#8tqi875tnc8i7t86O*&VTB#87q43").matches();
    }

    /**
     * Does this string match the pattern?
     *
     * @param matchStringArg
     * @return
     */
    public boolean isMatch(String matchStringArg) {
        try {
            return pattern.matcher(matchStringArg.toLowerCase().trim()).matches();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Pattern match problem "
                    + (pattern == null ? "null" : pattern.pattern()), e);
            return false;
        }
    }

    public String getPatternText() {
        return patternText;
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
