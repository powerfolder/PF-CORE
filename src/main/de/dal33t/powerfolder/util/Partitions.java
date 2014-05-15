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
package de.dal33t.powerfolder.util;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Helper class representing a set of ranges containing values.
 * This class provides the functionality to store and find ranges containing the same data.
 * For example: It can (and does) manage the availability of data stored in a file. Ranges containing data or free ranges can
 * be searched for.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 *
 */
public class Partitions<T> implements Serializable {

    private static final Logger log = Logger.getLogger(Partitions.class.getName());
    private static final long serialVersionUID = 1L;

	private Partitions<T> parent;
    private Partitions<T> a;
    private Partitions<T> b;

	private Range range;
	private T content;

	/**
	 * Creates a new Partition with the given range containing the given value.
	 * @param r
	 * @param base
	 */
	public Partitions(Range r, T base) {
		range = r;
		content = base;
	}

	private Partitions(Partitions<T> p, Range range, T val) {
		parent = p;
		this.range = range;
		content = val;
	}

	private boolean isLeaf() {
		return a == null;
	}

	private boolean isRoot() {
		return parent == null;
	}

	/**
	 * The partitions are stored in a binary tree, this method returns the depth of the tree.
	 * @return
	 */
	public int depth() {
		if (isLeaf()) {
			return 0;
		}
		return Math.max(a.depth(), b.depth()) + 1;
	}

	/**
	 * Returns the range in which partitions can lie.
	 * @return
	 */
	public Range getPartionedRange() {
		return range;
	}

	/**
	 * Inserts the value on the given range.
	 * @param r
	 * @param value
	 */
	public void insert(Range r, T value) {
		if (!r.intersects(range)) {
			return;
		}

		if (r.contains(range)) {
			content = value;
			a = b = null;
			if (!isRoot()) {
				parent.checkMergeChildren();
			}
			return;
		}

		// Don't create children unless necessary
		if (isLeaf() && sameValue(value, content)) {
			return;
		}
		// If this is a leaf, create children first and then propagate insert (if necessary)
		if (isLeaf()) {
			long m = (range.getStart() + range.getEnd()) / 2;
			a = new Partitions<T>(this, Range.getRangeByNumbers(range.getStart(), m), content);
			b = new Partitions<T>(this, Range.getRangeByNumbers(m + 1, range.getEnd()), content);
		}

		a.insert(r, value);
		// If b is null then a and b were succesfully merged
		if (b != null) {
			b.insert(r, value);
		}
	}

	/**
	 * Searches for the given value in the given range r.
	 * It searches for the first range with the given value which intersects with the given range.
	 * The intersection between that range and the given one is returned.
	 * If no qualified range is found, this method returns null.
	 * @param r
	 * @param val
	 * @return
	 */
	public Range search(Range r, T val) {
		if (!range.intersects(r)) {
			return null;
		}

		if (isLeaf()) {
			if (sameValue(content, val)) {
				return Range.getRangeByNumbers(
						Math.max(r.getStart(), range.getStart()),
						Math.min(r.getEnd(), range.getEnd()));
			} else {
				return null;
			}
		}

		Range ra = a.search(r, val);
		Range rb = b.search(r, val);
		if (ra == null) {
            return rb;
        }
		if (rb == null) {
            return ra;
        }
		if (ra.getEnd() + 1 == rb.getStart()) {
			return Range.getRangeByNumbers(ra.getStart(), rb.getEnd());
		}
		return ra;
	}

	private void checkMergeChildren() {
		if (a.isLeaf() && b.isLeaf() && sameValue(a.content, b.content)) {
			content = a.content;
			a = null;
			b = null;
			if (!isRoot()) {
				parent.checkMergeChildren();
			}
		}
	}

	private boolean sameValue(T a, T b) {
		return a == b || (a != null && a.equals(b));
	}

	public void logRanges(Class clazz) {
		if (isLeaf()) {
			log.info(clazz.getName() + ' ' + getPartionedRange() + " with value " + content);
			return;
		}
		a.logRanges(clazz);
		b.logRanges(clazz);
	}

	/**
	 * Counts the number of occurrences of the given value in the given range.
	 * @param r
	 * @param val
	 * @return
	 */
	public long count(Range r, T val) {
		if (isLeaf()) {
			if (!sameValue(content, val)) {
				return 0;
			}
			return range.intersectionLength(r);
		}
		return a.count(r, val) + b.count(r, val);
	}
}
