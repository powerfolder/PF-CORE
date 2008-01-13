package de.dal33t.powerfolder.util.delta;

import de.dal33t.powerfolder.util.RingBuffer;


/**
 * Adler32 implementation which supports rolling over data.
 * Although there is a java Adler32 implementation (even done in native code), it lacks
 * the ability to "roll" over data making it much slower when continuous data has to be
 * processed.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public final class RollingAdler32 implements RollingChecksum {
	private final static int MOD_ADLER = 65521;
	private RingBuffer rbuf;
	private int n;

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
