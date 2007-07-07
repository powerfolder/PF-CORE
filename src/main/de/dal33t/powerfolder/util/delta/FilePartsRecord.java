package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.Serializable;

/**
 * Holds the info of one set of PartInfos. 
 *  
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class FilePartsRecord implements Serializable {
	private static final long serialVersionUID = 1L;

	private PartInfo[] infos;
	private int partLength;
	private long fileLength;
	private byte[] fileDigest;
	
	public FilePartsRecord(long fileLength, PartInfo[] infos, int partSize, byte[] fileDigest) throws IOException {
		partLength = partSize;
		this.infos = infos;
		this.fileDigest = fileDigest;
		this.fileLength = fileLength;
	}

	public PartInfo[] getInfos() {
		return infos;
	}

	public int getPartLength() {
		return partLength;
	}

	public byte[] getFileDigest() {
		return fileDigest;
	}

	public long getFileLength() {
		return fileLength;
	}
}
