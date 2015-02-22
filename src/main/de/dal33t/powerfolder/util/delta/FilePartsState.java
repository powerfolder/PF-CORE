/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.util.delta;

import java.io.Serializable;

import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;


/**
 * Manages the parts of a file which contain data or not.
 * This class is Thread-safe.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 4280 $
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

	/**
	 * Creates a new instance with the given length.
	 * All elements are set to NEEDED initially.
	 * @param fileLength
	 */
	public FilePartsState(long fileLength) {
		parts = new Partitions<PartState>(Range.getRangeByLength(0, fileLength), PartState.NEEDED);
	}

	/**
	 * Returns the first range that lies within "in", which is marked with the given state.
	 * @param in
	 * @param state
	 * @return a Range or null if if no range was found
	 */
	public synchronized Range findPart(Range in, PartState state) {
		return parts.search(in, state);
	}

	/**
	 * Returns the first range that is marked with the state.
	 * @param state
	 * @return
	 */
	public synchronized Range findFirstPart(PartState state) {
		return findPart(parts.getPartionedRange(), state);
	}

	/**
	 * Marks a range of data with a given state
	 * @param range
	 * @param state
	 */
	public synchronized void setPartState(Range range, PartState state) {
		parts.insert(range, state);
	}

	/**
	 * Counts the number of PartStates in the given range that match the given PartState.
	 * @param r
	 * @param s
	 * @return
	 */
	public synchronized long countPartStates(Range r, PartState s) {
		return parts.count(r, s);
	}

	/**
	 * Resets all pending ranges to needed.
	 */
	public synchronized void purgePending() {
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

	public Range getRange() {
		return parts.getPartionedRange();
	}

	public synchronized boolean isCompleted() {
	    if (parts.getPartionedRange().getLength() == 0) {
	        return true;
	    }
		Range r = parts.search(parts.getPartionedRange(), PartState.AVAILABLE);
		if (r == null) {
			return false;
		}
		return r.equals(parts.getPartionedRange());
	}
}
