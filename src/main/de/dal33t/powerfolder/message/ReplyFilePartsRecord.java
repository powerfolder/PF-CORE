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
import de.dal33t.powerfolder.protocol.FilePartInfoListReplyProto;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

import java.io.IOException;

/**
 * Reply to a RequestFilePartsRecord message.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class ReplyFilePartsRecord extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private FileInfo file;
    private FilePartsRecord record;

    public ReplyFilePartsRecord() {
    }

    public ReplyFilePartsRecord(FileInfo file, FilePartsRecord record) {
        super();
        this.file = file;
        this.record = record;

        validate();
    }

    public FileInfo getFile() {
        return file;
    }

    public FilePartsRecord getRecord() {
        return record;
    }

    private void validate() {
        Reject.noNullElements(file, record);
    }

    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        validate();
    }
    
    /**
     * Init from D2D message
     * 
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param mesg
     *            Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof FilePartInfoListReplyProto.FilePartInfoListReply) {
            FilePartInfoListReplyProto.FilePartInfoListReply proto =
                (FilePartInfoListReplyProto.FilePartInfoListReply) mesg;

            this.file = new FileInfo(proto.getFileInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        FilePartInfoListReplyProto.FilePartInfoListReply.Builder builder =
            FilePartInfoListReplyProto.FilePartInfoListReply.newBuilder();

        // Translate old message name to new name defined in protocol file
        builder.setClazzName("FilePartInfoListReply");
        builder.setFileInfo((FileInfoProto.FileInfo) this.file.toD2D());

        return builder.build();
    }
}
