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
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator for FileInfo
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.15 $
 */
public class FileInfoComparator extends Loggable implements
    Comparator<FileInfo>
{

    // All the available file comparators
    public static final int BY_FILE_TYPE = 0;
    public static final int BY_NAME = 1;
    public static final int BY_RELATIVE_NAME = 2;
    public static final int BY_SIZE = 3;
    public static final int BY_MEMBER = 4;
    public static final int BY_MODIFIED_DATE = 5;
    public static final int BY_FOLDER = 6;
    public static final int BY_VERSION = 7;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private int sortBy;
    private static final FileInfoComparator[] comparators;

    static {
        comparators = new FileInfoComparator[8];
        comparators[BY_FILE_TYPE] = new FileInfoComparator(BY_FILE_TYPE);
        comparators[BY_NAME] = new FileInfoComparator(BY_NAME);
        comparators[BY_RELATIVE_NAME] = new FileInfoComparator(BY_RELATIVE_NAME);
        comparators[BY_SIZE] = new FileInfoComparator(BY_SIZE);
        comparators[BY_MEMBER] = new FileInfoComparator(BY_MEMBER);
        comparators[BY_MODIFIED_DATE] = new FileInfoComparator(BY_MODIFIED_DATE);
        comparators[BY_FOLDER] = new FileInfoComparator(BY_FOLDER);
        comparators[BY_VERSION] = new FileInfoComparator(BY_VERSION);
    }

    public FileInfoComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public static FileInfoComparator getComparator(int sortByArg) {
        return comparators[sortByArg];
    }

    /**
     * Compare by various types. If types are the same, sub-compare on file
     * name, for nice table display.
     *
     * @param o1
     * @param o2
     * @return the value
     */
    public int compare(FileInfo o1, FileInfo o2) {

        switch (sortBy) {
            case BY_FILE_TYPE :
                String ext1 = o1.getExtension();
                String ext2 = o2.getExtension();
                if (ext1 == null || ext2 == null) {
                    return sortByFileName(o1, o2, false);
                }
                int x = ext1.compareTo(ext2);
                if (x == 0) {
                    return sortByFileName(o1, o2, false);
                }
                return x;
            case BY_NAME :
                if (o1.isDiretory() && !o2.isDiretory()) {
                    return Integer.MIN_VALUE;
                }
                if (o2.isDiretory() && !o1.isDiretory()) {
                    return Integer.MAX_VALUE;
                }
                return sortByFileName(o1, o2, false);
            case BY_RELATIVE_NAME :
                return sortByFileName(o1, o2, true);
            case BY_SIZE :
                if (o1.isLookupInstance() || o2.isLookupInstance()) {
                    return sortByFileName(o1, o2, false);
                }
                if (o1.getSize() < o2.getSize()) {
                    return BEFORE;
                }
                if (o1.getSize() > o2.getSize()) {
                    return AFTER;
                }
                return sortByFileName(o1, o2, false);
            case BY_MEMBER :
                if (o1.getModifiedBy() == null && o2.getModifiedBy() == null) {
                    return sortByFileName(o1, o2, false);
                } else if (o1.getModifiedBy() == null) {
                    return BEFORE;
                } else if (o2.getModifiedBy() == null) {
                    return AFTER;
                }
                x = o1.getModifiedBy().nick.toLowerCase().compareTo(
                    o2.getModifiedBy().nick.toLowerCase());
                if (x == 0) {
                    return sortByFileName(o1, o2, false);
                }
                return x;
            case BY_MODIFIED_DATE :
                if (o1.getModifiedDate() == null
                    && o2.getModifiedDate() == null)
                {
                    return sortByFileName(o1, o2, false);
                } else if (o1.getModifiedDate() == null) {
                    return BEFORE;
                } else if (o2.getModifiedDate() == null) {
                    return AFTER;
                }
                x = o2.getModifiedDate().compareTo(o1.getModifiedDate());
                if (x == 0) {
                    return sortByFileName(o1, o2, false);
                }
                return x;
            case BY_FOLDER :
                if (o1.getFolderInfo() == null && o2.getFolderInfo() == null) {
                    return sortByFileName(o1, o2, false);
                } else if (o1.getFolderInfo() == null) {
                    return BEFORE;
                } else if (o2.getFolderInfo() == null) {
                    return AFTER;
                }
                x = o1.getFolderInfo().getName().compareToIgnoreCase(o2
                    .getFolderInfo().getName());
                if (x == 0) {
                    return sortByFileName(o1, o2, false);
                }
                return x;
            case BY_VERSION :
                if (o1.getFolderInfo() == null && o2.getFolderInfo() == null) {
                    return sortByFileName(o1, o2, false);
                } else if (o1.getFolderInfo() == null) {
                    return BEFORE;
                } else if (o2.getFolderInfo() == null) {
                    return AFTER;
                } else if (o1 instanceof DirectoryInfo
                    || o2 instanceof DirectoryInfo)
                {
                    return sortByFileName(o1, o2, false);
                } else {
                    x = o1.getVersion() - o2.getVersion();
                    if (x == 0) {
                        return sortByFileName(o1, o2, false);
                    }
                    return x;
                }
        }
        return 0;
    }

    private static int sortByFileName(DiskItem o1, DiskItem o2, boolean fullName)
    {

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
            case BY_FILE_TYPE :
                text = stub + "file type";
                break;
            case BY_NAME :
                text = stub + "name";
                break;
            case BY_SIZE :
                text = stub + "size";
                break;
            case BY_MEMBER :
                text = stub + "member";
                break;
            case BY_MODIFIED_DATE :
                text = stub + "modified date";
                break;
            default :
                text = "???";
        }
        return text;
    }
}
