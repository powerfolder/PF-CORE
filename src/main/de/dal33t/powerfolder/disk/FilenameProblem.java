package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Identifies problem with filenames. Note the directory names mostly have the
 * same restrictions! Ref: <A
 * HREF="http://en.wikipedia.org/wiki/Filename">Wikepedia/Filename</A>
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilenameProblem {

    private final static String[] reservedWords = {"CON", "PRN", "AUX",
        "CLOCK$", "NUL", "COM0", "COM1", "COM2", "COM3", "COM4", "COM5",
        "COM6", "COM7", "COM8", "COM9", "LPT0", "LPT1", "LPT2", "LPT3", "LPT4",
        "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    private static HashMap<String, String> reservedWordsHashMap;
    static {
        // for performace reasons the reserved filenames are put in a hashmap
        reservedWordsHashMap = new HashMap<String, String>();
        for (String filename : reservedWords) {
            reservedWordsHashMap.put(filename.toLowerCase(), filename);
        }
    }

    /** must be a filename only, path elements must be removed */
    private static final String stripExtension(String filename) {
        int lastPoint = filename.lastIndexOf(".");
        if (lastPoint >= 0) {
            return filename.substring(0, lastPoint);
        }
        return filename;
    }

    /** Will also return true if file is called AUX.txt or aux! */
    public static final boolean isReservedWindowsFilename(String filename) {
        return reservedWordsHashMap.containsKey(stripExtension(filename)
            .toLowerCase());
    }

    /** 0-31 and /\?*<":>+[] */
    public static final boolean containsIllegalWindowsChars(String filename) {
        for (byte aChar : filename.getBytes()) {
            if (aChar <= 31) {
                return true;
            }
            if (aChar == '\\') {
                return true;
            }
            if (aChar == '/') {
                return true;
            }
            if (aChar == '?') {
                return true;
            }
            if (aChar == '*') {
                return true;
            }
            if (aChar == '<') {
                return true;
            }
            if (aChar == '"') {
                return true;
            }
            if (aChar == ':') {
                return true;
            }
            if (aChar == '<') {
                return true;
            }
            if (aChar == '>') {
                return true;
            }
            if (aChar == '+') {
                return true;
            }
            if (aChar == ']') {
                return true;
            }
            if (aChar == '[') {
                return true;
            }
        }
        return false;
    }

    /** windows filename may not end with . or space ( ) */
    public static final boolean endsWithIllegalWindowsChar(String filename) {
        return filename.endsWith(".") || filename.endsWith(" ");
    }

    /** : and / are illegal on Mac OSX */
    public static final boolean containsIllegalMacOSXChar(String filename) {
        return filename.contains("/") || filename.contains(":");
    }

    /** / is illegal on Unix */
    public static final boolean containsIllegalLinuxChar(String filename) {
        return filename.contains("/");
    }

    public static final boolean isToLong(String filename) {
        return filename.length() > 255;
    }

    public static final boolean hasProblems(String filename) {
        return containsIllegalLinuxChar(filename)
            || containsIllegalMacOSXChar(filename)
            || containsIllegalWindowsChars(filename)
            || endsWithIllegalWindowsChar(filename)
            || isReservedWindowsFilename(filename) || isToLong(filename);
    }

    public static final boolean hasProblemsOnWindows(String filename) {
        return containsIllegalWindowsChars(filename)
            || endsWithIllegalWindowsChar(filename)
            || isReservedWindowsFilename(filename) || isToLong(filename);
    }

    public static final boolean hasProblemsOnMacOSX(String filename) {
        return containsIllegalMacOSXChar(filename) || isToLong(filename);
    }

    public static final boolean hasProblemsOnLinux(String filename) {
        return containsIllegalLinuxChar(filename) || isToLong(filename);
    }

    /** FIXME i18n */
    public static final List<String> describeProblems(String filename) {
        if (!hasProblems(filename)) {
            throw new IllegalArgumentException(
                "filename must have problems before we can describe the problem ;)");
        }
        List<String> returnValue = new ArrayList<String>(1);
        if (containsIllegalLinuxChar(filename)) {
            returnValue
                .add("The filename contains characters that may cause problems on Unix/Linux computers.\nThe character / is not allowed on those computers");
        }

        if (containsIllegalMacOSXChar(filename)) {
            returnValue
                .add("The filename contains characters that may cause problems on Mac OSX computers.\nThe characters / and : are not allowed on those computers");
        }

        if (containsIllegalWindowsChars(filename)) {
            returnValue
                .add("The filename contains characters that may cause problems on Windows computers.\nThe characters /\\?*<\":>+[] and \"controll\" characters (ASCII code 0 till 31) are not allowed on those computers");
        }

        if (endsWithIllegalWindowsChar(filename)) {
            returnValue
                .add("The filename ends with characters that may cause problems on Windows computers.\nThe characters . and space ( ) are not allowed as last characters on those computers");
        }

        if (isReservedWindowsFilename(filename)) {
            returnValue
                .add("The filename is a reserved filename on Windows,\nit is recommended not to use this names on windows:\n CON, PRN, AUX, CLOCK$, NUL COM0, COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9,\nLPT0, LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9.");
        }

        if (isToLong(filename)) {
            returnValue
                .add("The filename is longer than 255 characters,\nthis in know to cause problems on Windows, Mac OSX and Unix/Linux computers.");
        }
        return returnValue;
    }
}