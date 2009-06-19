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
package de.dal33t.powerfolder.disk.problem;

import java.util.*;
import java.io.File;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.Controller;

/**
 * Identifies problems with filenames. Note the directory names mostly have the
 * same restrictions!<BR>
 * Ref: <A HREF="http://en.wikipedia.org/wiki/Filename">Wikepedia/Filename</A>
 * <p/>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilenameProblemHelper {

    /**
     * All names the are not allowed on windows
     */
    private static final String[] RESERVED_WORDS = {"CON", "PRN", "AUX",
            "CLOCK$", "NUL", "COM0", "COM1", "COM2", "COM3", "COM4", "COM5",
            "COM6", "COM7", "COM8", "COM9", "LPT0", "LPT1", "LPT2", "LPT3", "LPT4",
            "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    private static final int MAX_FILENAME_LENGTH = 255;

    /**
     * For performace reasons the reserved filenames are put in a hashmap
     */
    private static final Map<String, String> RESERVED_WORDS_HASH_MAP;

    static {
        RESERVED_WORDS_HASH_MAP = new HashMap<String, String>();
        for (String filename : RESERVED_WORDS) {
            RESERVED_WORDS_HASH_MAP.put(filename.toLowerCase(), filename);
        }
    }

    /**
     * See if there are any problems.
     *
     * @param filename
     * @return
     */
    public static boolean hasProblems(String filename) {
        return containsIllegalLinuxChar(filename)
                || containsIllegalMacOSXChar(filename)
                || containsIllegalWindowsChars(filename)
                || endsWithIllegalWindowsChar(filename)
                || isReservedWindowsFilename(filename) || isToLong(filename);
    }

    /**
     * Create problems for the file.
     *
     * @param fileInfo
     * @return
     */
    public static List<Problem> getProblems(FileInfo fileInfo) {
        String filename = fileInfo.getFilenameOnly();
        List<Problem> returnValue = new ArrayList<Problem>();

        if (containsIllegalLinuxChar(filename)) {
            returnValue.add(new IllegalLinuxCharsFilenameProblem(fileInfo));
        }

        if (containsIllegalMacOSXChar(filename)) {
            returnValue.add(new IllegalMacosxCharsFilenameProblem(fileInfo));
        }

        if (containsIllegalWindowsChars(filename)) {
            returnValue.add(new IllegalWindowsCharsFilenameProblem(fileInfo));
        }

        if (endsWithIllegalWindowsChar(filename)) {
            returnValue.add(new EndIllegalCharsFilenameProblem(fileInfo));
        }

        if (isReservedWindowsFilename(filename)) {
            returnValue.add(new ReservedWordFilenameProblem(fileInfo));
        }

        if (isToLong(filename)) {
            returnValue.add(new TooLongFilenameProblem(fileInfo));
        }
        return returnValue;
    }

    /**
     * Will also return true if file is called AUX.txt or aux!
     * Note: Only public for test access.
     */
    public static boolean isReservedWindowsFilename(String filename) {
        return RESERVED_WORDS_HASH_MAP.containsKey(stripExtension(filename)
                .toLowerCase());
    }

    /**
     * must be a filename only, path elements must be removed
     */
    private static String stripExtension(String filename) {
        int lastPoint = filename.lastIndexOf('.');
        if (lastPoint >= 0) {
            return filename.substring(0, lastPoint);
        }
        return filename;
    }

    /**
     * 0-31 and |\?*<":>/
     */
    public static boolean containsIllegalWindowsChars(String filename) {
        int s = filename.length();
        for (int i = 0; i < s; i++) {
            char aChar = filename.charAt(i);
            // if (aChar <= 31) {
            // return true;
            // }
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

    /**
     * windows filename may not end with . or space ( )
     */
    public static boolean endsWithIllegalWindowsChar(String filename) {
        return filename.endsWith(".") || filename.endsWith(" ");
    }

    /**
     * : and / are illegal on Mac OSX
     */
    private static boolean containsIllegalMacOSXChar(String filename) {
        return filename.contains("/") || filename.contains(":");
    }

    /**
     * / is illegal on Unix
     */
    private static boolean containsIllegalLinuxChar(String filename) {
        return filename.contains("/");
    }

    public static boolean isToLong(String filename) {
        return filename.length() > MAX_FILENAME_LENGTH;
    }

    /**
     * This method tries to rename the file to a better filename without
     * problems.
     */
    public static void resolve(Controller controller, FileInfo fileInfo,
                             String newFilename, Problem problem) {

        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());
        File file = folder.getDiskFile(fileInfo);
        if (!file.exists()) {
            return;
        }

        File newFile = new File(folder.getLocalBase(),
                fileInfo.getLocationInFolder() + '/' + newFilename);
        if (file.renameTo(newFile)) {
            FileInfo renamedFileInfo = FileInfo.newFile(folder, newFile,
                controller.getMySelf().getInfo());
            if (folder.isKnown(fileInfo)) {
                folder.removeFilesLocal(fileInfo);
            }
            folder.scanNewFile(renamedFileInfo);
            fileInfo.getFolder(controller.getFolderRepository()).removeProblem(
                    problem);

        }
    }

//    private static String removeChars(String filenameArg, String charsToRemove) {
//        String filename = filenameArg;
//        for (int i = 0; i < charsToRemove.length(); i++) {
//            char c = charsToRemove.charAt(i);
//            while (filename.indexOf(c) != -1) {
//                int index = filename.indexOf(c);
//                filename = filename.substring(0, index)
//                    + filename.substring(index + 1, filename.length());
//            }
//        }
//        return filename;
//    }

//    private static String makeUniqueAndValid(Controller controller, String newNameArg, FileInfo fileInfo) {
//        StringBuilder newName = new StringBuilder(newNameArg);
//        if (newName.length() == 0) {
//            newName.append("-1");
//            int count = 2;
//            while (!isUnique(controller, newName.toString(), fileInfo)) {
//                newName.append("-" + count++);
//            }
//        } else {
//            int count = 1;
//            while (!isUnique(controller, newName.toString(), fileInfo)) {
//                newName.append("-" + count++);
//            }
//        }
//        return newName.toString();
//    }

    /**
     * add a -1 (or -2 etc if filename not unique) to the filename part (before
     * the extension)
     */
//    private static String addSuffix(Controller controller, FileInfo fileInfo) {
//        int index = fileInfo.getFilenameOnly().lastIndexOf('.');
//        if (index > 0) { // extention found
//            String extension = fileInfo.getFilenameOnly().substring(index,
//                fileInfo.getFilenameOnly().length());
//            String newName = stripExtension(fileInfo.getFilenameOnly()) + "-1"
//                + extension;
//            int count = 2;
//            while (!isUnique(controller, newName, fileInfo)) {
//                newName = stripExtension(fileInfo.getFilenameOnly()) + '-'
//                    + count++ + extension;
//            }
//            return newName;
//        }
//        // no extention
//        String newName = fileInfo.getFilenameOnly() + "-1";
//        int count = 2;
//        while (!isUnique(controller, newName, fileInfo)) {
//            newName = fileInfo.getFilenameOnly() + '-' + count++;
//        }
//        return newName;
//    }

    /**
     * Unique if a file with that name does not exist
     */
    public static boolean isUnique(Controller controller, String newName,
                                   FileInfo fileInfo) {
        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());
        File newFile = new File(folder.getLocalBase(), fileInfo
            .getLocationInFolder()
            + '/' + newName);
        return !newFile.exists();
    }

    /**
     * Tries to find a shorter, unique filename in a folder for a file that has
     * a really long name.
     *
     * @see #isToLong(String)
     * @see TooLongFilenameProblem
     *
     * @param controller
     * @param fileInfo
     * @return
     */
    public static String getShorterFilename(Controller controller,
                                           FileInfo fileInfo) {
        int length = Math.min(MAX_FILENAME_LENGTH,
                fileInfo.getFilenameOnly().length());

        while (!isUnique(controller,
                fileInfo.getFilenameOnly().substring(0, length), fileInfo)) {
            length--;
            if (length < 1) {
                throw new IllegalStateException("Length too small when shortening " +
                        fileInfo.getFilenameOnly());
            }
        }

        return fileInfo.getFilenameOnly().substring(0, length);
    }
}