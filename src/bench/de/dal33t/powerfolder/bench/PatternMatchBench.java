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
import de.dal33t.powerfolder.util.StringUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * This is a bench test to see if we can get better performance using a
 * compiled regex matcher over the existing implementation.
 *
 * The test loops 1000 times over 10 check strings over 10 patterns.
 *
 * The first run uses the existing PatternMatch.isMatch() method.
 *
 * The second run pre-compiles the patterns, then tests with those.
 * Note that the compile time is not included in the run time.
 * Also the regex pattrn has '*' characters replaced with '.*?', which (I
 * believe) is a general non-greedy quantifier. Other regex symbols will need
 * to be escaped in the final implementation.
 *
 * Initial tests indicate a 2:1 speed improvement.
 *
 * http://en.wikipedia.org/wiki/Regular_expression
 */
public class PatternMatchBench extends Bench {

    /** Ten check strings */
    private static final String[] CHECK_STRINGS = {
            "a uhui iauwehru hdkab f",
            "asdfk as,jhlk uhelkh kj",
            "a dfjhlkuahw fklhkljha ",
            "lkjlkhaseu hasjdflbkbsd",
            "a lkhasduy uhj sfja kj ",
            "iuhba bkadsf kjhbkjhba ",
            "jhiu  ads bnbkajhsbf kh",
            "akjkhlkjbxkjhgad fkh as",
            "jjjjjjjjjjjjjjjjjjjjjjj",
            "  lkjhk   kj    jkk h  "
    };

    /** Ten patterns, some with '*' characters */
    private static final String[] PATTERN_STRINGS = {
            "h sdf",
            "s hj*s j",
            "hkjh as ",
            "ye",
            "as*dl j*lkja ",
            "iui*ajj",
            "kj",
            "hlksj",
            "j*j*j",
            "hlkah"
    };

    /**
     * Main.
     *
     * @param args
     */
    public static void main(String[] args) {
        PatternMatchBench bench = new PatternMatchBench();
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
        map.put("Existing implementation", doExistingRun());
        map.put("Regex implementation", doRegexRun());
        return map;
    }

    /**
     * Iterate the check strings and the patterns and do matching.
     *
     * @return
     *          the run time in milliseconds.
     */
    private Comparable doExistingRun() {

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

    /**
     * Compile Patterns, the test by iterating the check strings
     * and the patterns and do matching. Compile time is not included in the
     * results.
     *
     * @return
     *          the run time in milliseconds.
     */
    private Comparable doRegexRun() {

        // Compile patterns
        Pattern[] patterns = new Pattern[PATTERN_STRINGS.length];
        for (int i = 0; i < PATTERN_STRINGS.length; i++) {
            String modifiedPattern = PATTERN_STRINGS[i];
            // Replace '*' with a general regex non-greedy quantifier.
            modifiedPattern = StringUtils.replace(modifiedPattern, "*", ".*?");
            patterns[i] = Pattern.compile(modifiedPattern);
        }

        // Iterate test
        Date start = new Date();
        for (int i = 0; i < 1000; i++) {
            for (String checkString : CHECK_STRINGS) {
                for (Pattern pattern : patterns) {
                    pattern.matcher(checkString).matches();
                }
            }
        }
        Date end = new Date();
        return end.getTime() - start.getTime();
    }
}
