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
* $Id$
*/
package de.dal33t.powerfolder.util;

/**
 * This class has a single method that allows you to determine whether or not a
 * given string matches a given pattern. See the information in the isMatch
 * method for more information.
 * <P>
 * If you're using the JDK (aka the J2SE) 1.4 or higher, you should probably use
 * the java.util.regex package to make Regular Expressions to provide string
 * pattern-matching functionality, because it will give you a much greater range
 * of functionality. There are (at the time of this writing) some good examples
 * of Regular Expression usage <A
 * HREF="http://developer.java.sun.com/developer/technicalArticles/releases/1.4regex/">
 * here</A> on the Sun website.
 * <P>
 * You may use this code as you wish, just don't pretend that you wrote it
 * yourself, and don't hold me liable for anything that it does or doesn't do.
 * If you're feeling especially honest, please include a link to nsftools.com
 * along with the code.
 * <p>
 * For updates or more information about this program, please visit <a
 * href="http://www.nsftools.com">www.nsftools.com</a>
 * 
 * Jvo: changed for powerfolder, removed all ? and [] stuff
 * 
 * @author Julian Robichaux
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version 1.00
 * @deprecated use CompilingPatternMatch - faster - see PatternMatchBench.
 */
public class PatternMatch {

    /**
     * Returns a boolean value indicating whether or not checkString matches the
     * pattern. The pattern can include single characters, a range of characters
     * enclosed in brackets, a question mark (which matches exactly one
     * character), or an asterisk (which matches zero or more characters).
     * <P>
     * If you're matching a character range, it can be either single characters,
     * like [abc], or a range of characters, like [a-c], or a combination, like
     * [a-clmnx-z]. For example, the pattern 'b[aiu]t' would match 'bat', 'bit',
     * and 'but', and the pattern 'a[1-9]' would match a1, a2, a3, a4, a5, a6,
     * a7, a8, and a9.
     * <P>
     * This should all work much (exactly?) like file matching in DOS. For
     * example, a pattern of '*.txt' should match all strings ending in '.txt',
     * '*.*' should match all strings with a '.' in them, '*.???' should match
     * strings with a three letter extension, and so on.
     * <P>
     * Also, please note that the pattern check IS case-IN-sensitive!
     */
    public static boolean isMatch(String checkStringArg, String patternArg) {

        String checkString = checkStringArg.toLowerCase().trim();
        String pattern = patternArg.toLowerCase().trim();

        // If pattern does not contain a '*', then we can do a simple equals.
        if (!pattern.contains("*")) {
            return pattern.equals(checkString);
        }

        int patternPos = 0;

        for (int i = 0; i < checkString.length(); i++) {
            // if we're at the end of the pattern but not the end
            // of the string, return false
            if (patternPos >= pattern.length()) {
                return false;
            }

            // grab the characters we'll be looking at
            char patternChar = pattern.charAt(patternPos);
            char thisChar = checkString.charAt(i);

            if (patternChar == '*') {
                // if this is the last thing we're matching, we have a match
                if (patternPos >= pattern.length() - 1) {
                    return true;
                }

                // otherwise, do a recursive search
                for (int j = i; j < checkString.length(); j++) {
                    if (isMatch(checkString.substring(j), pattern
                            .substring(patternPos + 1))) {
                        return true;
                    }
                }

                // if we never returned from that, there is no match
                return false;

            }

            // the default condition is to do an exact character match
            if (thisChar != patternChar) {
                return false;
            }

            // advance the patternPos before we loop again
            patternPos++;

        }

        // if there's still something in the pattern string, check to
        // see if it's one or more '*' characters. If that's all it is,
        // just advance to the end
        int k;
        for (k = patternPos; k < pattern.length(); k++) {
            if (pattern.charAt(k) != '*') {
                break;
            }
        }
        patternPos = k;

        // at the end of all this, if we're at the end of the pattern
        // then we have a good match
        return patternPos == pattern.length();
    }
}
