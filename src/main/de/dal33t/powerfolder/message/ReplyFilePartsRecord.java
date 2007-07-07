package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Reply to a RequestFilePartsRecord message.
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public class ReplyFilePartsRecord extends Message {
	private static final long serialVersionUID = 100L;

	private FileInfo file;
	private FilePartsRecord record;
	
	public ReplyFilePartsRecord() {
	}

	public ReplyFilePartsRecord(FileInfo file, FilePartsRecord record) {
		super();
		this.file = file;
		this.record = record;
	}

	public FileInfo getFile() {
		return file;
	}

	public FilePartsRecord getRecord() {
		return record;
	}
}
