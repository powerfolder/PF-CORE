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
 * $Id: FileInfoComparator.java 17997 2012-02-03 13:44:16Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.data;

import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

import java.util.Comparator;

/**
 * Comparator for FileInfoLocation
 *
 * @author <a href="mailto:glasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.15 $
 */
public class FileInfoLocationComparator extends Loggable implements Comparator<FileInfoLocation> {

    // All the available file comparators
    public static final int BY_MODIFIED_DATE = 1;
    public static final int BY_VERSION = 2;
    public static final int BY_SIZE = 3;
    public static final int BY_LOCATION = 4;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private int sortBy;
    private static final FileInfoLocationComparator[] COMPARATORS;

    static {
        COMPARATORS = new FileInfoLocationComparator[8];
        COMPARATORS[BY_MODIFIED_DATE] = new FileInfoLocationComparator(BY_MODIFIED_DATE);
        COMPARATORS[BY_VERSION] = new FileInfoLocationComparator(BY_VERSION);
        COMPARATORS[BY_SIZE] = new FileInfoLocationComparator(BY_SIZE);
        COMPARATORS[BY_LOCATION] = new FileInfoLocationComparator(BY_LOCATION);
    }

    public FileInfoLocationComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public static FileInfoLocationComparator getComparator(int sortByArg) {
        return COMPARATORS[sortByArg];
    }

    /**
     * Compare by various types. If types are the same, sub-compare on file
     * name, for nice table display.
     *
     * @param o1
     * @param o2
     * @return the value
     */
    public int compare(FileInfoLocation o1, FileInfoLocation o2) {

        FileInfo fileInfo1 = o1.getFileInfo();
        FileInfo fileInfo2 = o2.getFileInfo();
        switch (sortBy) {
            case BY_MODIFIED_DATE:
                if (fileInfo1.getModifiedDate() == null
                        && fileInfo2.getModifiedDate() == null) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                } else if (fileInfo1.getModifiedDate() == null) {
                    return BEFORE;
                } else if (fileInfo2.getModifiedDate() == null) {
                    return AFTER;
                }
                int x = fileInfo2.getModifiedDate().compareTo(fileInfo1.getModifiedDate());
                if (x == 0) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                }
                return x;
            case BY_VERSION:
                if (fileInfo1.getFolderInfo() == null && fileInfo2.getFolderInfo() == null) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                } else if (fileInfo1.getFolderInfo() == null) {
                    return BEFORE;
                } else if (fileInfo2.getFolderInfo() == null) {
                    return AFTER;
                } else if (fileInfo1 instanceof DirectoryInfo
                        || fileInfo2 instanceof DirectoryInfo) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                } else {
                    x = fileInfo1.getVersion() - fileInfo2.getVersion();
                    if (x == 0) {
                        return sortByFileName(fileInfo1, fileInfo2, false);
                    }
                    return x;
                }
            case BY_SIZE:
                if (fileInfo1.isLookupInstance() || fileInfo2.isLookupInstance()) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                }
                if (fileInfo1.getSize() < fileInfo2.getSize()) {
                    return BEFORE;
                }
                if (fileInfo1.getSize() > fileInfo2.getSize()) {
                    return AFTER;
                }
                return sortByFileName(fileInfo1, fileInfo2, false);
            case BY_LOCATION:
                int location1 = o1.getLocation();
                int location2 = o2.getLocation();
                if (location1 == location2) {
                    return sortByFileName(fileInfo1, fileInfo2, false);
                }
                if (location1 < location2) {
                    return BEFORE;
                }
                if (location1 > location2) {
                    return AFTER;
                }
                return sortByFileName(fileInfo1, fileInfo2, false);
        }
        return 0;
    }

    private static int sortByFileName(DiskItem o1, DiskItem o2, boolean fullName) {

        // Sort directories before files.
        if (o1.isDiretory() && o2.isFile()) {
            return -1;
        } else if (o1.isFile() && o2.isDiretory()) {
            return 1;
        }
        if (fullName) {
            return o1.getRelativeName().compareToIgnoreCase(
                    o2.getRelativeName());
        } else {
            return o1.getFilenameOnly().compareToIgnoreCase(
                    o2.getFilenameOnly());
        }
    }

    // General ****************************************************************

    public String toString() {
        String stub = "FileInfo comparator, sorting by ";
        String text;
        switch (sortBy) {
            case BY_MODIFIED_DATE:
                text = stub + "modified date";
                break;
            case BY_VERSION:
                text = stub + "version";
                break;
            case BY_SIZE:
                text = stub + "size";
                break;
            case BY_LOCATION:
                text = stub + "location";
                break;
            default:
                text = "???";
        }
        return text;
    }
}
