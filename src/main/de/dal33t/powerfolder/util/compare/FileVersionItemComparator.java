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

import de.dal33t.powerfolder.disk.FileVersionInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator for FileVersionInfo
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4 $
 */
public class FileVersionItemComparator extends Loggable implements
    Comparator<FileVersionInfo> {

    // All the available file comparators
    public static final int BY_VERSION = 0;
    public static final int BY_SIZE = 1;
    public static final int BY_DATE = 2;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private int sortBy;
    private static final FileVersionItemComparator[] COMPARATORS;

    static {
        COMPARATORS = new FileVersionItemComparator[3];
        COMPARATORS[BY_VERSION] = new FileVersionItemComparator(BY_VERSION);
        COMPARATORS[BY_SIZE] = new FileVersionItemComparator(BY_SIZE);
        COMPARATORS[BY_DATE] = new FileVersionItemComparator(BY_DATE);
    }

    private FileVersionItemComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public static FileVersionItemComparator getComparator(int sortByArg) {
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
    public int compare(FileVersionInfo o1, FileVersionInfo o2) {

        switch (sortBy) {
            case BY_VERSION :
                return o1.getVersion() - o2.getVersion();
            case BY_SIZE :
                return (int) (o1.getSize() - o2.getSize());
            case BY_DATE :
                if (o1.getCreated() == null
                    && o2.getCreated() == null)
                {
                    return 0;
                } else if (o1.getCreated() == null) {
                    return BEFORE;
                } else if (o2.getCreated() == null) {
                    return AFTER;
                }
                return o2.getCreated().compareTo(o1.getCreated());
        }
        return 0;
    }
}