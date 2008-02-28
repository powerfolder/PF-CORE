package de.dal33t.powerfolder.util.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;

/**
 * WORK IN PROGRESS - Don't even try to use it ;)
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class UDTSocket {
	private InputStream in;
	private OutputStream out;
	
	public enum SockOpts {
		UDT_LINGER;
	};

	public UDTSocket() {
	}
	
	public native UDTSocket accept() throws IOException;
	public native void bind(SocketAddress bindPoint) throws IOException;
	public native void close() throws IOException;
	public native void connect(SocketAddress endPoint) throws IOException;
	public native void listen(int backlog) throws IOException;
	
	
	public InputStream getInputStream() throws IOException {
		if (in == null) {
			in = new UDTInputStream();
		}
		return in;
	}
	
	public OutputStream getOutputStream() throws IOException {
		if (out == null) {
			out = new UDTOutputStream();
		}
		return out;
	}
	
	private native int recv(byte[] buffer, int off, int len) throws IOException;
	private native int send(byte[] buffer, int off, int len) throws IOException;
	
	private class UDTInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			byte b[] = new byte[1];
			return recv(b, 0, 1);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return recv(b, off, len);
		}
	}
	
	private class UDTOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			byte buf[] = new byte[] { (byte) b };
			send(buf, 0, 1);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			send(b, off, len);
		}
		
	}
}
