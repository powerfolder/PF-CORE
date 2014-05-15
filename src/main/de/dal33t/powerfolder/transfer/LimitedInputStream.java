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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * $Id$
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
