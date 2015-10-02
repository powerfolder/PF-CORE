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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.RangeProto;

/**
 * This class represents an interval.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public final class Range
  implements Serializable, D2DObject
{
	private static final long serialVersionUID = 100L;
    private long start;
    private long length;


	/**
	 * Creates a range with the given parameters
	 * @param start the beginning of the range
	 * @param length the length of the range
	 * @return a range
	 */
	public static Range getRangeByLength(long start, long length) {
		return new Range(start, length);
	}

	/**
	 * Creates the smallest possible range containing the given numbers.
	 * @param number1 number to be within the range
	 * @param number2 number to be within the range
	 * @return
	 */
	public static Range getRangeByNumbers(long number1, long number2) {
		long s = Math.min(number1, number2);
		return new Range(s, Math.max(number1, number2) - s + 1);
	}

	private Range(long start, long length) {
		if (length < 0) {
			throw new NegativeArraySizeException();
		}
		this.start = start;
		this.length = length;
	}

    /** Range
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    Range(AbstractMessage mesg)
    {
      initFromD2D(mesg);
    }

	/**
	 * @param range
	 * @return true if the given range is contained within this range
	 */
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

	/**
	 * @return the last index contained in this range
	 */
	public long getEnd() {
		return start + length - 1;
	}

	/**
	 * @return the length of this range
	 */
	public long getLength() {
		return length;
	}

	/**
	 * @return the first index contained in this range
	 */
	public long getStart() {
		return start;
	}

	@Override
	public int hashCode() {
		return (int) ((getStart() * 13) & (getEnd() * 137));
	}

	/**
	 * Creates a range which contains only the indices contained in the intersection of this range and the given range.
	 * @param range the range to intersect with
	 * @return the intersected range or null if the ranges don't overlap
	 */
	public Range intersection(Range range) {
		if (!intersects(range)) {
			return null;
		}
		return getRangeByNumbers(Math.max(getStart(), range.getStart()),
				Math.min(getEnd(), range.getEnd()));
	}

	/**
	 * Returns the number of indices which are in this range and the given range.
	 * @param r
	 * @return 0 if the ranges don't overlap, the length of the intersection between them otherwise
	 */
	public long intersectionLength(Range r) {
		if (!intersects(r)) {
			return 0;
		}
		return Math.min(getEnd(), r.getEnd()) - Math.max(getStart(), r.getStart()) + 1;
	}

	/**
	 * @param range the range to intersect test with
	 * @return true if the ranges overlap
	 */
	public boolean intersects(Range range) {
		return getStart() <= range.getEnd() && getEnd() >= range.getStart();
	}

	@Override
	public String toString() {
		return "[" + getStart() + " - " + getEnd() + "]";
	}

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof RangeProto.Range)
        {
          RangeProto.Range proto = (RangeProto.Range)mesg;

          this.start  = proto.getStart();
          this.length = proto.getLength();
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      RangeProto.Range.Builder builder = RangeProto.Range.newBuilder();

      builder.setClassName("Range");
      builder.setStart(this.start);
      builder.setLength(this.length);

      return builder.build();
    }
}
