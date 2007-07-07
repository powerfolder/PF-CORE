package de.dal33t.powerfolder.util.delta;

import java.io.Serializable;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;

/**
 * Manages the parts of a file which contain data or not.
 * 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 *
 */
public class FilePartsState implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum PartState {
		NEEDED, // Not available and not yet requested
		PENDING, // Requested for download but not yet received
		AVAILABLE // Available for own upload
	}
	
	private Partitions<PartState> parts;
	
	public FilePartsState(long fileLength) {
		parts = new Partitions<PartState>(Range.getRangeByLength(0, fileLength), PartState.NEEDED);
	}
	
	/**
	 * Returns the first range that lies within "in", which is marked with the given state.
	 * @param in
	 * @param state
	 * @return a Range or null if if no range was found
	 */
	public Range findPart(Range in, PartState state) {
		return parts.search(in, state);
	}
	
	/**
	 * Returns the first range that is marked with the state.
	 * @param state
	 * @return
	 */
	public Range findFirstPart(PartState state) {
		return findPart(parts.getPartionedRange(), state);
	}

	/**
	 * Marks a range of data with a given state
	 * @param range
	 * @param state
	 */
	public void setPartState(Range range, PartState state) {
		parts.insert(range, state);
	}
	
	/**
	 * Resets all pending ranges to needed.
	 */
	public void purgePending() {
		Range r = Range.getRangeByNumbers(0, parts.getPartionedRange().getEnd());
		Range wr;
		while ((wr = findPart(r, PartState.PENDING)) != null) {
			setPartState(wr, PartState.NEEDED);
			r = Range.getRangeByNumbers(wr.getEnd() + 1, r.getEnd());
		}
	}
	
	public long getFileLength() {
		return parts.getPartionedRange().getLength();
	}

	public boolean isCompleted() {
		Range r = parts.search(parts.getPartionedRange(), PartState.AVAILABLE);
		if (r == null) {
			return false;
		}
		return r.equals(parts.getPartionedRange());
	}
	
	public void debugOutput(Logger log) {
		parts.logRanges(log);
	}
}
