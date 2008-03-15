package de.dal33t.powerfolder.util.delta;

import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Checksum;

import de.dal33t.powerfolder.util.Reject;

/**
 * Creates arrays of PartInfos given the algorithms to use and a data set. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public final class FilePartsRecordBuilder {
	private final Checksum chksum;
	private final MessageDigest partDigester, fileDigester;
    private List<PartInfo> parts = new LinkedList<PartInfo>();
    private long processed;
    private int partSize, partPos;
    
	
    public FilePartsRecordBuilder(Checksum chksumRoller, MessageDigest partDigester, MessageDigest fileDigester,
        int partSize) {
        super();
        Reject.noNullElements(chksumRoller, partDigester, fileDigester);

        this.chksum = chksumRoller;
        this.partDigester = partDigester;
        this.fileDigester = fileDigester;
        this.partSize = partSize;
    }
	
	/**
     * Updates the current record with the given data.
	 * @param data
	 * @param off
	 * @param len
	 */
	public void update(byte[] data, int off, int len) {
	    processed += len;
	    fileDigester.update(data, off, len);
	    
	    while (len > 0) {
	        if (partPos + len >= partSize) {
	            int rem = partSize - partPos;
	            chksum.update(data, off, rem);
	            partDigester.update(data, off, rem);
	            parts.add(new PartInfo(parts.size(), chksum.getValue(), partDigester.digest()));
	            // Only the checksum needs to be reset, since getValue() doesn't do that.
	            chksum.reset();
	            off += rem;
	            len -= rem;
	            partPos = 0;
	        } else {
	            chksum.update(data, off, len);
	            partDigester.update(data, off, len);
	            partPos += len;
	            len = 0;
	        }
	    }
	}
	
	/**
     * Updates the current record with the given data.
	 * @param data
	 */
	public void update(int data) {
	   update(new byte[] { (byte) (data & 0xFF) });
	}
	
	/**
	 * Updates the current record with the given data.
	 * Same as calling update(data, 0, data.length).
	 * @param data the data to update with.
	 */
	public void update(byte[] data) {
	    update(data, 0, data.length);
	}
	
	/**
	 * Performs final operations, such as padding and returns the resulting set.
     * The builder is reset after this call is made.
	 * @return a record containing {@link PartInfo}s and additional information. 
	 */
	public FilePartsRecord getRecord() {
	    try {
	        // Finalize result
	        if (partPos > 0) {
	            for (int i = 0; i < partSize - partPos; i++) {
	                chksum.update(0);
	                partDigester.update((byte) 0);
	            }
	            parts.add(new PartInfo(parts.size(), chksum.getValue(), partDigester.digest()));
	        }
	        return new FilePartsRecord(processed, parts.toArray(new PartInfo[0]), partSize, fileDigester.digest());
	    } finally {
	        reset();
	    }
	}
	
	/**
	 * Resets the builder for further use. 
	 */
	public void reset() {
        processed = 0;
        partPos = 0;
        chksum.reset();
        partDigester.reset();
        parts.clear();
    }
}
