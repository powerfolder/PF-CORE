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
import org.jetbrains.annotations.NotNull;

/**
 * Matching on any texts that end with the given pattern.
 */
public class StartMatchPattern extends AbstractPattern {

    /**
     * @see AbstractPattern#AbstractPattern(String)
     */
    StartMatchPattern(@NotNull String patternString) {
        super(patternString.replace("*", ""));
        Reject.ifFalse(patternString.indexOf("*") == patternString
            .length() - 1,
            "Pattern must end with * and should not contain any other stars");
    }

    @Override
    public boolean isMatch(@NotNull String matchString) {
        return matchString.toLowerCase().startsWith(patternText);
    }

}
