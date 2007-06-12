package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Checksum;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class PartInfoMaker {
	private Checksum chksum;
	private MessageDigest digester;
	private ValueModel processedBytes = new ValueHolder((long) 0);
	
	public PartInfoMaker(Checksum chksumRoller, MessageDigest digester) {
		super();
		if (chksumRoller == null || digester == null) {
			throw new NullPointerException("Parameter is null");
		}
		this.chksum = chksumRoller;
		this.digester = digester;
	}

	/**
	 * Using an Inputstream this method creates PartInfos.
	 * These PartInfos can be used with a PartInfoMatcher to create a list of matches.
	 * Those can finally be used to calculate the difference between to data sets.
	 * @param in the Inputstream to retrieve data from
	 * @param partSize the size of one part (frame)
	 * @return an array of PartInfos
	 * @throws IOException if a read error occured
	 */
	public PartInfo[] createPartInfos(InputStream in, int partSize) throws IOException {
		List<PartInfo> parts = new LinkedList<PartInfo>();
		int idx = 0;
		int n = 0;
		byte[] buf = new byte[4096];
		int read = 0;
		processedBytes.setValue((long) 0);
		while ((read = in.read(buf)) > 0) {
			int ofs = 0;
			processedBytes.setValue((Long) processedBytes.getValue() + read);
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

	/**
	 * Returns the number of bytes processed in createPartInfos.
	 * The value gets updated while the method processes the data. It gets reseted after
	 * each call of createPartInfos.
	 * @return
	 */
	public ValueModel getProcessedBytes() {
		return processedBytes;
	}
}
