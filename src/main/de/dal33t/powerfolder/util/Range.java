package de.dal33t.powerfolder.util;

import java.io.Serializable;

/**
 * This class represents a value range.
 * Although there is a Range class in the apache source tree, but I dislike it.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class Range implements Serializable {
	private static final long serialVersionUID = 100L;
	private long start, length;

	private Range(long start, long length) {
		if (length < 0) {
			throw new NegativeArraySizeException();
		}
		this.start = start;
		this.length = length;
	}

	public static Range getRangeByLength(long start, long length) {
		return new Range(start, length);
	}
	
	public static Range getRangeByNumbers(long number1, long number2) {
		long s = Math.min(number1, number2);
		return new Range(s, Math.max(number1, number2) - s + 1);
	}
	
	public long getLength() {
		return length;
	}

	public long getStart() {
		return start;
	}
	
	public long getEnd() {
		return start + length - 1;
	}
	
	public boolean intersects(Range range) {
		return getStart() <= range.getEnd() && getEnd() >= range.getStart();
	}
	
	public boolean contains(Range range) {
		return getStart() <= range.getStart() && getEnd() >= range.getEnd();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Range) {
			Range r = (Range) obj;
			return getStart() == r.getStart() && getLength() == r.getLength();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) ((getStart() * 13) & (getEnd() * 137));
	}

	@Override
	public String toString() {
		return "[" + getStart() + " - " + getEnd() + "]";
	}
}
