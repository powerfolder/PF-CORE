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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Implementation of a ringbuffer.
 * This class represents a ringbuffer of bytes. It's not using a generic type due to
 * performance reasons.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public final class RingBuffer {
	private final byte[] data;
	private int rpos, wlen;

	public RingBuffer(int size) {
		data = new byte[size];
	}

	/**
	 * Writes the given byte to the buffer.
	 * @param b the value (only the byte part will be used)
	 */
	public void write(int b) {
		if (wlen >= data.length) {
			throw new BufferOverflowException();
		}
		data[(rpos + wlen) % data.length] = (byte) b;
		wlen++;
	}

	/**
	 * Writes from the given array of bytes to the buffer.
	 * @param b the array
	 * @param ofs the offset in the array to start from
	 * @param len the number of bytes to write
	 */
	public void write(byte[] b, int ofs, int len) {
		if (len + wlen > data.length) {
			throw new BufferOverflowException();
		}
		while (len > 0) {
			int wpos = (rpos + wlen) % data.length;
			int rem = Math.min(data.length - wpos, len);
			System.arraycopy(b, ofs, data, wpos, rem);
			ofs += rem;
			wlen += rem;
			len -= rem;
		}
	}

	/**
	 * Writes the values of the given array into the buffer.
	 * Same as write(b, 0, b.length);
	 * @param b
	 */
	public void write(byte[] b) {
		write(b, 0, b.length);
	}

	/**
	 * Reads one byte from the buffer.
	 * @return
	 */
	public int read() {
		if (wlen <= 0) {
			throw new BufferUnderflowException();
		}
		final int val = data[rpos] & 0xff;
		rpos = (rpos + 1) % data.length;
		wlen--;
		return val;
	}

	/**
	 * Skips the given amount of bytes
	 * @param n
	 */
	public void skip(int n) {
	    if (n > wlen) {
            throw new BufferUnderflowException();
	    }
	    rpos = (rpos + n) % data.length;
	    wlen -= n;
	}

	/**
	 * Reads one byte from the buffer without removing it.
	 * @return
	 */
	public int peek() {
		if (wlen <= 0) {
			throw new BufferUnderflowException();
		}
		return data[rpos] & 0xff;
	}

	/**
	 * Reads values from the buffer into an array.
	 * @param target the array to store into
	 * @param ofs the start of the storge in the array
	 * @param len the number of bytes to read
	 */
	public void read(byte[] target, int ofs, int len) {
		if (len > wlen) {
			throw new BufferUnderflowException();
		}
		while (len > 0) {
			int wpos = (rpos + wlen) % data.length;
			int rem = Math.min(data.length - wpos, len);
			System.arraycopy(data, wpos, target, ofs, rem);
			ofs += rem;
			wlen -= rem;
			rpos = (rpos + rem) % data.length;
			len -= rem;
		}
	}

	/**
	 * Reads values from the buffer into an array, without removing them from the buffer.
	 * @param target
	 * @param ofs
	 * @param len
	 */
	public void peek(byte[] target, int ofs, int len) {
		if (len > wlen) {
			throw new BufferUnderflowException();
		}
		int wpos = rpos;
		while (len > 0) {
			int rem = Math.min(data.length - wpos, len);
			System.arraycopy(data, wpos, target, ofs, rem);
			ofs += rem;
			len -= rem;
			wpos = (wpos + rem) % data.length;
		}
	}

	/**
	 * @return the number of bytes stored in the buffer.
	 */
	public int available() {
		return wlen;
	}

	/**
	 * @return the remaining space in number of bytes.
	 */
	public int remaining() {
		return data.length - wlen;
	}

	/**
	 * @return the size of the buffer.
	 */
	public int size() {
		return data.length;
	}

	/**
	 * Resets the buffer.
	 * After this call, the buffer is empty and available() == 0.
	 */
	public void reset() {
		rpos = 0;
		wlen = 0;
	}
}
