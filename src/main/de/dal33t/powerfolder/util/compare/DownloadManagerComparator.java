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
 * $Id: DownloadManagerComparator.java 6404 2009-01-15 12:50:57Z harry $
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.transfer.DownloadManager;

public class DownloadManagerComparator implements Comparator<DownloadManager> {

    // All the available file comparators
    public static final int BY_EXT = 0;
    public static final int BY_FILE_NAME = 1;
    public static final int BY_PROGRESS = 2;
    public static final int BY_SIZE = 3;
    public static final int BY_FOLDER = 4;
    public static final int BY_MEMBER = 5;
    public static final int BY_COMPLETED_DATE = 6;

    private int sortBy;

    public DownloadManagerComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public int compare(DownloadManager o1, DownloadManager o2) {

        switch (sortBy) {
            case BY_EXT :
                return o1.getFileInfo().getExtension()
                    .compareTo(o2.getFileInfo().getExtension());
            case BY_FILE_NAME :
                return o1.getFileInfo().getFilenameOnly()
                    .compareToIgnoreCase(o2.getFileInfo().getFilenameOnly());
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
                long s1 = o1.getFileInfo().getSize();
                long s2 = o2.getFileInfo().getSize();
                if (s1 == s2) {
                    return 0;
                } else {
                    return s1 - s2 > 0 ? 1 : -1;
                }
            case BY_FOLDER :
                return o1.getFileInfo().getFolderInfo().getName().compareTo(o2
                    .getFileInfo().getFolderInfo().getName());
            case BY_MEMBER :
                if (o1.getSources().size() > 1 && o1.getSources().size() > 1) {
                    return 0;
                } else if (o1.getSources().size() == 1
                    && o2.getSources().size() == 1)
                {
                    return o1
                        .getSources()
                        .iterator()
                        .next()
                        .getPartner()
                        .getNick()
                        .compareTo(
                            o2.getSources().iterator().next().getPartner()
                                .getNick());
                } else {
                    return o1.getSources().size();
                }
            case BY_COMPLETED_DATE :
                if (o1.getCompletedDate() == null) {
                    if (o2.getCompletedDate() == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2.getCompletedDate() == null) {
                    return 1;
                }
                return o1.getCompletedDate().compareTo(o2.getCompletedDate());
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
