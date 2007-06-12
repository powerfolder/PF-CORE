package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class PartInfoMatcher {
	private RollingChecksum chksum;
	private MessageDigest digester;
	private ValueModel processedBytes = new ValueHolder((long) 0);
	
	
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
		byte[] buf = new byte[4096];
		RingBuffer rbuf = new RingBuffer(chksum.getFrameSize());
		Map<Long, List<PartInfo>> chkmap = new TreeMap<Long, List<PartInfo>>();
		for (PartInfo info: pinf) {
			List<PartInfo> pList = chkmap.get(info.getChecksum());
			if (pList == null) {
				chkmap.put(info.getChecksum(), pList = new LinkedList<PartInfo>());
			}
			pList.add(info);
		}
		int read = 0;
		long n = 0;
		byte[] dbuf = new byte[chksum.getFrameSize()];
		while ((read = in.read(buf)) > 0) {
			for (int i = 0; i < read; i++) {
				chksum.update(buf[i]);
				if (rbuf.remaining() == 0) {
					rbuf.read();
				}
				rbuf.write(buf[i]);
				// Only try to match checksums if we read at least one frame of bytes
				if (rbuf.remaining() == 0) {
					List<PartInfo> mList = chkmap.get(chksum.getValue());
					if (mList != null) {
						// Create digest of current frame
						rbuf.peek(dbuf, 0, chksum.getFrameSize());
						byte[] digest = digester.digest(dbuf);
						
						for (PartInfo info: mList) {
							if (Arrays.equals(digest, info.getDigest())) {
								mi.add(new MatchInfo(info, n + i - chksum.getFrameSize() + 1));
								break;
							}
						}
					}
				}
			}
			// n is used to calculate the filler bytes at the end of the file
			n += read;
			processedBytes.setValue(n);
		}
		int rem = (int) (n % chksum.getFrameSize());
		if (rem > 0) {
			rbuf.peek(dbuf, 0, chksum.getFrameSize());
			byte[] digest = digester.digest(dbuf);
			while (rem < chksum.getFrameSize()) {
				chksum.update(0);
				digester.update((byte) 0);
				rem++;
			}

			List<PartInfo> mList = chkmap.get(chksum.getValue());
			
			for (PartInfo info: mList) {
				if (Arrays.equals(digest, info.getDigest())) {
					mi.add(new MatchInfo(info, n - chksum.getFrameSize()));
					break;
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
