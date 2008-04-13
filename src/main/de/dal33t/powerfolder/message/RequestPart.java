package de.dal33t.powerfolder.message;

import java.io.IOException;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;

public class RequestPart extends Message {
	private static final long serialVersionUID = 100L;

	private FileInfo file;
	private Range range;
	private double progress;
	
	public RequestPart() {
        // Serialization constructor
	}
	
	public RequestPart(FileInfo file, double progress) {
		this(file, Range.getRangeByLength(0, file.getSize()), progress);
	}

	public RequestPart(FileInfo file, Range range, double progress) {
		super();
		this.file = file;
		this.range = range;
		this.progress = progress;
		validate();
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

    // Overridden due to validation!
    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        validate();
    }

    private void validate() {
        validateFile(file);
        validateRange(range);
        validateProgress(progress);
    }

    private void validateFile(FileInfo file) {
        Reject.ifNull(file, "File is null");
    }

    private void validateRange(Range range) {
        Reject.ifNull(range, "Range is null");
        Reject.ifTrue(range.getStart() < 0 || range.getEnd() > file.getSize(), "Invalid range: " + range);
    }

    private void validateProgress(double progress) {
        Reject.ifTrue(progress < 0 || progress > 1, "Invalid progress: " + progress);
    }
}
