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
package de.dal33t.powerfolder.light;

import java.util.Date;

/**
 * Common interface for FileInfo and Directory.
 */
public interface DiskItem {
    String getExtension();

    /**
     * @return the name, relative to the folder base.
     */
    String getRelativeName();

    /**
     * @return the name of the item only excluding any path information.
     */
    String getFilenameOnly();

    String getLowerCaseFilenameOnly();

    long getSize();

    MemberInfo getModifiedBy();

    Date getModifiedDate();

    FolderInfo getFolderInfo();

    /**
     * @return true if this item is directory, false if is a file.
     */
    boolean isDiretory();

    /**
     * @return true if this item is file, false if is a directory.
     */
    boolean isFile();
}
