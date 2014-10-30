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
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Comparator which sorts folders.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FolderComparator extends Loggable implements Comparator<Folder> {

    public static final FolderComparator INSTANCE = new FolderComparator();

    private FolderComparator() {
    }

    public int compare(Folder o1, Folder o2) {
        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
    }
}