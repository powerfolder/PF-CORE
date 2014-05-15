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
 * $Id: FileInfo.java 5858 2008-11-24 02:30:33Z tot $
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * File history storage container. Classes implementing this are able to provide
 * more or less file history metadata information and conflict detection.
 * <p>
 * http://dev.powerfolder.com/projects/powerfolder/wiki/Versioning
 * <P>
 * TRAC #388
 * <P>
 * TODO Add required methods.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface FileHistoryService {
    /**
     * Adds a new version to the file history metadata storage. Creates a new FileHistory
     * #
     *
     * @param newFileVersion
     */
    void add(FileInfo newFileVersion);

    /**
     * @param fileInfo
     * @return the file history for this file.
     */
    FileHistory findFileHistory(FileInfo fileInfo);
}
