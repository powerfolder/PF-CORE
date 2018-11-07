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

import org.jetbrains.annotations.NotNull;

/**
 * Matching on any texts that end with the given pattern.
 */
public abstract class AbstractPattern implements Pattern {

    /**
     * Original pattern text.
     */
    final String patternText;

    /**
     * Constructor.
     *
     * @param patternString The pattern this class should represent.
     */
    AbstractPattern(@NotNull String patternString) {
        patternText = patternString.toLowerCase().trim();
    }

    /*
     * (non-Javadoc)
     * @see de.dal33t.powerfolder.util.pattern.Pattern#getPatternText()
     */
    @Override
    public @NotNull String getPatternText() {
        return patternText;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AbstractPattern)) {
            return false;
        }

        return patternText.equals(((AbstractPattern) obj).patternText);
    }

    public final int hashCode() {
        return patternText.hashCode();
    }

    // Internal helper ********************************************************

    /**
     * Used to see if a char is an upper or lower case character.
     *
     * @param c The character to test
     * @param cLower A lowercase character
     * @param cUpper An uppercase character
     * @return {@code True} if {@code c} equals either {@code cLower} or {@code cUpper}. {@code False} otherwise.
     */
    static boolean equalChar(char c, char cLower, char cUpper) {
        if (c == cLower) {
            return true;
        }
        return c == cUpper;
    }
}
