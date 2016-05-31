package de.dal33t.powerfolder.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class StringUtils {
	/**
     * <p>Checks if a String is empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isEmpty(null) = true
     * StringUtils.isEmpty("") = true
     * StringUtils.isEmpty(" ") = false
     * StringUtils.isEmpty("tot") = false
     * StringUtils.isEmpty(" tot ") = false
     * </pre>
     *
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty(String   str) {
        return str == null || str.length() == 0;
    }

    /**
     * <p>Checks if a String is not empty ("") and not null.</p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null) = false
     * StringUtils.isNotEmpty("") = false
     * StringUtils.isNotEmpty(" ") = true
     * StringUtils.isNotEmpty("tot") = true
     * StringUtils.isNotEmpty(" tot ") = true
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null
     */
    public static boolean isNotEmpty(String   str) {
        return str != null && str.length() > 0;
    }

    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null) = true
     * StringUtils.isBlank("") = true
     * StringUtils.isBlank(" ") = true
     * StringUtils.isBlank("tot") = false
     * StringUtils.isBlank(" tot ") = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     * @since 2.0
     */
    public static boolean isBlank(String   str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null) = false
     * StringUtils.isNotBlank("") = false
     * StringUtils.isNotBlank(" ") = false
     * StringUtils.isNotBlank("tot") = true
     * StringUtils.isNotBlank(" tot ") = true
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is
     * not empty and not null and not whitespace
     * @since 2.0
     */
    public static boolean isNotBlank(String   str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return false;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Replaces all occurrences of a String within another String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *) = null
     * StringUtils.replace("", *, *) = ""
     * StringUtils.replace("any", null, *) = "any"
     * StringUtils.replace("any", *, null) = "any"
     * StringUtils.replace("any", "", *) = "any"
     * StringUtils.replace("aba", "a", null) = "aba"
     * StringUtils.replace("aba", "a", "") = "b"
     * StringUtils.replace("aba", "a", "z") = "zbz"
     * </pre>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text text to search and replace in, may be null
     * @param repl the String to search for, may be null
     * @param with the String to replace with, may be null
     * @return the text with any replacements processed,
     * <code>null</code> if null String input
     */
    public static String   replace(String   text, String   repl, String   with) {
        return replace(text, repl, with, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *) = null
     * StringUtils.replace("", *, *, *) = ""
     * StringUtils.replace("any", null, *, *) = "any"
     * StringUtils.replace("any", *, null, *) = "any"
     * StringUtils.replace("any", "", *, *) = "any"
     * StringUtils.replace("any", *, *, 0) = "any"
     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
     * StringUtils.replace("abaa", "a", "", -1) = "b"
     * StringUtils.replace("abaa", "a", "z", 0) = "abaa"
     * StringUtils.replace("abaa", "a", "z", 1) = "zbaa"
     * StringUtils.replace("abaa", "a", "z", 2) = "zbza"
     * StringUtils.replace("abaa", "a", "z", -1) = "zbzz"
     * </pre>
     *
     * @param text text to search and replace in, may be null
     * @param repl the String to search for, may be null
     * @param with the String to replace with, may be null
     * @param max maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed,
     * <code>null</code> if null String input
     */
    public static String   replace(String   text, String   repl, String   with, int max) {
        if (text == null || isEmpty(repl) || with == null || max == 0) {
            return text;
        }

        StringBuilder buf = new StringBuilder(text.length());
        int start = 0;
        int end;
        while ((end = text.indexOf(repl, start)) != -1) {
            buf.append(text.substring(start, end)).append(with);
            start = end + repl.length();

            if (--max == 0) {
                break;
            }
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static boolean isEqual(String string1, String string2) {
        if (string1 == null && string2 == null) {
            return true;
        } else if (string1 != null && string2 != null) {
            return string1.equals(string2);
        } else {
            return false;
        }
    }

    /**
     * Concatenates a list of Strings, using a given seperator.
     *
     * <pre>
     * StringUtils.join(",", []) = ""
     * StringUtils.join(",", ["a", "b"]) = "a,b"
     * StringUtils.join("x", ["a", "b", "c", "d", "e"]) = "axbxcxdxe"
     * </pre>
     *
     * @param separator
     *            a String to separate the joined values. May be empty.
     * @param strings
     *            a list of Strings to be joined
     * @return a single String containing all of the individual strings
     */
    public static String join(String separator, Iterable<String> strings) {
        Iterator<String> it = strings.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(it.next());
        while (it.hasNext()) {
            sb.append(separator);
            sb.append(it.next());
        }
        return sb.toString();
    }


    /**
     * Concatenates a list of Strings, using a given seperator.
     *
     * <pre>
     * StringUtils.join(",") = ""
     * StringUtils.join(",", "a", "b") = "a,b"
     * StringUtils.join("x", "a", "b", "c", "d", "e") = "axbxcxdxe"
     * </pre>
     *
     * @param separator
     *            a String to separate the joined values. May be empty.
     * @param strings
     *            a list of Strings to be joined
     * @return a single String containing all of the individual strings
     */
    public static String join(String separator, String... strings) {
        return join(separator, Arrays.asList(strings));
    }

    /**
     * Count the number of {@code c} chars in {@code input}.
     * 
     * @param input
     * @param c
     * @return
     */
    public static int countChar(String input, char c) {
        Reject.ifBlank(input, "String is blank");

        int count = 0;
        int i = 0;

        for (i = input.indexOf(c, i); i != -1; count++) {
            i = input.indexOf(c, i + 1);
        }

        return count;
    }

    /**
     * Checks if a string starts with a substring - ignoring the case.
     *
     * @param inputString
     *              a string that will be checked
     * @param startString
     *              a string to check if the inputString starts with this startString
     * @return a boolean value:
     *              {@code true} if the inputString starts with the subString
     *              {@code false} if the inputString does not start with the subString
     */

    public static boolean startsWithString(String inputString, String startString){
        Reject.ifNull(inputString, "inputString is blank");
        Reject.ifNull(startString, "subString is blank");

        return (inputString.toLowerCase().startsWith(startString.toLowerCase()));
    }

    /**
     * Checks if a string contains a specific substring - ignoring the case.
     *
     * @param inputString
     *              a string that will be checked
     * @param subString
     *              a string to check if the inputString starts with this subString
     * @return a boolean value:
     *              {@code true} if the inputString starts with the subString
     *              {@code false} if the inputString does not start with the subString
     */

    public static boolean hasSubString(String inputString, String subString){
        Reject.ifNull(inputString, "inputString is blank");
        Reject.ifNull(subString, "subString is blank");

        return (inputString.toLowerCase().contains(subString.toLowerCase()));
    }
    
    /**
     * Cuts a string down to the last 1024 characters cutting only at line ends
     * 
     * @param notes
     *              input string
     * @return cut string
     */
    public static String cutNotes(String notes) {
        if (notes.length() <= 2048) {
            return notes;
        }
        String last2049Characters = notes.substring(notes.length() - 2049);
        int positionOfLineBreak = last2049Characters.indexOf("\n");
        if ( positionOfLineBreak <= -1 || positionOfLineBreak >= 2048) {
            positionOfLineBreak = 0;
        }
        return last2049Characters.substring(positionOfLineBreak + 1, 2049);
    }

    /**
     * Seperated a given string by it's line endings
     *
     * /r is for line endings on mac os
     *
     * @param string input string
     * @return seperated string array
     */

    public static List cutOnLineBreaks(String string) {

        List<String> seperated = Arrays.asList(string.replaceAll("\\r", "").split("\\n"));
        return seperated;
    }

}
