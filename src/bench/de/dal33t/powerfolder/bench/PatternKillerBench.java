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
* $Id: PatternMatchBench.java 8022 2009-05-21 07:46:07Z harry $
*/
package de.dal33t.powerfolder.bench;

import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.CompilingPatternMatch;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * This is a bench test to performance test pattern matching with long * chains.
 * The original implementation does not handle these well, because of
 * exponential depth processing. The new implementation has a linear
 * implementation.
 *
 * The test loops 1000 times over the string for the pattern.
 *
 * The first run uses the existing PatternMatch.isMatch() method.
 *
 * The second run pre-compiles the pattern, then tests with check string.
 * Note that the compile time is not included in the run time.
 */
public class PatternKillerBench extends Bench {

    /** Check strings */
    private static final String[] CHECK_STRINGS = {
            "aaaaaaaaaaaaaaaaaaaaaaaaaab"
    };

    /** Patterns, with many '*' characters */
    private static final String[] PATTERN_STRINGS = {
            "a*a*a*a*a"
    };

    /**
     * Main.
     *
     * @param args
     */
    public static void main(String[] args) {
        PatternKillerBench bench = new PatternKillerBench();
        bench.run();
    }

    /**
     * The run that takes the lowest time is the winner.
     *
     * @return
     */
    protected boolean winnerIsGreatest() {
        return false;
    }

    /**
     * Get results for both runs.
     *
     * @return
     */
    protected Map<String, Comparable> getResults() {
        HashMap<String, Comparable> map = new HashMap<String, Comparable>();
        map.put("Old implementation", doOldRun());
        map.put("New implementation", doNewRun());
        return map;
    }

    /**
     * Iterate the check strings and the patterns and do matching.
     *
     * @return
     *          the run time in milliseconds.
     */
    private static Comparable doOldRun() {

        // Iterate test
        Date start = new Date();
        for (int i = 0; i < 1000; i++) {
            for (String checkString : CHECK_STRINGS) {
                for (String patternString : PATTERN_STRINGS) {
                    PatternMatch.isMatch(checkString, patternString);
                }
            }
        }
        Date end = new Date();
        return end.getTime() - start.getTime();
    }

    private static Comparable doNewRun() {
        CompilingPatternMatch[] patterns = new CompilingPatternMatch[PATTERN_STRINGS.length];
        for (int i = 0; i < PATTERN_STRINGS.length; i++) {
            String modifiedPattern = PATTERN_STRINGS[i];
            patterns[i] = new CompilingPatternMatch(modifiedPattern);
        }

        // Iterate test
        Date start = new Date();
        for (int i = 0; i < 1000; i++) {
            for (String checkString : CHECK_STRINGS) {
                for (CompilingPatternMatch pattern : patterns) {
                    pattern.isMatch(checkString);
                }
            }
        }
        Date end = new Date();
        return end.getTime() - start.getTime();
    }

}