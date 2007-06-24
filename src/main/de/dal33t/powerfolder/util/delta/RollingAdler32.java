package de.dal33t.powerfolder.util.delta;

import de.dal33t.powerfolder.util.RingBuffer;


/**
 * Adler32 implementation which supports rolling over data.
 * Although there is a java Adler32 implementation (even done in native code), it lacks
 * the ability to "roll" over data making it much slower when continuous data has to be
 * processed.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class RollingAdler32 implements RollingChecksum {
	private final static int MOD_ADLER = 65521;
	private final static int MAX_STEPS = 256;
	private RingBuffer rbuf;
	private int n;

	/** Adler32 specific */
	private int A = 1, B;
	// Optimizations
	private int steps;
	
	public RollingAdler32(int n) {
		rbuf = new RingBuffer(n);
		this.n = n;
		steps = 8192;
	}
	
	public void update(int nd) {
		int fb = 0;
		nd &= 0xff; // This allows update to be called with bytes directly

		if (rbuf.remaining() == 0) {
			fb = rbuf.read();
			/*
			if (!keepB) {
				B--;
			} else {
				keepB = false;
			}
			*/
			B--;
		}

		rbuf.write(nd);
		
		A = A + nd - fb;
		B = B + A - n * fb;

//		System.out.println(A + " " + B + " " + nd + " " + fb);
		
		if (steps-- == 0) {
			steps = MAX_STEPS;
			normalize();
		}
	}

	private void normalize() {
		A = (A % MOD_ADLER + MOD_ADLER) % MOD_ADLER;
		B = (B % MOD_ADLER + MOD_ADLER) % MOD_ADLER;
	}

	public void update(byte[] data) {
		update(data, 0, data.length);
	}
	
	public void reset() {
		A = 1;
		B = 0;
		rbuf.reset();
 		steps = MAX_STEPS;
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
		normalize();
		return ((long) B << 16) | A;
	}
}
