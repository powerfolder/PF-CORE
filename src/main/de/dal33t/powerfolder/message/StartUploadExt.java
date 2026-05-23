/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;

/**
 * Message to indicate that the upload can be started. This message is sent by
 * the uploader. The remote side should send PartRequests or PartinfoRequests.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class StartUploadExt extends StartUpload implements Externalizable {
    private static final long serialVersionUID = 2241562195221726484L;
    
    public StartUploadExt() {
        super();
    }

    public StartUploadExt(FileInfo fInfo) {
        super(fInfo);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        fileInfo = FileInfoFactory.readExt(in);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        fileInfo.writeExternal(out);
    }
}
