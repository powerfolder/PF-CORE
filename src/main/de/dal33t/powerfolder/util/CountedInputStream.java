package de.dal33t.powerfolder.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

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
