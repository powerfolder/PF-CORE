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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.protocol.AbortUploadProto;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message to indicate that the upload was aborted. The remote side should stop
 * the download.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class AbortUpload extends Message
    implements D2DMessage
{
    private static final long serialVersionUID = 100L;

    public FileInfo file;

    public AbortUpload(FileInfo file) {
        Reject.ifNull(file, "Fileinfo is null");
        this.file = file;
    }

    @Override
    public String toString() {
        return "Abort download of: " + file;
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2DMessage(AbstractMessage mesg)
    {
      if(mesg instanceof AbortUploadProto.AbortUpload)
        {
          AbortUploadProto.AbortUpload proto = (AbortUploadProto.AbortUpload)mesg;

          this.file = new FileInfo(proto.getFile());
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2DMessage()
    {
      AbortUploadProto.AbortUpload.Builder builder = AbortUploadProto.AbortUpload.newBuilder();

      builder.setClassName("AbortUpload");
      builder.setFile((FileInfoProto.FileInfo) this.file.toD2DMessage());

      return builder.build();
    }
}