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
package de.dal33t.powerfolder.util.delta;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.RingBuffer;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 4280 $
 */
public class PartInfoMatcher extends FilterInputStream {
	private static final int BUFFER_SIZE = 16384;
	private final RollingChecksum chksum;
	private final MessageDigest digester;
    private final RingBuffer rbuf;
    private final byte[] buf = new byte[BUFFER_SIZE];
    private final Map<Long, List<PartInfo>> partCache =
        new HashMap<Long, List<PartInfo>>();
    private final byte[] dbuf;

    private long pos;

	public PartInfoMatcher(InputStream in, RollingChecksum chksum, MessageDigest digester, PartInfo[] partInfos) {
		super(in);
		Reject.noNullElements(chksum, digester, partInfos);
		this.chksum = chksum;
		this.digester = digester;

        rbuf = new RingBuffer(chksum.getFrameSize());
        dbuf = new byte[chksum.getFrameSize()];
        for (PartInfo info: partInfos) {
            List<PartInfo> pList = partCache.get(info.getChecksum());
            if (pList == null) {
                partCache.put(info.getChecksum(), pList = new LinkedList<PartInfo>());
            }
            pList.add(info);
        }
	}

	public MatchInfo nextMatch() throws IOException, InterruptedException {
	    // Step 1: Fill buffer for matching
	    int rem = rbuf.remaining();

	    while (rem > 0) {
	        int amount = Math.min(rem, BUFFER_SIZE);
	        int read = read(buf, 0, amount);
	        if (read == -1) {
	            break;
	        }
	        pos += read;
	        rem -= read;
	        chksum.update(buf, 0, read);
	        rbuf.write(buf, 0, read);
	    }
	    // Step 2: If the buffer is full, try to find a match or EOF
	    while (rbuf.remaining() == 0) {
	        if (Thread.interrupted()) {
	            throw new InterruptedException();
	        }
	        List<PartInfo> lookup = partCache.get(chksum.getValue());
	        if (lookup != null) {
                rbuf.peek(dbuf, 0, chksum.getFrameSize());
                byte[] digest = digester.digest(dbuf);

                for (PartInfo info: lookup) {
                    if (Arrays.equals(digest, info.getDigest())) {
                        MatchInfo retval = new MatchInfo(info, pos - chksum.getFrameSize());
                        rbuf.reset();
                        return retval;
                    }
                }
	        }
	        rbuf.skip(1);
            int data = read();
            if (data == -1) {
                break;
            }
	        pos++;
	        rbuf.write(data);
	        chksum.update(data);
	    }
	    // Step 3: If we got on EOF before try to finalize the result or return null if all is done
        rem = (int) (pos % chksum.getFrameSize());
        if (rem > 0) {
            pos -= rem;

            int av = rbuf.available();
            rbuf.peek(dbuf, 0, av);
            digester.update(dbuf, 0, av);


            rem = chksum.getFrameSize() - rem;

            for (int i = 0; i < rem; i++) {
                chksum.update(0);
                digester.update((byte) 0);
            }

            byte[] digest = digester.digest();
            List<PartInfo> mList = partCache.get(chksum.getValue());

            if (mList != null) {
                for (PartInfo info: mList) {
                    if (Arrays.equals(digest, info.getDigest())) {
                        return new MatchInfo(info, pos);
                    }
                }
            }
        }
        return null;
	}
}
