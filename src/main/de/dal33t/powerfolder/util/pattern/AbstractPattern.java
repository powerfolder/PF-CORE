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
    public AbstractPattern(String patternStringArg) {
        patternText = patternStringArg.toLowerCase().trim();
    }

    /*
     * (non-Javadoc)
     * @see de.dal33t.powerfolder.util.pattern.Pattern#getPatternText()
     */
    public final String getPatternText() {
        return patternText;
    }

    /*
     * (non-Javadoc)
     * @see de.dal33t.powerfolder.util.pattern.Pattern#isMatch(java.lang.String)
     */
    public abstract boolean isMatch(String matchString);

    // Internal helper ********************************************************

    protected static boolean equalChar(char c1, char cl2, char cu2) {
        if (c1 == cl2) {
            return true;
        }
        if (c1 == cu2) {
            return true;
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AbstractPattern that = (AbstractPattern) obj;

        return !(patternText != null
            ? !patternText.equals(that.patternText)
            : that.patternText != null);
    }

    public int hashCode() {
        return patternText != null ? patternText.hashCode() : 0;
    }
}
