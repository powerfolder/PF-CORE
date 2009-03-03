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

import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.FileInfoHolder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator for FileInfo
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.15 $
 */
public class DiskItemComparator extends Loggable implements
    Comparator<DiskItem>
{

    // All the available file comparators
    public static final int BY_FILE_TYPE = 0;
    public static final int BY_NAME = 1;
    public static final int BY_SIZE = 2;
    public static final int BY_MEMBER = 3;
    public static final int BY_MODIFIED_DATE = 4;
    public static final int BY_AVAILABILITY = 5;
    public static final int BY_FOLDER = 6;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private Directory directory;
    private int sortBy;
    private static final DiskItemComparator[] comparators;

    static {
        comparators = new DiskItemComparator[7];
        comparators[BY_FILE_TYPE] = new DiskItemComparator(BY_FILE_TYPE);
        comparators[BY_NAME] = new DiskItemComparator(BY_NAME);
        comparators[BY_SIZE] = new DiskItemComparator(BY_SIZE);
        comparators[BY_MEMBER] = new DiskItemComparator(BY_MEMBER);
        comparators[BY_MODIFIED_DATE] = new DiskItemComparator(BY_MODIFIED_DATE);
        comparators[BY_AVAILABILITY] = new DiskItemComparator(BY_AVAILABILITY);
        comparators[BY_FOLDER] = new DiskItemComparator(BY_FOLDER);
    }

    public DiskItemComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public DiskItemComparator(int sortBy, Directory directory) {
        this.sortBy = sortBy;
        this.directory = directory;
    }

    public static DiskItemComparator getComparator(int sortByArg) {
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
    public int compare(DiskItem o1, DiskItem o2) {

        switch (sortBy) {
            case BY_FILE_TYPE:
                String ext1 = o1.getExtension();
                String ext2 = o2.getExtension();
                if (ext1 == null || ext2 == null) {
                    return sortByFileName(o1, o2);
                }
                int x = ext1.compareTo(ext2);
                if (x == 0) {
                    return sortByFileName(o1, o2);
                }
                return x;
            case BY_NAME :
                return sortByFileName(o1, o2);
            case BY_SIZE :

                if (o1.getSize() < o2.getSize()) {
                    return BEFORE;
                }
                if (o1.getSize() > o2.getSize()) {
                    return AFTER;
                }
                return sortByFileName(o1, o2);
            case BY_MEMBER :
                if (o1.getModifiedBy() == null && o2.getModifiedBy() == null) {
                    return sortByFileName(o1, o2);
                } else if (o1.getModifiedBy() == null) {
                    return BEFORE;
                } else if (o2.getModifiedBy() == null) {
                    return AFTER;
                }
                x = o1.getModifiedBy().nick.toLowerCase().compareTo(
                    o2.getModifiedBy().nick.toLowerCase());
                if (x == 0) {
                    return sortByFileName(o1, o2);
                }
                return x;
            case BY_MODIFIED_DATE :
                if (o1.getModifiedDate() == null
                    && o2.getModifiedDate() == null)
                {
                    return sortByFileName(o1, o2);
                } else if (o1.getModifiedDate() == null) {
                    return BEFORE;
                } else if (o2.getModifiedDate() == null) {
                    return AFTER;
                }
                x = o2.getModifiedDate().compareTo(o1.getModifiedDate());
                if (x == 0) {
                    return sortByFileName(o1, o2);
                }
                return x;
            case BY_AVAILABILITY :
                if (o1 instanceof Directory && o2 instanceof Directory) {
                    return sortByFileName(o1, o2);
                } else if (o1 instanceof Directory) {
                    return BEFORE;
                } else if (o2 instanceof Directory) {
                    return AFTER;
                }

                if (directory == null) {
                    throw new IllegalStateException(
                        "need a directoy to compare by BY_AVAILABILITY");
                }

                FileInfoHolder holder1 = directory
                    .getFileInfoHolder((FileInfo) o1);
                FileInfoHolder holder2 = directory
                    .getFileInfoHolder((FileInfo) o2);
                if (holder1 != null && holder2 != null) {
                    int av1 = holder1.getAvailability();
                    int av2 = holder2.getAvailability();
                    if (av1 == av2) {
                        return sortByFileName(o1, o2);
                    }
                    if (av1 < av2) {
                        return BEFORE;
                    }
                    return AFTER;
                }
                return sortByFileName(o1, o2);
            case BY_FOLDER :
                if (o1.getFolderInfo() == null && o2.getFolderInfo() == null) {
                    return sortByFileName(o1, o2);
                } else if (o1.getFolderInfo() == null) {
                    return BEFORE;
                } else if (o2.getFolderInfo() == null) {
                    return AFTER;
                }
                x = o1.getFolderInfo().name.compareToIgnoreCase(o2
                    .getFolderInfo().name);
                if (x == 0) {
                    return sortByFileName(o1, o2);
                }
                return x;
        }
        return 0;
    }

    private static int sortByFileName(DiskItem o1, DiskItem o2) {
        return o1.getFilenameOnly().compareToIgnoreCase(o2.getFilenameOnly());
    }

    // General ****************************************************************

    public String toString() {
        String stub = "FileInfo comparator, sorting by ";
        String text;
        switch (sortBy) {
            case BY_FILE_TYPE:
                text = stub + "file type";
                break;
            case BY_NAME :
                text = stub + "name";
                break;
            case BY_SIZE :
                text = stub +  "size";
                break;
            case BY_MEMBER :
                text = stub +  "member";
                break;
            case BY_MODIFIED_DATE :
                text = stub +  "modified date";
                break;
            case BY_AVAILABILITY :
                text = stub +  "availability";
                break;
            default:
                text = "???";
        }
        return text;
    }
}
