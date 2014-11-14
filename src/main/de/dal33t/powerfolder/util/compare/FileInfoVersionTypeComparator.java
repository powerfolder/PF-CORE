/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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

import de.dal33t.powerfolder.ui.information.folder.files.versions.FileInfoVersionTypeHolder;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator for FileInfoVersionTypeHolder
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class FileInfoVersionTypeComparator extends Loggable implements
        Comparator<FileInfoVersionTypeHolder> {

    // All the available file comparators
    public static final int BY_VERSION_TYPE = 0;
    public static final int BY_VERSION = 1;
    public static final int BY_SIZE = 2;
    public static final int BY_MODIFIED_DATE = 3;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private int sortBy;
    private static final FileInfoVersionTypeComparator[] comparators;

    static {
        comparators = new FileInfoVersionTypeComparator[4];
        comparators[BY_VERSION_TYPE] = new FileInfoVersionTypeComparator(BY_VERSION_TYPE);
        comparators[BY_VERSION] = new FileInfoVersionTypeComparator(BY_VERSION);
        comparators[BY_SIZE] = new FileInfoVersionTypeComparator(BY_SIZE);
        comparators[BY_MODIFIED_DATE] = new FileInfoVersionTypeComparator(BY_MODIFIED_DATE);
    }

    public FileInfoVersionTypeComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public static FileInfoVersionTypeComparator getComparator(int sortByArg) {
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
    public int compare(FileInfoVersionTypeHolder o1, FileInfoVersionTypeHolder o2) {

        switch (sortBy) {
            case BY_VERSION_TYPE:
                if (o1.isOnline() && o2.isOnline()) {
                    return 0;
                } else if (!o1.isOnline() && !o2.isOnline()) {
                    return 0;
                } else {
                    return o1.isOnline() ? 1 : -1;
                }
            case BY_VERSION:
                if (o1.getFileInfo().getFolderInfo() == null
                        && o2.getFileInfo().getFolderInfo() == null) {
                    return 0;
                } else if (o1.getFileInfo().getFolderInfo() == null) {
                    return BEFORE;
                } else if (o2.getFileInfo().getFolderInfo() == null) {
                    return AFTER;
                } else {
                    return o1.getFileInfo().getVersion() - o2.getFileInfo().getVersion();
                }
            case BY_SIZE:

                if (o1.getFileInfo().getSize() < o2.getFileInfo().getSize()) {
                    return BEFORE;
                }
                if (o1.getFileInfo().getSize() > o2.getFileInfo().getSize()) {
                    return AFTER;
                }
                return 0;
            case BY_MODIFIED_DATE:
                if (o1.getFileInfo().getModifiedDate() == null
                        && o2.getFileInfo().getModifiedDate() == null) {
                    return 0;
                } else if (o1.getFileInfo().getModifiedDate() == null) {
                    return BEFORE;
                } else if (o2.getFileInfo().getModifiedDate() == null) {
                    return AFTER;
                }
                return o2.getFileInfo().getModifiedDate().compareTo(
                        o1.getFileInfo().getModifiedDate());
        }
        return 0;
    }
}