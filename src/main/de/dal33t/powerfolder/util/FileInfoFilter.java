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
 * $Id: FileInfo.java 5858 2008-11-24 02:30:33Z tot $
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * <p>
 * A filter for FileInfos.
 * </p>
 * 
 * @author Dennis Waldherr
 */
public interface FileInfoFilter {
    /**
     * Tests wether or not the given FileInfo should be accepted.
     * 
     * @param fInfo
     * @return true if the fInfo should be accepted, false if it should be
     *         discarded.
     */
    boolean accept(FileInfo fInfo);
}
