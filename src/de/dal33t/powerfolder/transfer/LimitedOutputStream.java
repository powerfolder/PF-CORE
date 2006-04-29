package de.dal33t.powerfolder.transfer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * $Id: LimitedOutputStream.java,v 1.1 2006/03/05 23:53:35 bytekeeper Exp $
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 */
public class LimitedOutputStream extends FilterOutputStream implements LimitedStream {
    protected BandwidthLimiter limiter;
    
    public LimitedOutputStream(BandwidthLimiter limiter, OutputStream arg0) {
        super(arg0);
        this.limiter = limiter;
    }

    @Override
    public void write(byte[] arg0, int offs, int len) throws IOException {
        while (len > 0) {
            long allowed;
            try {
                allowed = limiter.requestBandwidth(len);
            } catch (InterruptedException e) {
                throw new IOException(e.toString());
            }
            out.write(arg0, offs, (int) allowed);
            offs += allowed;
            len -= allowed;
        }
    }

    public void write(byte[] arg0, int offs, int len, boolean unlimited) throws IOException {
        while (len > 0) {
            long allowed = len;
            if (!unlimited) {
                try {
                    allowed = limiter.requestBandwidth(len);
                } catch (InterruptedException e) {
                    throw new IOException(e.toString());
                }
            }
            out.write(arg0, offs, (int) allowed);
            offs += allowed;
            len -= allowed;
        }
    }
    
    
    @Override
    public void write(int arg0) throws IOException {
        try {
            limiter.requestBandwidth(1);
        } catch (InterruptedException e) {
            throw new IOException(e.toString());
        }
        out.write(arg0);
    }
    
    public BandwidthLimiter getBandwidthLimiter() {
        return limiter;
    }

    public void setBandwidthLimiter(BandwidthLimiter limiter) {
        this.limiter = limiter;        
    }
}
