package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.util.RingBuffer;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public class PartInfoMatcher {
	private static final int BUFFER_SIZE = 16384;
	private RollingChecksum chksum;
	private MessageDigest digester;
	
	/**
	 * "Values" for statistics. Generally those get reseted with each call to matchParts.
	 */
	private ValueModel processedBytes = new ValueHolder((long) 0);
	private ValueModel matchedParts = new ValueHolder((long) 0);
	
	public PartInfoMatcher(RollingChecksum chksum, MessageDigest digester) {
		super();
		this.chksum = chksum;
		this.digester = digester;
	}

	/**
	 * Given input data and an array of PartInfos this method tries to match those.
	 * 
	 * @param in the data
	 * @param pinf the array of PartInfos created by a PartInfoMaker
	 * @return a list of matches, sorted by position in the data
	 * @throws IOException if a read error occured
	 */
	public List<MatchInfo> matchParts(InputStream in, PartInfo[] pinf) throws IOException {
		List<MatchInfo> mi = new LinkedList<MatchInfo>();
		byte[] buf = new byte[BUFFER_SIZE];
		RingBuffer rbuf = new RingBuffer(chksum.getFrameSize());
		Map<Long, List<PartInfo>> chkmap = new HashMap<Long, List<PartInfo>>();
		for (PartInfo info: pinf) {
			List<PartInfo> pList = chkmap.get(info.getChecksum());
			if (pList == null) {
				chkmap.put(info.getChecksum(), pList = new LinkedList<PartInfo>());
			}
			pList.add(info);
		}
		
		matchedParts.setValue((long) 0);
		
		int read = 0;
		long n = 0;
		byte[] dbuf = new byte[chksum.getFrameSize()];
		while ((read = in.read(buf)) > 0) {
			int i = 0;
			while (i < read) {
				int rem = Math.min(rbuf.remaining() - 1, read - i);
				if (rem > 0) {
					chksum.update(buf, i, rem);
					rbuf.write(buf, i, rem);
					i += rem;
				}
				for (; i < read; i++) {
					chksum.update(buf[i]);
					rbuf.write(buf[i]);
					List<PartInfo> mList = chkmap.get(chksum.getValue());
					if (mList != null) {
						// Create digest of current frame
						rbuf.peek(dbuf, 0, chksum.getFrameSize());
						byte[] digest = digester.digest(dbuf);
	                    digester.reset();
						boolean foundMatch = false;
	                    
						for (PartInfo info: mList) {
							if (Arrays.equals(digest, info.getDigest())) {
								mi.add(new MatchInfo(info, n + i - chksum.getFrameSize() + 1));
								matchedParts.setValue((Long) matchedParts.getValue() + 1);
								foundMatch = true;
								break;
							}
						}
						// Speedup: If we found a match, there's no need to check for matching blocks for the next
						// frameSize bytes.
						if (foundMatch) {
							// Clearing the ring buffer will suffice
							rbuf.reset();
							// Break and reload the buffer
							break;
						}
					}
					rbuf.read();
				}
			}
			// n is used to calculate the filler bytes at the end of the file
			n += read;
			processedBytes.setValue(n);
		}
		int rem = (int) (n % chksum.getFrameSize());
		if (rem > 0) {
			rem = chksum.getFrameSize() - rem;
			int av = rbuf.available();
			rbuf.peek(dbuf, 0, av);
			rem -= av;
			digester.update(dbuf, 0, av);
			byte[] digest = digester.digest();
            digester.reset();
			while (rem < chksum.getFrameSize()) {
				chksum.update(0);
				digester.update((byte) 0);
				rem++;
			}

			List<PartInfo> mList = chkmap.get(chksum.getValue());
			
			if (mList != null) {
				for (PartInfo info: mList) {
					if (Arrays.equals(digest, info.getDigest())) {
						mi.add(new MatchInfo(info, n - chksum.getFrameSize()));
						break;
					}
				}
			}
		}
		return mi;
	}

	/**
	 * Returns the number of bytes processed in matchParts.
	 * The value gets updated while the method processes the data. It gets reseted after
	 * each call of matchParts.
	 * @return
	 */
	public ValueModel getProcessedBytes() {
		return processedBytes;
	}
}
