package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Checksum;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class PartInfoMaker {
	private Checksum chksum;
	private MessageDigest digester;
	
	public PartInfoMaker(Checksum chksumRoller, MessageDigest digester) {
		super();
		if (chksumRoller == null || digester == null) {
			throw new NullPointerException("Parameter is null");
		}
		this.chksum = chksumRoller;
		this.digester = digester;
	}

	public PartInfo[] createPartInfos(InputStream in, int partSize) throws IOException {
		List<PartInfo> parts = new LinkedList<PartInfo>();
		int idx = 0;
		int n = 0;
		byte[] buf = new byte[4096];
		int read = 0;
		while ((read = in.read(buf)) > 0) {
			int ofs = 0;
			while (read > 0) {
				if (n + read >= partSize) {
					int rem = partSize - n;
					chksum.update(buf, ofs, rem);
					digester.update(buf, ofs, rem);
					parts.add(new PartInfo(idx++, chksum.getValue(), digester.digest()));
					chksum.reset();
					read -= rem;
					ofs += rem;
					n = 0;
				} else {
					chksum.update(buf, ofs, read);
					digester.update(buf, ofs, read);
					n += read;
					read = 0;
				}
			}
		}
		if (n < partSize && n > 0) {
			Arrays.fill(buf, (byte) 0);
			chksum.update(buf, 0, partSize - n);
			digester.update(buf, 0, partSize - n);
			parts.add(new PartInfo(idx++, chksum.getValue(), digester.digest()));
		}
		chksum.reset();
		return parts.toArray(new PartInfo[0]);
	}
}
