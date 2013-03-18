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
 * Matching on any texts that end with the given pattern.
 */
public abstract class AbstractPattern implements Pattern {

    /**
     * Original pattern text.
     */
    private final String patternText;

    /**
     * Constructor.
     * 
     * @param patternStringArg
     */
    protected AbstractPattern(String patternStringArg) {
        patternText = patternStringArg.toLowerCase().trim();
    }

    /*
     * (non-Javadoc)
     * @see de.dal33t.powerfolder.util.pattern.Pattern#getPatternText()
     */
    public String getPatternText() {
        return patternText;
    }

    public final boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof AbstractPattern)) {
            return false;
        }

        AbstractPattern that = (AbstractPattern) obj;

        return  patternText == null
                ? that.patternText == null
                : patternText.equals(that.patternText);
    }

    public final int hashCode() {
        return patternText == null ? 0 : patternText.hashCode();
    }

    // Internal helper ********************************************************

    /**
     * Used to see if a char is an upper or lower case.
     *
     * @param c
     * @param cLower
     * @param cUpper
     * @return
     */
    protected static boolean equalChar(char c, char cLower, char cUpper) {
        if (c == cLower) {
            return true;
        }
        if (c == cUpper) {
            return true;
        }
        return false;
    }

}
