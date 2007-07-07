package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Requests a FilePartsRecord for a given file.
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */ 
public class RequestFilePartsRecord extends Message {
	private static final long serialVersionUID = 100L;

	private FileInfo file;
	
	public RequestFilePartsRecord() {
	}

	public RequestFilePartsRecord(FileInfo file) {
		super();
		this.file = file;
	}

	public FileInfo getFile() {
		return file;
	}
}
