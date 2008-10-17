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

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;


/**
 * Comparator which sorts folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FolderComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        int value = 0;
        String name1;
        String name2;

        if (o1 instanceof Folder) {
            Folder f1 = (Folder) o1;
            value -= 2000;
            name1 = f1.getName();
        } else if (o1 instanceof FolderInfo) {
            value -= 500;
            name1 = ((FolderInfo) o1).name;
        } else {
            throw new IllegalArgumentException(
                "Only Folder, FolderInfo or FolderDetails as argument allowed");
        }

        if (o2 instanceof Folder) {
            Folder f2 = (Folder) o2;
            value += 2000;
            name2 = f2.getName();
        } else if (o2 instanceof FolderInfo) {
            value += 500;
            name2 = ((FolderInfo) o2).name;
        } else {
            throw new IllegalArgumentException(
                "Only Folder, FolderInfo or FolderDetails as argument allowed");
        }

        // now add name
        int nameComp = name1.toLowerCase().compareTo(name2.toLowerCase());
        if (nameComp > 0) {
            nameComp = 1;
        } else if (nameComp < 0) {
            nameComp = -1;
        }

        value += nameComp;

        return value;
    }

}