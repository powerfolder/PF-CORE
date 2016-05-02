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
import de.dal33t.powerfolder.protocol.RequestFilePartsRecordProto;

/**
 * Requests a FilePartsRecord for a given file.
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class RequestFilePartsRecord extends Message
  implements D2DObject
{
	private static final long serialVersionUID = 100L;

	private FileInfo file;

	public RequestFilePartsRecord() {
	}

	public RequestFilePartsRecord(FileInfo file) {
		super();
		this.file = file;
	}

	public FileInfo getFile() {
		return file;
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
      if(mesg instanceof RequestFilePartsRecordProto.RequestFilePartsRecord)
        {
          RequestFilePartsRecordProto.RequestFilePartsRecord proto =
            (RequestFilePartsRecordProto.RequestFilePartsRecord)mesg;

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
    toD2D()
    {
      RequestFilePartsRecordProto.RequestFilePartsRecord.Builder builder =
        RequestFilePartsRecordProto.RequestFilePartsRecord.newBuilder();

      builder.setClazzName("RequestFilePartsRecord");
      builder.setFile((FileInfoProto.FileInfo)this.file.toD2D());

      return builder.build();
    }
}
