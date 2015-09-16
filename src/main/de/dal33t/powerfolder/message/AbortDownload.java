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
import de.dal33t.powerfolder.protocol.AbortDownloadProto;
import de.dal33t.powerfolder.protocol.PingProto;

/**
 * Message to indicate that the download was aborted. The remote side should stop the upload.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class AbortDownload extends Message
    implements D2DMessage
{
    private static final long serialVersionUID = 100L;

    public FileInfo file;

    public AbortDownload() {
      /* Empty constructor for D2D */
    }

    public AbortDownload(FileInfo file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "Abort download of: " + file;
    }

    /** initFromD2DMessage
     * Init message from D2D message
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2DMessage(AbstractMessage mesg)
    {
      if(mesg instanceof AbortDownloadProto.AbortDownload)
        {
          AbortDownloadProto.AbortDownload abort = (AbortDownloadProto.AbortDownload)mesg;
        }
    }

    /** toD2DMessage
     * Convert message to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2DMessage()
    {
      AbortDownloadProto.AbortDownload.Builder builder = AbortDownloadProto.AbortDownload.newBuilder();

      builder.setClassName("AbortDownload");
      builder.setTransferId(value)

      return builder.build();
    }
}