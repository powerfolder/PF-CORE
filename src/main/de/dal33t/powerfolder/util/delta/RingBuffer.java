package de.dal33t.powerfolder.util.delta;

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
