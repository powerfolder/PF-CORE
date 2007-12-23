package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Range;

public class RequestPart extends Message {
	private static final long serialVersionUID = 100L;

	private FileInfo file;
	private Range range;
	private double progress;
	
	public RequestPart() {
        // Serialization constructor
	}
	
	public RequestPart(FileInfo file, double remaining) {
		this(file, Range.getRangeByLength(0, file.getSize()), remaining);
	}

	public RequestPart(FileInfo file, Range range, double remaining) {
		super();
		this.file = file;
		this.range = range;
		progress = remaining;
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

	/**
	 * The progress is a guess of the transfer progress.
	 * The downloader sets this value so the uploader can show a progress to the user.
	 * The actual progress is implementation dependent and is therefore given as a double value in the range [0,1]
	 * @return the progress 
	 */
	public double getProgress() {
		return progress;
	}
}
