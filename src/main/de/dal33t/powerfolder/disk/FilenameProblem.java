package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Identifies problem with filenames. Note the directory names mostly have the
 * same restrictions! Ref: <A
 * HREF="http://en.wikipedia.org/wiki/Filename">Wikepedia/Filename</A>
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilenameProblem {
    private FileInfo fileInfo;
    /** The FileInfo that hold the same name (but with differnt case) */
    private FileInfo fileInfoDupe;
    private ProblemType problemType;

    public enum ProblemType {
        /** to long on variuos systems (most have an 255 limit) */
        TO_LONG,
        /** 0-31 and /\?*<":>+[] */
        CONTAINS_ILLEGAL_WINDOWS_CHARS,
        /** : and / are illegal on Mac OSX */
        CONTAINS_ILLEGAL_MACOSX_CHARS,
        /** / is illegal on Unix */
        CONTAINS_ILLEGAL_LINUX_CHARS,
        /** like AUX (excludes the extension) */
        IS_RESERVED_WINDOWS_WORD,
        /** filename in winds maynot end with . and space ( ) */
        ENDS_WITH_ILLEGAL_WINDOWS_CHARS,
        /** There is a duplicate filename (but with different case) */
        DUPPLICATE_FOUND;
    }

    /** all names the are not allowd on windows */
    private final static String[] reservedWords = {"CON", "PRN", "AUX",
        "CLOCK$", "NUL", "COM0", "COM1", "COM2", "COM3", "COM4", "COM5",
        "COM6", "COM7", "COM8", "COM9", "LPT0", "LPT1", "LPT2", "LPT3", "LPT4",
        "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    /** for performace reasons the reserved filenames are put in a hashmap */
    private static HashMap<String, String> reservedWordsHashMap;

    static {
        reservedWordsHashMap = new HashMap<String, String>();
        for (String filename : reservedWords) {
            reservedWordsHashMap.put(filename.toLowerCase(), filename);
        }
    }

    /** creates a FileName Problem. only used internal use the getProblems method */
    private FilenameProblem(FileInfo fileInfo, ProblemType problemType) {
        this.fileInfo = fileInfo;
        this.problemType = problemType;
    }

    /**
     * creates a FileName Problem with a duplicate (problemType =
     * DUPPLICATE_FOUND)
     */
    public FilenameProblem(FileInfo fileInfo, FileInfo dupe) {
        this.fileInfo = fileInfo;
        this.fileInfoDupe = dupe;
        this.problemType = ProblemType.DUPPLICATE_FOUND;
    }
    

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public FileInfo getFileInfoDupe() {
        return fileInfoDupe;
    }

    public ProblemType getProblemType() {
        return problemType;
    }


    /** FIXME i18n */
    public final String describeProblem() {
        switch (problemType) {
            case CONTAINS_ILLEGAL_LINUX_CHARS :
                return "The filename contains characters that may cause problems on Unix/Linux computers.\nThe character / is not allowed on those computers";
            case CONTAINS_ILLEGAL_MACOSX_CHARS :
                return "The filename contains characters that may cause problems on Mac OSX computers.\nThe characters / and : are not allowed on those computers";
            case CONTAINS_ILLEGAL_WINDOWS_CHARS :
                return "The filename contains characters that may cause problems on Windows computers.\nThe characters /\\?*<\":>+[] and \"controll\" characters (ASCII code 0 till 31) are not allowed on those computers";
            case ENDS_WITH_ILLEGAL_WINDOWS_CHARS :
                return "The filename ends with characters that may cause problems on Windows computers.\nThe characters . and space ( ) are not allowed as last characters on those computers";
            case IS_RESERVED_WINDOWS_WORD :
                return "The filename is a reserved filename on Windows,\nit is recommended not to use this names on windows:\n CON, PRN, AUX, CLOCK$, NUL COM0, COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9,\nLPT0, LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9.";
            case TO_LONG :
                return "The filename is longer than 255 characters,\nthis in know to cause problems on Windows, Mac OSX and Unix/Linux computers.";
        }
        throw new IllegalStateException("invalid problemType: " + problemType);
    }
    
    public static final boolean hasProblems(String filename) {
        return containsIllegalLinuxChar(filename)
            || containsIllegalMacOSXChar(filename)
            || containsIllegalWindowsChars(filename)
            || endsWithIllegalWindowsChar(filename)
            || isReservedWindowsFilename(filename) || isToLong(filename);
    }

    public static final List<FilenameProblem> getProblems(FileInfo fileInfo) {
        String filename = fileInfo.getFilenameOnly();
        if (!hasProblems(filename)) {
            throw new IllegalArgumentException(
                "filename must have problems before we can create the problem ;)");
        }
        List<FilenameProblem> returnValue = new ArrayList<FilenameProblem>(1);
        if (containsIllegalLinuxChar(filename)) {
            returnValue.add(new FilenameProblem(fileInfo,
                ProblemType.CONTAINS_ILLEGAL_LINUX_CHARS));
        }

        if (containsIllegalMacOSXChar(filename)) {
            returnValue.add(new FilenameProblem(fileInfo,
                ProblemType.CONTAINS_ILLEGAL_MACOSX_CHARS));
        }

        if (containsIllegalWindowsChars(filename)) {
            returnValue.add(new FilenameProblem(fileInfo,
                ProblemType.CONTAINS_ILLEGAL_WINDOWS_CHARS));
        }

        if (endsWithIllegalWindowsChar(filename)) {
            returnValue.add(new FilenameProblem(fileInfo,
                ProblemType.ENDS_WITH_ILLEGAL_WINDOWS_CHARS));
        }

        if (isReservedWindowsFilename(filename)) {
            returnValue.add(new FilenameProblem(fileInfo,
                ProblemType.IS_RESERVED_WINDOWS_WORD));
        }

        if (isToLong(filename)) {
            returnValue.add(new FilenameProblem(fileInfo, ProblemType.TO_LONG));
        }
        return returnValue;
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
    private static final boolean containsIllegalMacOSXChar(String filename) {
        return filename.contains("/") || filename.contains(":");
    }

    /** / is illegal on Unix */
    private static final boolean containsIllegalLinuxChar(String filename) {
        return filename.contains("/");
    }

    public static final boolean isToLong(String filename) {
        return filename.length() > 255;
    }

    
//    private static final boolean hasProblemsOnWindows(String filename) {
 //       return containsIllegalWindowsChars(filename)
 //           || endsWithIllegalWindowsChar(filename)
 //           || isReservedWindowsFilename(filename) || isToLong(filename);
//    }

 //   private static final boolean hasProblemsOnMacOSX(String filename) {
 //       return containsIllegalMacOSXChar(filename) || isToLong(filename);
 //   }

 //   private static final boolean hasProblemsOnLinux(String filename) {
//        return containsIllegalLinuxChar(filename) || isToLong(filename);
//    }
 
}