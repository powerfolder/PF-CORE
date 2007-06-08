package de.dal33t.powerfolder.util.delta;


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
	private byte[] ringbuffer;
	private int n;
	private int rpos, wpos;

	/** Adler32 specific */
	private int A = 1, B;
	// Optimizations
	private int steps;
	// Hack for the situation the data approaches ringbuffer-size
	private boolean keepB = true;
	
	public RollingAdler32(int n) {
		ringbuffer = new byte[n];
		this.n = n;
		steps = 8192;
		rpos = wpos = 0;
	}
	
	public void update(int nd) {
		int dist = (wpos - rpos + n) % n + 1;
		int fb = 0;
		nd &= 0xff; // This allows update to be called with bytes directly
		
		if (dist >= n) {
			fb = (ringbuffer[rpos] & 0xff);
			rpos = (rpos + 1) % n;
			if (!keepB) {
				B--;
			} else {
				keepB = false;
			}
		} else {
			keepB = true;
		}
			
		wpos = (wpos + 1) % n;
		ringbuffer[wpos] = (byte) nd;
		
		A = A + nd - fb;
		B = B + A - dist * fb;

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
		ringbuffer = new byte[n];
		steps = MAX_STEPS;
		rpos = wpos = 0;
		keepB = true;
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
