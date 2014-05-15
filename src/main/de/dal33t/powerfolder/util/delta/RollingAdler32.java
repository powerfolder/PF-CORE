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

import de.dal33t.powerfolder.util.RingBuffer;


/**
 * Adler32 implementation which supports rolling over data.
 * Although there is a java Adler32 implementation (even done in native code), it lacks
 * the ability to "roll" over data making it much slower when continuous data has to be
 * processed.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 4280 $
 */
public final class RollingAdler32 implements RollingChecksum {
	private final static int MOD_ADLER = 65521;
	private final RingBuffer rbuf;
	private final int n;

	/** Adler32 specific */
	private int A = 1, B;

	public RollingAdler32(int n) {
		rbuf = new RingBuffer(n);
		this.n = n;
	}

	public void update(int nd) {
		int fb = 0;
		nd &= 0xff; // This allows update to be called with bytes directly

		if (rbuf.remaining() == 0) {
			fb = rbuf.read();
			B--;
		}

		rbuf.write(nd);

		A = A + nd - fb;
		if (A < 0) {
			A += MOD_ADLER;
		} else if (A >= MOD_ADLER) {
			A -= MOD_ADLER;
		}
		B = (B + A - n * fb) % MOD_ADLER;
		if (B < 0) {
			B += MOD_ADLER;
		}
	}

	public void update(byte[] data) {
		update(data, 0, data.length);
	}

	public void reset() {
		A = 1;
		B = 0;
		rbuf.reset();
	}

	public void update(byte[] data, int ofs, int len) {
	    if (ofs < 0) {
	        throw new IndexOutOfBoundsException("Offset is negative");
	    }
	    if (len < 0) {
            throw new IndexOutOfBoundsException("Length is negative");
	    }
	    if (ofs + len > data.length) {
            throw new IndexOutOfBoundsException("Offset + length too large!");
	    }
		for (; len > 0; len--) {
			update(data[ofs++]);
		}
	}

	public int getFrameSize() {
		return n;
	}

	public long getValue() {
		return ((long) B << 16) | A;
	}
}
