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
package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.FilePartReplyProto;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Validate;

import java.io.IOException;

/**
 * A file chunk, part of a upload / donwload
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FileChunk extends Message
  implements LimitBandwidth, D2DObject
{
    private static final long serialVersionUID = 100L;

    public FileInfo file;
    public long offset;
    public byte[] data;

    public FileChunk() {
        // Serialisation constructor
    }

    public FileChunk(FileInfo file, long offset, byte[] data) {
        this.file = file;
        this.offset = offset;
        this.data = data;
        validate();
    }

    @Override
    public String toString() {
        return "FileChunk: " + file + " ("
            + Format.formatDecimal(file.getSize()) + " total bytes), offset: "
            + offset + ", chunk size: " + data.length;
    }

    // Overridden due to validation!
    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        validate();
    }

    /**
     *
     */
    private void validate() {
        Reject.noNullElements(file, data);
        Validate.isTrue(offset >= 0);
        Validate.isTrue(offset + data.length <= file.getSize());
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FilePartReplyProto.FilePartReply)
        {
          FilePartReplyProto.FilePartReply proto = (FilePartReplyProto.FilePartReply)mesg;

          this.file   = new FileInfo(proto.getFileInfo());
          this.offset = proto.getOffset();
          this.data   = proto.getData().toByteArray();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FilePartReplyProto.FilePartReply.Builder builder = FilePartReplyProto.FilePartReply.newBuilder();

      // Translate old message name to new name defined in protocol file
      builder.setClazzName("FilePartReply");
      builder.setFileInfo((FileInfoProto.FileInfo)this.file.toD2D());
      builder.setOffset(this.offset);
      builder.setData(ByteString.copyFrom(this.data));

      return builder.build();
    }
}