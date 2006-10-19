package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Identifies problems with filenames. Note the directory names mostly have the
 * same restrictions!<BR>
 * FIXME this should be fixed for directries to, now only the filename part is
 * handled<BR>
 * Ref: <A HREF="http://en.wikipedia.org/wiki/Filename">Wikepedia/Filename</A>
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilenameProblem {
    /** The fileinfo that has problems */
    private FileInfo fileInfo;

    /** The FileInfo that hold the same name (but with differnt case) */
    private FileInfo fileInfoDupe;

    private ProblemType problemType;

    private final static String FILENAME_SUFFIX = "-1";

    public enum ProblemType {
        /** to long on various systems (most have a 255 limit) */
        TO_LONG,
        /** 0-31 and |\?*<":>/ */
        CONTAINS_ILLEGAL_WINDOWS_CHARS,
        /** : and / are illegal on Mac OSX */
        CONTAINS_ILLEGAL_MACOSX_CHARS,
        /** / is illegal on Unix */
        CONTAINS_ILLEGAL_LINUX_CHARS,
        /** like AUX (excludes the extension) */
        IS_RESERVED_WINDOWS_WORD,
        /** filename in winds may not end with . and space ( ) */
        ENDS_WITH_ILLEGAL_WINDOWS_CHARS,
        /** There is a duplicate filename (but with different case) */
        DUPLICATE_FOUND;
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
        this.problemType = ProblemType.DUPLICATE_FOUND;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    /**
     * This method tryes to rename the file to a unique filename without
     * problems.
     * 
     * @return a new FileInfo object or null if solving fails.
     */
    public FileInfo solve(Controller controller) {
        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());
        File file = folder.getDiskFile(fileInfo);
        if (!file.exists()) {
            return null;
        }

        String newName = null;
        switch (problemType) {
            case CONTAINS_ILLEGAL_LINUX_CHARS : {
                // this wont happen now anyway (we will fail te read those files
                // correct)
                // String newName = removeChars(fileInfo.getFilenameOnly(), "/"
                // );
                // new File(folder.getLocalBase(),
                // fileInfo.getLocationInFolder() + "/");
                break;
            }
            case CONTAINS_ILLEGAL_MACOSX_CHARS : {
                newName = removeChars(fileInfo.getFilenameOnly(), ":/");
                newName = makeUniqueAndValid(controller, newName);
                break;
            }
            case CONTAINS_ILLEGAL_WINDOWS_CHARS : {
                newName = removeChars(fileInfo.getFilenameOnly(), "|\\?*<\":>/");
                newName = makeUniqueAndValid(controller, newName);
                break;
            }
            case ENDS_WITH_ILLEGAL_WINDOWS_CHARS : {// add a -1 to the filename
                newName = fileInfo.getFilenameOnly() + FILENAME_SUFFIX;
                int count = 2;
                while (!isUnique(controller, newName)) {
                    newName = fileInfo.getFilenameOnly() + "-" + count++;
                }
                break;
            }
            case IS_RESERVED_WINDOWS_WORD : {
                newName = addSuffix(controller);
                break;
            }
            case TO_LONG : { // shorten till unique filename found
                newName = fileInfo.getFilenameOnly().substring(0, 254);
                int length = 253;
                while (!isUnique(controller, newName)) {
                    newName = fileInfo.getFilenameOnly().substring(0, length--);
                }
                break;
            }
            case DUPLICATE_FOUND : {
                newName = addSuffix(controller);
                break;
            }
            default : {
                throw new IllegalStateException("invalid problemType: "
                    + problemType);
            }
        }
        if (newName == null) {
            return null;
        }

        File newFile = new File(folder.getLocalBase(), fileInfo
            .getLocationInFolder()
            + "/" + newName);
        if (file.renameTo(newFile)) {
            FileInfo renamedFileInfo = new FileInfo(folder, newFile);
            renamedFileInfo.setModifiedInfo(controller.getNodeManager()
                .getMySelf().getInfo(), new Date(newFile.lastModified()));
            fileInfo = renamedFileInfo;
            return renamedFileInfo;
        }
        return null;
    }

    /**
     * add a -1 (or -2 etc if filename not unique) to the filename part (before
     * the extension)
     */
    private String addSuffix(Controller controller) {
        int index = fileInfo.getFilenameOnly().lastIndexOf(".");
        if (index > 0) { // extention found
            String fileSuffix = fileInfo.getFilenameOnly().substring(index + 1,
                fileInfo.getFilenameOnly().length());
            String newName = stripExtension(fileInfo.getFilenameOnly()) + "-1"
                + fileSuffix;
            int count = 2;
            while (!isUnique(controller, newName)) {
                newName = stripExtension(fileInfo.getFilenameOnly()) + "-"
                    + count++ + fileSuffix;
            }
            return newName;
        }
        // no extention
        String newName = fileInfo.getFilenameOnly() + "-1";
        int count = 2;
        while (!isUnique(controller, newName)) {
            newName = fileInfo.getFilenameOnly() + "-" + count++;
        }
        return newName;
    }

    private String makeUniqueAndValid(Controller controller, String newName) {
        if (newName.length() == 0) {
            newName += "-1";
            int count = 2;
            while (!isUnique(controller, newName)) {
                newName += "-" + count++;
            }
        } else {
            int count = 1;
            while (!isUnique(controller, newName)) {
                newName += "-" + count++;
            }
        }
        return newName;
    }

    /** unique if a file with that name does not exist */
    private boolean isUnique(Controller controller, String newName) {
        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());
        File newFile = new File(folder.getLocalBase(), fileInfo
            .getLocationInFolder()
            + "/" + newName);
        return !newFile.exists();
    }

    private static String removeChars(String filename, String charsToRemove) {
        for (int i = 0; i < charsToRemove.length(); i++) {
            char c = charsToRemove.charAt(i);
            while (filename.indexOf(c) != -1) {
                int index = filename.indexOf(c);
                filename = filename.substring(0, index)
                    + filename.substring(index + 1, filename.length());
            }
        }
        return filename;
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

    public String shortDescription() {
        switch (problemType) {
            case CONTAINS_ILLEGAL_LINUX_CHARS : // fallthrough
            case CONTAINS_ILLEGAL_MACOSX_CHARS : // fallthrough
            case CONTAINS_ILLEGAL_WINDOWS_CHARS :
                return "The filename contains characters that are not recommended";
            case ENDS_WITH_ILLEGAL_WINDOWS_CHARS :
                return "The filename ends with characters that are not recommended";
            case IS_RESERVED_WINDOWS_WORD :
                return "The filename is a reserved filename on Windows";
            case TO_LONG :
                return "The filename is to long";
            case DUPLICATE_FOUND :
                return "Duplicate filename found (with differend case)";
        }
        throw new IllegalStateException("invalid problemType: " + problemType);
    }

    /** FIXME i18n */
    public String describeProblem() {
        switch (problemType) {
            case CONTAINS_ILLEGAL_LINUX_CHARS :
                return "The filename contains characters that may cause problems on Unix/Linux computers.\nThe character / is not allowed on those computers";
            case CONTAINS_ILLEGAL_MACOSX_CHARS :
                return "The filename contains characters that may cause problems on Mac OSX computers.\nThe characters / and : are not allowed on those computers";
            case CONTAINS_ILLEGAL_WINDOWS_CHARS :
                return "The filename contains characters that may cause problems on Windows computers.\nThe characters |\\?*<\":>/  and \"controll\" characters (ASCII code 0 till 31)\nare not allowed on those computers";
            case ENDS_WITH_ILLEGAL_WINDOWS_CHARS :
                return "The filename ends with characters that may cause problems on Windows computers.\nThe characters . and space ( ) are not allowed as last characters on those computers";
            case IS_RESERVED_WINDOWS_WORD :
                return "The filename is a reserved filename on Windows,\nit is recommended not to use this names on windows:\n CON, PRN, AUX, CLOCK$, NUL COM0, COM1, COM2, COM3,\nCOM4, COM5,COM6, COM7, COM8, COM9, LPT0, LPT1,\nLPT2, LPT3, LPT4,LPT5, LPT6, LPT7, LPT8, and LPT9.";
            case TO_LONG :
                return "The filename is longer than 255 characters,\nthis in know to cause problems on Windows, Mac OSX and Unix/Linux computers.";
            case DUPLICATE_FOUND :
                return "A filename with the same name is found but it has a differend case:\n"
                    + fileInfoDupe.getName()
                    + "\nthis causes problems on windows computers";
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

    /** 0-31 and |\?*<":>/ */
    public static final boolean containsIllegalWindowsChars(String filename) {
        for (byte aChar : filename.getBytes()) {
            if (aChar <= 31) {
                return true;
            }
            if (aChar == '|') {
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

    // private static final boolean hasProblemsOnWindows(String filename) {
    // return containsIllegalWindowsChars(filename)
    // || endsWithIllegalWindowsChar(filename)
    // || isReservedWindowsFilename(filename) || isToLong(filename);
    // }

    // private static final boolean hasProblemsOnMacOSX(String filename) {
    // return containsIllegalMacOSXChar(filename) || isToLong(filename);
    // }

    // private static final boolean hasProblemsOnLinux(String filename) {
    // return containsIllegalLinuxChar(filename) || isToLong(filename);
    // }

}