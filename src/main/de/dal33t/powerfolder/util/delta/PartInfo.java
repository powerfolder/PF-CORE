package de.dal33t.powerfolder.util.delta;

/**
 * Info for a frame of bytes.
 * A partinfo contains only enough information to check for matches and reconstruct
 * the location in a file.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class PartInfo {
	private long index;
	private long checksum;
	private byte[] digest;
	public long getChecksum() {
		return checksum;
	}
	public byte[] getDigest() {
		return digest;
	}
	public long getIndex() {
		return index;
	}
	public PartInfo(long index, long checksum, byte[] digest) {
		super();
		this.index = index;
		this.checksum = checksum;
		this.digest = digest;
	}
}
