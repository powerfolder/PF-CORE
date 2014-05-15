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
package de.dal33t.powerfolder.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple filter that counts the amount of data read through it.
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class CountedInputStream extends FilterInputStream {
	private long readBytes;

	public CountedInputStream(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		int res = super.read();
		if (res >= 0) {
			readBytes++;
		}
		return res;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int res = super.read(b, off, len);
		if (res > 0) {
			readBytes += res;
		}
		return res;
	}

	@Override
	public long skip(long n) throws IOException {
		long res = super.skip(n);
		readBytes += res;
		return res;
	}

	public long getReadBytes() {
		return readBytes;
	}
}
