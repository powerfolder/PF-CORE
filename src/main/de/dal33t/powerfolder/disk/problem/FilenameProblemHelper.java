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
 * $Id: FilenameProblemHelper.java 16658 2011-10-24 23:22:51Z tot $
 */
package de.dal33t.powerfolder.disk.problem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;

/**
 * Identifies problems with filenames. Note the directory names mostly have the
 * same restrictions!<BR>
 * Ref: <A HREF="http://en.wikipedia.org/wiki/Filename">Wikepedia/Filename</A>
 * <p/>
 * 
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

    public static final String[] ILLEGAL_LINUX_CHARS = {"/"};

    public static final String[] ILLEGAL_WINDOWS_CHARS = {"|", "\\", "?", "\"",
        "*", "<", ":", ">", "/"};

    public static final String[] ILLEGAL_MACOSX_CHARS = {"/", ":"};

    /**
     * See if there are any problems.
     * 
     * @param fInfo
     * @return
     */
    public static boolean hasProblems(FileInfo fInfo) {
        return hasProblems(fInfo.getFilenameOnly());
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
            || isReservedWindowsFilename(filename) || isTooLong(filename);
    }

    /**
     * Create problems for the file.
     * 
     * @param controller
     * @param fileInfo
     * @return
     */
    public static List<Problem> getProblems(Controller controller,
        FileInfo fileInfo)
    {
        String filename = fileInfo.getFilenameOnly();
        List<Problem> returnValue = new ArrayList<Problem>();

        if (PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(controller)) {

            if (containsIllegalLinuxChar(filename)) {
                returnValue.add(new IllegalLinuxCharsFilenameProblem(fileInfo));
            }

            if (containsIllegalMacOSXChar(filename)) {
                returnValue
                    .add(new IllegalMacosxCharsFilenameProblem(fileInfo));
            }

            if (containsIllegalWindowsChars(filename)) {
                returnValue
                    .add(new IllegalWindowsCharsFilenameProblem(fileInfo));
            }

            if (endsWithIllegalWindowsChar(filename)) {
                returnValue.add(new EndIllegalCharsFilenameProblem(fileInfo));
            }

            if (isReservedWindowsFilename(filename)) {
                returnValue.add(new ReservedWordFilenameProblem(fileInfo));
            }

            if (isTooLong(filename)) {
                returnValue.add(new TooLongFilenameProblem(fileInfo));
            }
        }
        return returnValue;
    }

    /**
     * Will also return true if file is called AUX.txt or aux
     */
    public static boolean isReservedWindowsFilename(String filename) {
        String filePart = stripExtension(filename).toUpperCase();
        for (String reservedWord : RESERVED_WORDS) {
            if (reservedWord.equals(filePart)) {
                return true;
            }
        }
        return false;
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
     * |\?*<":>/ illegal in windows
     */
    public static boolean containsIllegalWindowsChars(String filename) {
        for (String illegalLinuxChar : ILLEGAL_WINDOWS_CHARS) {
            if (filename.contains(illegalLinuxChar)) {
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
        for (String illegalLinuxChar : ILLEGAL_MACOSX_CHARS) {
            if (filename.contains(illegalLinuxChar)) {
                return true;
            }
        }
        return false;
    }

    /**
     * / is illegal on Unix
     */
    private static boolean containsIllegalLinuxChar(String filename) {
        for (String illegalLinuxChar : ILLEGAL_LINUX_CHARS) {
            if (filename.contains(illegalLinuxChar)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTooLong(String filename) {
        return filename.length() > MAX_FILENAME_LENGTH;
    }

    /**
     * This method tries to rename the file to a better filename without
     * problems.
     */
    public static void resolve(Controller controller, FileInfo fileInfo,
        String newFilename, Problem problem)
    {

        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());
        Path file = folder.getDiskFile(fileInfo);
        if (Files.exists(file)) {
            return;
        }

        Path newFile = PathUtils.buildFileFromRelativeName(folder
                .getLocalBase(), newFilename);
        try {
            Files.move(file, newFile);
            FileInfo renamedFileInfo = FileInfoFactory.newFile(folder, newFile,
                controller.getMySelf().getInfo(), false);
            if (folder.isKnown(fileInfo)) {
                folder.removeFilesLocal(fileInfo);
            }
            folder.scanChangedFile(renamedFileInfo);
            fileInfo.getFolder(controller.getFolderRepository()).removeProblem(
                problem);
        }
        catch (IOException e) {
        }
    }

    public static String removeChars(String filenameArg, String[] charsToRemove)
    {
        String filename = filenameArg;
        for (String c : charsToRemove) {
            while (filename.contains(c)) {
                int index = filename.indexOf(c);
                filename = filename.substring(0, index)
                    + filename.substring(index + 1, filename.length());
            }
        }
        return filename;
    }

    /**
     * Unique if a file with that name does not exist
     * 
     * @param controller
     * @param newName
     *            the new name WITHOUT path!
     * @param fileInfo
     *            the FileInfo
     * @return if the file with the new name (same path as FileInfo) does not
     *         exists yet.
     */
    public static boolean isUnique(Controller controller, String newName,
        FileInfo fileInfo)
    {
        Folder folder = controller.getFolderRepository().getFolder(
            fileInfo.getFolderInfo());

        String path;
        int i = fileInfo.getRelativeName().lastIndexOf('/');
        if (i > 0) {
            path = fileInfo.getRelativeName().substring(i,
                fileInfo.getRelativeName().length());
        } else {
            path = "/";
        }
        Path newFile = PathUtils.buildFileFromRelativeName(folder
            .getLocalBase(), path + newName);
        return Files.notExists(newFile);
    }

    /**
     * Tries to find a shorter, unique filename in a folder for a file that has
     * a really long name.
     * 
     * @see #isTooLong(String)
     * @see TooLongFilenameProblem
     * @param controller
     * @param fileInfo
     * @return
     */
    public static String getShorterFilename(Controller controller,
        FileInfo fileInfo)
    {
        int length = Math.min(MAX_FILENAME_LENGTH, fileInfo.getFilenameOnly()
            .length());

        while (!isUnique(controller, fileInfo.getFilenameOnly().substring(0,
            length), fileInfo))
        {
            length--;
            if (length < 1) {
                throw new IllegalStateException(
                    "Length too small when shortening "
                        + fileInfo.getFilenameOnly());
            }
        }

        return fileInfo.getFilenameOnly().substring(0, length);
    }

    /**
     * Makes a unique filename in a folder.
     * 
     * @param controller
     * @param fileInfo
     * @return
     */
    public static String makeUnique(Controller controller, FileInfo fileInfo) {
        String filename = fileInfo.getFilenameOnly();
        String extension = "";
        if (filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.'));
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        String extra = "-1";
        int count = 1;
        while (!isUnique(controller, filename + extra + extension, fileInfo)) {
            extra = "-" + count++;
            if (count >= 1000) {
                throw new IllegalStateException(
                    "Unable to fina a unique filename. Ended at: " + filename
                        + extra + extension);
            }
        }
        return filename + extra + extension;
    }
}