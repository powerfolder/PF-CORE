package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Checksum;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.util.CountedInputStream;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public class FilePartsRecordBuilder {
	private static final int BUFFER_SIZE = 16384;
	private Checksum chksum;
	private MessageDigest partDigester, fileDigester;
	private ValueModel processedBytes = new ValueHolder((long) 0);
	
	public FilePartsRecordBuilder(Checksum chksumRoller, MessageDigest partDigester, MessageDigest fileDigester) {
		super();
		if (chksumRoller == null || partDigester == null || fileDigester == null) {
			throw new NullPointerException("Parameter is null");
		}
		this.chksum = chksumRoller;
		this.partDigester = partDigester;
		this.fileDigester = fileDigester;
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
	public FilePartsRecord buildFilePartsRecord(InputStream input, int partSize) throws IOException {
		List<PartInfo> parts = new LinkedList<PartInfo>();
		int idx = 0;
		int n = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		int read = 0;
		processedBytes.setValue((long) 0);
		
		CountedInputStream in = new CountedInputStream(input);
		
		chksum.reset();
		partDigester.reset();
		fileDigester.reset();
		
		while ((read = in.read(buf)) > 0) {
			int ofs = 0;
			processedBytes.setValue((Long) processedBytes.getValue() + read);
			fileDigester.update(buf, 0, read);
			while (read > 0) {
				if (n + read >= partSize) {
					int rem = partSize - n;
					chksum.update(buf, ofs, rem);
					partDigester.update(buf, ofs, rem);
					parts.add(new PartInfo(idx++, chksum.getValue(), partDigester.digest()));
//                    partDigester.reset();
					chksum.reset();
					read -= rem;
					ofs += rem;
					n = 0;
				} else {
					chksum.update(buf, ofs, read);
					partDigester.update(buf, ofs, read);
					n += read;
					read = 0;
				}
			}
		}
		if (n < partSize && n > 0) {
			for (int i = 0; i < partSize - n; i++) {
				chksum.update(0);
				partDigester.update((byte) 0);
			}
			parts.add(new PartInfo(idx++, chksum.getValue(), partDigester.digest()));
		}
		return new FilePartsRecord((Long) processedBytes.getValue(), parts.toArray(new PartInfo[0]), partSize, fileDigester.digest());
	}

	/**
	 * Returns the number of bytes processed in createPartInfos.
	 * The value gets updated while the method processes the data. It gets reseted after
	 * each call of createPartInfos.
	 * @return
	 */
	public ValueModel getProcessedBytesCount() {
		return processedBytes;
	}
}
