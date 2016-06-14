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
 * $Id: FolderComparator.java 6297 2009-01-02 04:18:58Z tot $
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator which sorts folder infos.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FolderInfoComparator extends Loggable implements
    Comparator<FolderInfo>
{

    public static final FolderInfoComparator INSTANCE = new FolderInfoComparator();

    private FolderInfoComparator() {
    }

    public int compare(FolderInfo o1, FolderInfo o2) {
        if (o1.getName() == null) {
            return -1;
        }
        if (o2.getName() == null) {
            return 1;
        }
        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
    }
}