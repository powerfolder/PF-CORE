package de.dal33t.powerfolder.util;

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

}
