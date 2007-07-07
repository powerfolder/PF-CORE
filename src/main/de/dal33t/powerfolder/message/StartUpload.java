package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Message to indicate that the upload can be started.
 * This message is sent by the uploader.
 * The remote side should send PartRequests or PartinfoRequests.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */ 
public class StartUpload extends Message {
	private static final long serialVersionUID = 100L;
	private FileInfo fileInfo;

	public StartUpload() {
	}

	public StartUpload(FileInfo fInfo) {
		fileInfo = fInfo;
	}
	
	public FileInfo getFile() {
		return fileInfo;
	}
}
