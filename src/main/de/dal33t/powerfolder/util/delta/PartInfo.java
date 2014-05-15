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
 * Info for a frame of bytes.
 * A partinfo contains only enough information to check for matches and reconstruct
 * the location in a file.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public final class PartInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private long index;
	private long checksum;
	private byte[] digest;

	public PartInfo(long index, long checksum, byte[] digest) {
		super();
		this.index = index;
		this.checksum = checksum;
		this.digest = digest;
	}
	/**
	 * Returns the checksum calculated for this part.
	 * @return
	 */
	public long getChecksum() {
		return checksum;
	}
	/**
	 * Returns the message digest calculated for this part.
	 * @return
	 */
	public byte[] getDigest() {
		return digest;
	}
	/**
	 * Returns the index of this part.
	 * @return
	 */
	public long getIndex() {
		return index;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			b.append('-').append(Integer.toHexString(digest[i] & 0xff));
		}
		return "{" + getIndex() + ": " + getChecksum() + ", '" + b.toString() + "'}";
	}

	@Override
	public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
		if (obj instanceof PartInfo) {
			PartInfo pi = (PartInfo) obj;
			return index == pi.index
				&& checksum == pi.checksum
				&& Arrays.equals(digest, pi.digest);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) index ^ (int) (index >> 32) ^ (int) checksum ^ (int) (checksum >> 32) ^ Arrays.hashCode(digest);
	}
}
