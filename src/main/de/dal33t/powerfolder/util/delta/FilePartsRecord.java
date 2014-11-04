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

/**
 * Holds the info of one set of PartInfos.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public final class FilePartsRecord implements Serializable {
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
}
