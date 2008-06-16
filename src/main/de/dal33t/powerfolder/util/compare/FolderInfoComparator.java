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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Comparator for Folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.8 $
 */
public class FolderInfoComparator extends PFComponent implements Comparator {
    // All the available folder comparators
    public final static int BY_NAME = 0;
    public final static int BY_NUMBER_OF_FILES = 1;
    public final static int BY_SIZE = 2;

    private static final int BEFORE = -1;
    private static final int EQUAL = 0;
    private static final int AFTER = 1;

    private int sortBy;

    public FolderInfoComparator(Controller controller, int sortBy) {
        super(controller);
        this.sortBy = sortBy;
    }

    public int compare(Object o1, Object o2) {

        if (o1 instanceof FolderInfo && o2 instanceof FolderInfo) {
            FolderInfo folder1 = (FolderInfo) o1;
            FolderInfo folder2 = (FolderInfo) o2;

            switch (sortBy) {
                case BY_NAME :
                    return folder1.name.toLowerCase().compareTo(
                        folder2.name.toLowerCase());
                case BY_NUMBER_OF_FILES :
                    if (folder1.filesCount < folder2.filesCount)
                        return BEFORE;
                    if (folder1.filesCount > folder2.filesCount)
                        return AFTER;
                    return EQUAL;
                case BY_SIZE :
                    if (folder1.bytesTotal < folder2.bytesTotal)
                        return BEFORE;
                    if (folder1.bytesTotal > folder2.bytesTotal)
                        return AFTER;
                    return EQUAL;
            }
        }
        return EQUAL;
    }

    // General ****************************************************************

    public String toString() {
        String text = "FileInfo comparator, sorting by ";
        switch (sortBy) {
            case BY_NAME :
                text += "name";
                break;
            case BY_NUMBER_OF_FILES :
                text += "number of files";
                break;
            case BY_SIZE :
                text += "size";
                break;
        }
        return text;
    }
}
