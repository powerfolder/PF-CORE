package de.dal33t.powerfolder.util.delta;

import java.util.List;

import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;

import de.dal33t.powerfolder.util.Partitions;

/**
 * Manages the parts of a file which contain data or not.
 * 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 *
 */
public class FilePartsManager {
	private enum RangeState {
		NEEDED,
		AVAILABLE
	}
	
	private Partitions<RangeState> parts;
	
	public FilePartsManager(long fileLength) {
		parts = new Partitions<RangeState>(new IntRange(0, fileLength - 1), RangeState.NEEDED);
	}
	
	public FilePartsManager(long fileLength, List<MatchInfo> matches, long matchLen) {
		this(fileLength);
		for (MatchInfo mi: matches) {
			parts.insert(new IntRange(mi.getMatchedPosition(), mi.getMatchedPosition() + matchLen - 1), RangeState.AVAILABLE);
		}
	}
	
	/**
	 * Returns the first range that lies within "in", which is still required.
	 * @param in
	 * @return
	 */
	public Range findRequiredPart(Range in) {
		return parts.search(in, RangeState.NEEDED);
	}

	/**
	 * Marks a range of data as available or required.
	 * @param range
	 * @param b
	 */
	public void setAvailable(IntRange range, boolean b) {
		parts.insert(range, b ? RangeState.AVAILABLE : RangeState.NEEDED);
	}
	
}
