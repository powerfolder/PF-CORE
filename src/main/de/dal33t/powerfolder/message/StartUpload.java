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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Message to indicate that the upload can be started. This message is sent by
 * the uploader. The remote side should send PartRequests or PartinfoRequests.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class StartUpload extends Message {
    private static final long serialVersionUID = 100L;
    protected FileInfo fileInfo;

    public StartUpload() {
    }

    public StartUpload(FileInfo fInfo) {
        fileInfo = fInfo;
    }

    public FileInfo getFile() {
        return fileInfo;
    }

    public String toString() {
        return "StartUpload of " + fileInfo.toDetailString();
    }
}
