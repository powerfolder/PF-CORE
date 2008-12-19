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

import java.io.File;
import java.util.Comparator;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator for FileInfo
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.15 $
 */
public class RecycleBinComparator extends Loggable implements Comparator<FileInfo> {

    // All the available file comparators
    public static final int BY_FILETYPE = 0;
    public static final int BY_NAME = 1;
    public static final int BY_SIZE = 2;
    public static final int BY_MODIFIED_DATE = 3;
    public static final int BY_FOLDER = 4;

    private static final int BEFORE = -1;
    private static final int EQUAL = 0;
    private static final int AFTER = 1;

    private int sortBy;
    private static DiskItemComparator[] comparators;
    private RecycleBin recycleBin;

    static {
        comparators = new DiskItemComparator[5];
        comparators[BY_FILETYPE] = new DiskItemComparator(
            BY_FILETYPE);
        comparators[BY_NAME] = new DiskItemComparator(
            BY_NAME);
        comparators[BY_SIZE] = new DiskItemComparator(
            BY_SIZE);
        comparators[BY_MODIFIED_DATE] = new DiskItemComparator(
            BY_MODIFIED_DATE);
        comparators[BY_FOLDER] = new DiskItemComparator(
            BY_FOLDER);
    }

    public RecycleBinComparator(int sortBy, RecycleBin recycleBin) {
        this.recycleBin = recycleBin;
        this.sortBy = sortBy;
    }

    public static DiskItemComparator getComparator(int sortByArg) {
        return comparators[sortByArg];
    }

    public int compare(FileInfo o1, FileInfo o2) {

            switch (sortBy) {
            case BY_FILETYPE :
                String ext1 = o1.getExtension();
                String ext2 = o2.getExtension();
                if (ext1 == null || ext2 == null) {
                    return EQUAL;
                }
                return ext1.compareTo(ext2);
            case BY_NAME :
                return o1.getName().compareToIgnoreCase(o2.getName());
            case BY_SIZE :
                File file1 = recycleBin.getDiskFile(o1);
                File file2 = recycleBin.getDiskFile(o2);
                    if (file1.length() < file2.length()) {
                        return BEFORE;
                    } else if (file1.length() > file2.length()) {
                        return AFTER;
                    } else {
                        return EQUAL;
                    }
                case BY_MODIFIED_DATE :
                    file1 = recycleBin.getDiskFile(o1);
                    file2 = recycleBin.getDiskFile(o2);
                    if (file1.lastModified() < file2.lastModified()) {
                        return BEFORE;
                    } else if (file1.lastModified() > file2.lastModified()) {
                        return AFTER;
                    } else {
                        return EQUAL;
                    }
                case BY_FOLDER :
                   return o1.getFolderInfo().name.compareToIgnoreCase(
                        o2.getFolderInfo().name);
            }
        return 0;
    }

    // General ****************************************************************

    public String toString() {
        String text = "FileInfo comparator, sorting by ";
        switch (sortBy) {
            case BY_FILETYPE :
                text += "file type";
                break;
            case BY_NAME :
                text += "name";
                break;
            case BY_SIZE :
                text += "size";
                break;
            case BY_MODIFIED_DATE :
                text += "modified date";
                break;
        }
        return text;
    }
}