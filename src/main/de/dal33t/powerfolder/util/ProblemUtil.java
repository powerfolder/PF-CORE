/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;

public final class ProblemUtil {

    /**
     * @param info
     * @param oldSource
     * @return true, if the interrupted action should continue or not
     */
    public static boolean resolveNoFileHistorySupport(Folder folder,
        FileInfo info, Member oldSource)
    {
        // TODO Well no resolution support here currently so...
        return true;
    }

}
