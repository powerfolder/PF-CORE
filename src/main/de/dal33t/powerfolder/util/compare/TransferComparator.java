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

import de.dal33t.powerfolder.transfer.Transfer;

import java.util.Comparator;

/**
 * Comparator for FileInfo
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.15 $
 */
public class TransferComparator implements Comparator<Transfer> {

    // All the available file comparators
    public static final int BY_EXT = 0;
    public static final int BY_FILE_NAME = 1;
    public static final int BY_PROGRESS = 2;
    public static final int BY_SIZE = 3;
    public static final int BY_FOLDER = 4;
    public static final int BY_MEMBER = 5;

    private int sortBy;

    public TransferComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public int compare(Transfer o1, Transfer o2) {

        switch (sortBy) {
            case BY_EXT :
                return o1.getFile().getExtension().compareTo(
                    o2.getFile().getExtension());
            case BY_FILE_NAME :
                return o1.getFile().getFilenameOnly().compareToIgnoreCase(
                    o2.getFile().getFilenameOnly());
            case BY_PROGRESS :
                int comp = o1.getState().compareTo(o2.getState());
                if (comp == 0 && o1.getCompletedDate() != null
                    && o2.getCompletedDate() != null)
                {

                    return -o1.getCompletedDate().compareTo(
                        o2.getCompletedDate());
                }
                return comp;
            case BY_SIZE :
                long s1 = o1.getFile().getSize();
                long s2 = o2.getFile().getSize();
                if (s1 == s2) {
                    return 0;
                } else {
                    return s1 - s2 > 0 ? 1 : -1;
                }
            case BY_FOLDER :
                return o1.getFile().getFolderInfo().getName().compareTo(o2.getFile()
                    .getFolderInfo().getName());
            case BY_MEMBER :
                return o1.getPartner().getNick().compareTo(
                    o2.getPartner().getNick());
        }
        return 0;
    }

    // General ****************************************************************

    public String toString() {
        String text = "FileInfo comparator, sorting by ";
        switch (sortBy) {
            case BY_EXT :
                text += "extension";
                break;
            case BY_FILE_NAME :
                text += "extension";
                break;
            case BY_PROGRESS :
                text += "progress";
                break;
            case BY_SIZE :
                text += "size";
                break;
            case BY_FOLDER :
                text += "folder";
                break;
            case BY_MEMBER :
                text += "member";
                break;
        }
        return text;
    }
}
