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
package de.dal33t.powerfolder.transfer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * $Id$
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
