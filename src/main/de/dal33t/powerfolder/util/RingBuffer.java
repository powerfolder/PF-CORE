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
public class RingBuffer {
	private byte[] data;
	private int rpos, wlen;
	
	public RingBuffer(int size) {
		data = new byte[size];
	}
	
	public void write(int b) {
		if (wlen >= data.length) {
			throw new BufferOverflowException();
		}
		data[(rpos + wlen) % data.length] = (byte) b;
		wlen++;
	}
	
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
	
	public void write(byte[] b) {
		write(b, 0, b.length);
	}
	
	public int read() {
		if (wlen <= 0) {
			throw new BufferUnderflowException();
		}
		int val = data[rpos] & 0xff;
		rpos = (rpos + 1) % data.length;
		wlen--;
		return val;
	}
	
	public int peek() {
		if (wlen <= 0) {
			throw new BufferUnderflowException();
		}
		return data[rpos] & 0xff;
	}
	
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
	
	public void peek(byte[] target, int ofs, int len) {
		if (len > wlen) {
			throw new BufferUnderflowException();
		}
		int wpos = (rpos + wlen) % data.length;
		while (len > 0) {
			int rem = Math.min(data.length - wpos, len);
			System.arraycopy(data, wpos, target, ofs, rem);
			ofs += rem;
			len -= rem;
			wpos = (wpos + rem) % data.length;
		}
	}
	
	public int available() {
		return wlen;
	}
	
	public int remaining() {
		return data.length - wlen;
	}
	
	public int size() {
		return data.length;
	}
	
	public void reset() {
		rpos = 0;
		wlen = 0;
	}
}
