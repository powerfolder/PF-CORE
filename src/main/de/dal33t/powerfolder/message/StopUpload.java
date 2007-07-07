package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Tells the uploader to stop uploading.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$ 
 */
public class StopUpload extends Message {
	private static final long serialVersionUID = 100L;
	private FileInfo fileInfo;

	public StopUpload() {
	}

	public StopUpload(FileInfo fInfo) {
		fileInfo = fInfo;
	}
	
	public FileInfo getFile() {
		return fileInfo;
	}
}
