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

import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Checksum;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Validate;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 4280 $
 */
public final class FilePartsRecordBuilder {
	private final Checksum chksum;
	private final MessageDigest partDigester, fileDigester;
	private final int partSize;
    private List<PartInfo> parts = new LinkedList<PartInfo>();
    private long processed;
    private int partPos;


    public FilePartsRecordBuilder(Checksum chksumRoller, MessageDigest partDigester, MessageDigest fileDigester,
        int partSize) {
        super();
        Reject.noNullElements(chksumRoller, partDigester, fileDigester);

        this.chksum = chksumRoller;
        this.partDigester = partDigester;
        this.fileDigester = fileDigester;
        this.partSize = partSize;
    }

	/**
     * Updates the current record with the given data.
	 * @param data
	 * @param off
	 * @param len
	 */
	public void update(byte[] data, int off, int len) {
	    Validate.notNull(data);
	    if (off < 0 || len < 0 || off + len > data.length) {
	        throw new IndexOutOfBoundsException("Invalid parameters!");
	    }
	    processed += len;
	    fileDigester.update(data, off, len);

	    while (len > 0) {
	        if (partPos + len >= partSize) {
	            int rem = partSize - partPos;
	            chksum.update(data, off, rem);
	            partDigester.update(data, off, rem);
	            parts.add(new PartInfo(parts.size(), chksum.getValue(), partDigester.digest()));
	            // Only the checksum needs to be reset, since getValue() doesn't do that.
	            chksum.reset();
	            off += rem;
	            len -= rem;
	            partPos = 0;
	        } else {
//	            System.err.println(len + " " + (data[off] & 0xff));
	            chksum.update(data, off, len);
	            partDigester.update(data, off, len);
	            partPos += len;
	            len = 0;
	        }
	    }
	}

	/**
     * Updates the current record with the given data.
	 * @param data
	 */
	public void update(int data) {
	   update(new byte[] { (byte) (data & 0xFF) });
	}

	/**
	 * Updates the current record with the given data.
	 * Same as calling update(data, 0, data.length).
	 * @param data the data to update with.
	 */
	public void update(byte[] data) {
	    update(data, 0, data.length);
	}

	/**
	 * Performs final operations, such as padding and returns the resulting set.
     * The builder is reset after this call is made.
	 * @return a record containing {@link PartInfo}s and additional information.
	 */
	public FilePartsRecord getRecord() {
	    try {
	        // Finalize result
	        if (partPos > 0) {
//	            System.err.println(partSize - partPos);
	            for (int i = 0; i < partSize - partPos; i++) {
	                chksum.update(0);
	                partDigester.update((byte) 0);
	            }
	            parts.add(new PartInfo(parts.size(), chksum.getValue(), partDigester.digest()));
	        }
	        return new FilePartsRecord(processed, parts.toArray(new PartInfo[0]), partSize, fileDigester.digest());
	    } finally {
	        reset();
	    }
	}

	/**
	 * Resets the builder for further use.
	 */
	public void reset() {
        processed = 0;
        partPos = 0;
        chksum.reset();
        fileDigester.reset();
        partDigester.reset();
        parts.clear();
    }
}
