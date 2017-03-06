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

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.UploadStartProto;

/**
 * Message to indicate that the upload can be started. This message is sent by
 * the uploader. The remote side should send PartRequests or PartinfoRequests.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class StartUpload extends Message implements D2DObject {
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

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof UploadStartProto.UploadStart) {
            UploadStartProto.UploadStart proto = (UploadStartProto.UploadStart)mesg;
            this.fileInfo = new FileInfo(proto.getFileInfo());
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        UploadStartProto.UploadStart.Builder builder = UploadStartProto.UploadStart.newBuilder();

        // Translate old message name to new name defined in protocol file
        builder.setClazzName("UploadStart");
        builder.setFileInfo((FileInfoProto.FileInfo) this.fileInfo.toD2D());
        
        return builder.build();
    }
}
