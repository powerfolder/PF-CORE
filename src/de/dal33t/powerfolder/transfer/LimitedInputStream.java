package de.dal33t.powerfolder.transfer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * $Id: LimitedInputStream.java,v 1.1 2006/03/05 23:53:35 bytekeeper Exp $
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 */
public class LimitedInputStream extends FilterInputStream implements LimitedStream {
    protected BandwidthLimiter limiter;
    
    public LimitedInputStream(BandwidthLimiter limiter, InputStream arg0) {
        super(arg0);
        this.limiter = limiter;
    }

    @Override
    public int read() throws IOException {
        try {
            limiter.requestBandwidth(1);
        } catch (InterruptedException e) {
            throw new IOException(e.toString());
        }
        return in.read();
    }

    @Override
    public int read(byte[] arg0, int off, int len) throws IOException {
        int allowed;
        try {
            allowed = (int) limiter.requestBandwidth(len);
        } catch (InterruptedException e) {
            throw new IOException(e.toString());
        }
        int amount = in.read(arg0, off, allowed);
        limiter.returnAvailable(allowed - amount);
        return amount;
    }

    public BandwidthLimiter getBandwidthLimiter() {
        return limiter;
    }

    public void setBandwidthLimiter(BandwidthLimiter limiter) {
        this.limiter = limiter;        
    }
}
