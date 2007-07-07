package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Range;

public class RequestPart extends Message {
	private static final long serialVersionUID = 100L;

	private FileInfo file;
	private Range range;
	
	public RequestPart() {
        // Serialisation constructor
	}
	
	public RequestPart(FileInfo file) {
		this(file, Range.getRangeByLength(0, file.getSize()));
	}

	public RequestPart(FileInfo file, Range range) {
		super();
		this.file = file;
		this.range = range;
	}
	
    public String toString() {
        return "Request to download part of : " + file + ", range " + range; 
    }

	/**
	 * @return the file which has the requested part
	 */
	public FileInfo getFile() {
		return file;
	}

	/**
	 * @return the range of data that is requested
	 */
	public Range getRange() {
		return range;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RequestPart) {
			RequestPart pr = (RequestPart) obj;
			return pr.file.equals(file) && pr.range.equals(range);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return file.hashCode() ^ range.hashCode();
	}
}
