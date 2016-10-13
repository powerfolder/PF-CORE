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
package de.dal33t.powerfolder.util.delta;

import java.io.Serializable;
import java.util.Arrays;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.FilePartsRecordProto;
import de.dal33t.powerfolder.protocol.PartInfoProto;

/**
 * Holds the info of one set of PartInfos.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public final class FilePartsRecord implements Serializable, D2DObject {
	private static final long serialVersionUID = 1L;

	private PartInfo[] infos;
	private int partLength;
	private long fileLength;
	private byte[] fileDigest;

    /**
     * Creates a new record with the given parameters.
     * @param fileLength the size of the file that was used.
     * @param infos the {@link PartInfo}s that were produced.
     * @param partSize the size of one part of the infos.
     * @param fileDigest the digest of the complete file.
     */
    public FilePartsRecord(long fileLength, PartInfo[] infos, int partSize,
        byte[] fileDigest)
    {
        partLength = partSize;
        this.infos = infos;
        this.fileDigest = fileDigest;
        this.fileLength = fileLength;
    }

    /**
     * Returns the {@link PartInfo}s this record contains.
     * @return
     */
    public PartInfo[] getInfos() {
        return infos;
    }

	/**
	 * Returns the size of one part of the infos.
	 * @return
	 */
	public int getPartLength() {
		return partLength;
	}

	/**
	 * Returns the digest of the complete file represented by this record.
	 * @return
	 */
	public byte[] getFileDigest() {
		return fileDigest;
	}

	/**
	 * Returns the size of the file used to create this record.
	 * @return
	 */
	public long getFileLength() {
		return fileLength;
	}

    @Override
    public boolean equals(Object arg0) {
        if (arg0.getClass() != FilePartsRecord.class) {
            return false;
        }
        FilePartsRecord o = (FilePartsRecord) arg0;
        return partLength == o.partLength
            && Arrays.equals(infos, o.infos)
            && Arrays.equals(fileDigest, o.fileDigest)
            && fileLength == o.fileLength;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fileDigest);
    }

    @Override
    public String toString() {
        return "[FilePartsRecord, fsize: " + fileLength + ", partLength: " + partLength + ", infocount: " + infos.length + "]";
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
        if (mesg instanceof FilePartsRecordProto.FilePartsRecord) {
            FilePartsRecordProto.FilePartsRecord proto = 
                (FilePartsRecordProto.FilePartsRecord) mesg;

            /* Convert list back to array */
            int i = 0;

            this.infos = new PartInfo[proto.getPartInfosCount()];

            for(PartInfoProto.PartInfo pinfo : proto.getPartInfosList()) {
                this.infos[i++] = new PartInfo(pinfo);
              }

            this.partLength = proto.getPartLength();
            this.fileLength = proto.getFileLength();
            this.fileDigest = proto.getFileDigest().toByteArray();
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
        FilePartsRecordProto.FilePartsRecord.Builder builder =
            FilePartsRecordProto.FilePartsRecord.newBuilder();

        builder.setClazzName(this.getClass().getSimpleName());

        /* Convert array to list */
        if (null != this.infos) {
            for(PartInfo pinfo : this.infos) {
               builder.addPartInfos((PartInfoProto.PartInfo)pinfo.toD2D());
            }
         }

        builder.setPartLength(this.partLength);
        builder.setFileLength(this.fileLength);
        builder.setFileDigest(ByteString.copyFrom(this.fileDigest));

        return builder.build();
    }
}
