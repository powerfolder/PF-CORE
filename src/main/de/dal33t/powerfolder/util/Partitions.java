package de.dal33t.powerfolder.util;

import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;


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
public class Partitions<T> {
	private Partitions<T> parent, a, b;

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
	 * Inserts the value on the given range.
	 * @param r
	 * @param value
	 */
	public void insert(Range r, T value) {
		if (!r.overlapsRange(range)) {
			return;
		}

		// Don't create children unless necessary
		if (isLeaf() && sameValue(value, content)) {
			return;
		}

		if (isLeaf() && r.containsRange(range)) {
			content = value;
			if (!isRoot()) {
				parent.checkMergeChildren();
			}
			return;
		}

		// If this is a leaf, create children first and then propagate insert (if necessary)
		if (isLeaf()) {
			int m = (range.getMinimumInteger() + range.getMaximumInteger()) / 2;
			a = new Partitions<T>(this, new IntRange(range.getMinimumInteger(), m), content);
			b = new Partitions<T>(this, new IntRange(m + 1, range.getMaximumInteger()), content);
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
		if (!range.overlapsRange(r)) {
			return null;
		}

		if (isLeaf()) {
			if (sameValue(content, val)) {
				return new IntRange(
						Math.max(r.getMinimumInteger(), range.getMinimumInteger()),
						Math.min(r.getMaximumInteger(), range.getMaximumInteger()));
			} else {
				return null;
			}
		}

		Range ra = a.search(r, val);
		Range rb = b.search(r, val);
		if (ra == null)
			return rb;
		if (rb == null)
			return ra;
		if (ra.getMaximumInteger() + 1 == rb.getMinimumInteger()) {
			return new IntRange(ra.getMinimumInteger(), rb.getMaximumInteger());
		}
		return ra;
	}

	private void checkMergeChildren() {
		if (sameValue(a.content, b.content)) {
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
}
